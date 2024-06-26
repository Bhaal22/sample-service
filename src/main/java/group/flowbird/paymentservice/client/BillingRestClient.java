package group.flowbird.paymentservice.client;

import group.flowbird.paymentservice.client.ClientDTO.InvoiceDTO;
import group.flowbird.paymentservice.configuration.BillingConfiguration;
import nl.yellowbrick.buckarooclient.utils.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.NoSuchElementException;

@Component
public class BillingRestClient {

    public static final Logger logger = LoggerFactory.getLogger(BillingRestClient.class);

    @Autowired
    RestClient restClient;

    @Autowired
    BillingConfiguration billingConfiguration;

    private InvoiceDTO invoiceDTO;

    public boolean hasRemoteServerErrorOccurred(){
        return restClient.remoteServerErrorOccurred;
    }

    public Long getCustomerId(Long invoiceId) throws NoSuchElementException {
        
        if(null != invoiceDTO && invoiceId.equals(invoiceDTO.getId())) {
            return invoiceDTO.getCustomerId();
        }
        
        invoiceDTO = getInvoice(invoiceId);
        if(null != invoiceDTO) {
            return invoiceDTO.getCustomerId();
        }
        throw new NoSuchElementException("Could not find any invoice for invoiceId: " + invoiceId);
    }
    
    public InvoiceDTO getInvoice(Long invoiceId){
        String url = billingConfiguration.getGetInvoice() + "/" + invoiceId;
        invoiceDTO = restClient.performRequest("", HttpMethod.GET, url, InvoiceDTO.class);
        return invoiceDTO;
    }

    public boolean hasOutStandingInvoice(Long customerId) {
        String url = billingConfiguration.getHasOutstandingInvoice() + "/" + customerId;
        return restClient.performRequest("", HttpMethod.GET, url, Boolean.class);
    }

    public Boolean updateInvoiceToPaid(Long invoiceId){

        String url = billingConfiguration.getSendInvoicePaymentInfo();
        invoiceDTO = getInvoice(invoiceId);
        if (null == invoiceDTO){
            logger.info(String.format("Couldn't find any invoice with id : ", invoiceId));
            /*
             * There has been a problem, daaamnit...
             */
            return false;
        }
        invoiceDTO.setPaidOn(Calendar.getInstance().getTime());
        invoiceDTO.setPaid(true);

        restClient.performRequest(RestUtils.mapStringFromObject(invoiceDTO), HttpMethod.POST, url, HttpStatus.class);
        return null != restClient.getResponseEntity() &&
               restClient.getResponseEntity().getStatusCode().equals(HttpStatus.OK);
    }
}
