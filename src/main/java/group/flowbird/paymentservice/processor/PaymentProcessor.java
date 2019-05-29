package group.flowbird.paymentservice.processor;


import group.flowbird.paymentservice.client.BillingRestClient;
import group.flowbird.paymentservice.client.ClientDTO.InvoiceDTO;
import group.flowbird.paymentservice.client.YellowSoapRestClient;
import group.flowbird.paymentservice.configuration.ApplicationConfiguration;
import group.flowbird.paymentservice.configuration.PaymentReminderConfiguration;
import nl.yellowbrick.buckarooclient.client.BuckarooTransactionClient;
import nl.yellowbrick.buckarooclient.dto.BuckarooDTO;
import nl.yellowbrick.buckarooclient.dto.BuckarooPushRequest;
import nl.yellowbrick.buckarooclient.dto.BuckarooTransactionResponse;
import nl.yellowbrick.buckarooclient.model.IbanValidation;
import nl.yellowbrick.buckarooclient.model.IbanValidationRequest;
import nl.yellowbrick.buckarooclient.payment.BuckarooInvoicePaymentTransaction;
import nl.yellowbrick.buckarooclient.service.IbanValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Locale;

@Service
public class PaymentProcessor {
    public static final Locale defaultLocale = new Locale("nl", "NL");
    public static final Logger logger = LoggerFactory.getLogger(PaymentProcessor.class);

    @Autowired
    BillingRestClient billingRestClient;

    @Autowired
    IbanValidationService ibanValidationService;

    @Autowired
    PaymentReminderConfiguration paymentReminderConfiguration;

    @Autowired
    ApplicationConfiguration applicationConfiguration;

    @Autowired
    YellowSoapRestClient yellowSoapRestClient;

    Locale locale;

    /**
     * Post Payment Transaction processor, used by both push success, fail and callback.
     * We could use TransactionResponse but apparently we use TransactionResponse and BuckarooTransactionRequest for callback and push
     * @param transactionId
     * @param status Buckaroo Status object, same structure everywhere
     * @param paymentKey : payment key is the proof that the user paid, if we already have a payment key for the request, means
     *        we already processed this transaction.
     * @return : returns the action URL (redirection in this case), Push source doesn't care about this, but callback does.
     */
    public String processPaymentTransaction(String transactionId, BuckarooDTO.Status status, String paymentKey){

        /*
         * Step 1 : check if the payment is successful or not
         */
        if(paymentIsUnsuccessful(status)){
            return paymentReminderConfiguration.getPaymentUnsuccessful() + getLocale();
        }

        /*
         * Step 2 : check if the Transaction is already processed by callback or push
         */

        IbanValidationRequest ibanValidationRequest = ibanValidationService.getIbanValidationRequest(transactionId);
        if(null != ibanValidationRequest.getPaymentKey()){
            logger.info("This transaction is already processed and successful");
            return paymentReminderConfiguration.getPaymentSuccessful();
        }

        /*
         * Step 3 : Update the ibanValidation related data to track the third party communication and response
         */
        updateIbanValidationData(ibanValidationRequest, status, paymentKey);


        /*
         * Step 4 : Let the billing know that invoice is paid
         */
        Long invoiceId = Long.valueOf(ibanValidationRequest.getInvoiceId());
        if(!billingRestClient.updateInvoiceToPaid(invoiceId)){
            logger.info("Couldn't update Invoice status to Paid, probable cause is : Billing Server is down");
            return paymentReminderConfiguration.getServerError() + getLocale();
        }

        /*
         * Step 5 : Check whether we can proceed or not, we can't activate customer if there is outstanding invoice.
         */
        if(billingRestClient.hasOutStandingInvoiceByInvoiceId(invoiceId)){
            return paymentReminderConfiguration.getPaymentSuccessfulStillOutstanding() + getLocale();
        }

        /*
         * Step 6 : Now we can activate the customer
         */
        Long customerId = billingRestClient.getCustomerId(invoiceId);
        logger.info(String.format("Activating customer, customer id = %d", customerId));
        if(!yellowSoapRestClient.activateCustomer(customerId)){
            return paymentReminderConfiguration.getServerError();
        }
        logger.info(String.format("Successfully activated customer , customer id = %d", customerId));

        /*
         * Step 6 : Final Step : Everything went perfect. Lets convey the message that payment is successful, congrats ;)
         */
        return paymentReminderConfiguration.getPaymentSuccessful() + getLocale();
    }

    /**
     * The user is redirected after Payment, this method finds out the proper response we should provide the user
     * And redirect to MyYellowbrick to process the response template.
     * @param callbackKey : provided by third party , uniquely identifiable third party request.
     * @return
     */
    public String processCallbackByCallbackKey(String callbackKey){

        /*
         * Please accept the ambiguity that callback key is actually stored as callback URL for faster query, definitely not the ideal way
         */
        if(null == callbackKey){
            logger.error("We can't allow you to fool us, please try with a callback Key");
            return paymentReminderConfiguration.getServerError();
        }

        String transactionId = ibanValidationService.getTransactionIdFromCallbackURL(callbackKey);
        if(null == transactionId){
            logger.error("Something went wrong horribly, We couldn't find any transaction id associated with the provided callback key");
            return paymentReminderConfiguration.getServerError();
        }

        BuckarooTransactionResponse response = getTransactionResponse(transactionId);
        String invoiceId = response.getInvoice();
        updateLocaleIfYouCan(invoiceId);
        logger.info(String.format("Processing Buckaroo Transaction with callback Key = %s", callbackKey));
        String callbackResponseURL = processPaymentTransaction(response.getKey(), response.getStatus(), response.getPaymentKey());
        logger.info(String.format("Received callback String = %s after processing", callbackResponseURL));
        return callbackResponseURL;
    }

    /**
     * Thin layer between third party and our internal post processing transaction,
     * However, the same post processing can be done by callback URL
     * We accept any of those trigger and respect that.
     * Only difference is push doesn't redirect the user.
     * @param request
     */
    public void processPushURL(BuckarooPushRequest request){
        processPaymentTransaction(request.getTransactionId(), request.getTransaction().getStatus(), request.getTransaction().getPaymentKey());
    }


    /**
     *
     * @param invoiceId
     * @return Redirected URL
     * Case 1 => There is no Outstanding Payment                        : redirect to MyYellowbrick corresponding page
     * Case 2 => There is Outstanding Payment but given invoice is Paid : redirect to MyYellowbrick corresponding page.
     * Case 2 => There is outstanding payment                           : initiate Buckaroo Transaction
     */
    public String processRedirectURL(Long invoiceId){
        InvoiceDTO invoiceDTO = billingRestClient.getInvoice(invoiceId);
        updateLocaleIfYouCan(invoiceDTO);

        if(billingRestClient.hasRemoteServerErrorOccurred()){
            return paymentReminderConfiguration.getServerError() + getLocale();
        }

        if(null == invoiceDTO){
            return paymentReminderConfiguration.getInvalidInvoiceId() + getLocale();
        }

        boolean hasoutStandingInvoice = true;
        hasoutStandingInvoice = billingRestClient.hasOutStandingInvoice(invoiceDTO.getCustomerId());

        if(billingRestClient.hasRemoteServerErrorOccurred()){
            return paymentReminderConfiguration.getServerError() + getLocale();
        }

        if(false == hasoutStandingInvoice){
            return paymentReminderConfiguration.getNoOutstandingInvoice() + getLocale();
        }

        if(invoiceDTO.isPaid()){
            return paymentReminderConfiguration.getCurrentInvoicePaidStillOutstanding() + getLocale();
        }

        if(!invoiceDTO.isInPaymentReminder()){
            /*
             * Payment Reminder link is expired
             */
            return paymentReminderConfiguration.getPaymentReminderLinkExpired() + getLocale();
        }
        /*
         * Everything is good, now we can initiate Payment
         */
        logger.info("Initiating Buckaroo Payment Transaction, Be paitient, we will soon have your redirection URL");
        return initiateBuckarooPaymentRedirect(invoiceDTO);
    }


    /*
     * Start of the private helper method section
     */

    /*
     * We only consider successfully paid transactions, unsuccessful here means its not paid, not the actual payment status.
     * We don't need intermediate statuses like , pending_input, etc.
     */
    private boolean paymentIsUnsuccessful(BuckarooDTO.Status status){
        if(null == status || ! BuckarooDTO.isSuccessfulTransaction(status.getCode().getCode())){
            if(null == status){
                logger.error("status object is null");
            }
            else logger.error(String.format("Status code = %d", status.getCode().getCode()));
            return true;
        }
        return false;
    }

    /*
     * We rely on the previously created tables called IbanValidation and IbanValidationRequest.
     * Though in this case they are not related to IbanValidation anymore, its merely storage for Buckaroo Payment gateway
     */
    private void updateIbanValidationData(IbanValidationRequest ibanValidationRequest, BuckarooDTO.Status status, String paymentKey){

        int statusCode = status.getCode().getCode();
        ibanValidationRequest.setResponseCode(statusCode);
        ibanValidationRequest.setResponseDescription(status.getCode().getDescription());
        ibanValidationRequest.setPaymentKey(paymentKey);
        ibanValidationService.updateIbanValidationRequest(ibanValidationRequest);

        /*
         * We also need to update IbanValidation
         */
        IbanValidation ibanValidation = ibanValidationService.getIbanValidation(ibanValidationRequest.getTransactionId());
        if(null == ibanValidation){
            logger.info("We dont have a valida IbanValidationObject, ignoring saving part.....");
            return;
        }

        ibanValidation.setValidationStatus(BuckarooDTO.getStatus(statusCode));
        ibanValidation.setMutation_date(Calendar.getInstance().getTime());

        ibanValidationService.updateIbanValidation(ibanValidation);
    }

    private BuckarooTransactionResponse getTransactionResponse(String transactionId){
        BuckarooTransactionClient<BuckarooInvoicePaymentTransaction> buckarooClient =
                ibanValidationService.createBuckarooClientForPayment(applicationConfiguration.getName(), null, null, defaultLocale); //we dont need all params
        return (BuckarooTransactionResponse) buckarooClient.getPaymentTransactionStatus(transactionId);
    }

    private String getLocale(){
        if(null == locale)return "";
        return "?locale=" + locale.getLanguage() + "_" + locale.getCountry();
    }


    private void updateLocaleIfYouCan(InvoiceDTO invoiceDTO){
        if(null != invoiceDTO && null != invoiceDTO.getLocale() && !invoiceDTO.getLocale().isEmpty()){
            String localeString = invoiceDTO.getLocale();
            String localeStrings[] = localeString.split("_");
            String newLocaleString = localeStrings[0] + "-" + localeStrings[1];
            this.locale = new Locale.Builder().setLanguageTag(newLocaleString).build();
        }
    }

    private void updateLocaleIfYouCan(String invoiceId){
        InvoiceDTO invoiceDTO = billingRestClient.getInvoice(Long.valueOf(invoiceId));
        updateLocaleIfYouCan(invoiceDTO);
    }

    private String initiateBuckarooPaymentRedirect(InvoiceDTO invoiceDTO){
        IbanValidationRequest ibanValidationRequest = ibanValidationService.redirectToIbanValidationThirdParty(
                invoiceDTO.getCustomerId(),
                applicationConfiguration.getName(),
                String.valueOf(invoiceDTO.getId()),
                //invoiceDTO.getAmount(), /* Buckaroo Test doesn't work, Before deployment, we must uncomment this  and remove the next line */
                .01,
                defaultLocale
        );

        if(null == ibanValidationRequest){
            return paymentReminderConfiguration.getServerError() + getLocale();
        }
        return ibanValidationRequest.getRedirectUrl();
    }
}
