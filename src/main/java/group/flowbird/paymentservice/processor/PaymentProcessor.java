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
import nl.yellowbrick.buckarooclient.model.IbanValidationRequest;
import nl.yellowbrick.buckarooclient.payment.BuckarooInvoicePaymentTransaction;
import nl.yellowbrick.buckarooclient.service.IbanValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

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


    public String processPaymentTransaction(String transactionId, BuckarooDTO.Status status, String paymentKey){

        if(null == status || ! status.getCode().getCode().equals(BuckarooDTO.TRANSACTION_STATUS_SUCCESS)){
            return paymentReminderConfiguration.getPaymentUnsuccessful();
        }

        /*
         * Update IbanValidationRequest
         */
        IbanValidationRequest ibanValidationRequest = ibanValidationService.getIbanValidationRequest(transactionId);
        /*
         * First check if the Transaction is already processed by callback or push
         */
        if(BuckarooDTO.isSuccessfulTransaction(ibanValidationRequest.getResponseCode()) && null != ibanValidationRequest.getPaymentKey()){
            return paymentReminderConfiguration.getPaymentSuccessful();
        }

        ibanValidationRequest.setResponseCode(status.getCode().getCode());
        ibanValidationRequest.setResponseDescription(status.getCode().getDescription());
        ibanValidationRequest.setPaymentKey(paymentKey);
        ibanValidationService.updateIbanValidationRequest(ibanValidationRequest);


        /*
         * Update Invoice status
         */
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setPaidOn(Calendar.getInstance().getTime());
        invoiceDTO.setId(Long.valueOf(ibanValidationRequest.getInvoiceId()));
        billingRestClient.updateInvoice(invoiceDTO);
        /*
         * Check if there is still outstanding payment
         */
        if(billingRestClient.hasOutStandingInvoice(invoiceDTO.getId())){
            return paymentReminderConfiguration.getPaymentSuccessfulStillOutstanding();
        }

        /*
         * activate the customer
         */
        try {
            yellowSoapRestClient.activateCustomer(invoiceDTO.getCustomerId());
        }catch (ResourceAccessException ex){
            logger.error("YellowSoap Server is down, please check");
            ex.printStackTrace();
            return paymentReminderConfiguration.getServerError();
        }catch (Exception ex){
            ex.printStackTrace();
            logger.error("Some unknown exception occurred, couldn't communicate with yellow  soap");
            return paymentReminderConfiguration.getServerError();
        }

        return paymentReminderConfiguration.getPaymentSuccessful();
    }

    public String processCallbackByCallbackKey(String callbackKey){

        /*
         * Please accept the ambiguity that callback key is actually stored as callback URL for faster query, definitely not the ideal way
         */
        String transactionId = ibanValidationService.getTransactionIdFromCallbackURL(callbackKey);
        BuckarooTransactionResponse response = getTransactionResponse(transactionId);
        //return processPaymentTransaction(response);
        return processPaymentTransaction(response.getKey(), response.getStatus(), response.getPaymentKey());
    }

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
        InvoiceDTO invoiceDTO;
        try{
            invoiceDTO = billingRestClient.getInvoice(invoiceId);
        }catch (ResourceAccessException ex){
            ex.printStackTrace();
            return paymentReminderConfiguration.getServerError();
        }catch (Exception ex){
            ex.printStackTrace();
            return paymentReminderConfiguration.getServerError();
        }

        if(null == invoiceDTO){
            return paymentReminderConfiguration.getInvalidInvoiceId();
        }

        if(false == billingRestClient.hasOutStandingInvoice(invoiceDTO.getCustomerId())){
            return paymentReminderConfiguration.getNoOutstandingInvoice();
        }

        // temporary call
        if(null != invoiceDTO.getPaidOn()){
            return paymentReminderConfiguration.getCurrentInvoicePaidStillOutstanding();
        }
        /*
         * Initiate Payment
         */
        return initiateBuckarooPaymentRedirect(invoiceDTO);
    }

    private BuckarooTransactionResponse getTransactionResponse(String transactionId){
        BuckarooTransactionClient<BuckarooInvoicePaymentTransaction> buckarooClient =
                ibanValidationService.createBuckarooClientForPayment(applicationConfiguration.getName(), null, null, defaultLocale); //we dont need all params
        return (BuckarooTransactionResponse) buckarooClient.getPaymentTransactionStatus(transactionId);
    }

    private String initiateBuckarooPaymentRedirect(InvoiceDTO invoiceDTO){
        String invoiceIdString = String.valueOf(invoiceDTO.getId());
        IbanValidationRequest ibanValidationRequest = ibanValidationService.redirectToIbanValidationThirdParty(
                invoiceDTO.getCustomerId(),
                applicationConfiguration.getName(),
                String.valueOf(invoiceDTO.getId()),
                Double.valueOf(invoiceDTO.getAmount()),
                defaultLocale
        );
        return ibanValidationRequest.getRedirectUrl();
    }

}
