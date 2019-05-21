package group.flowbird.paymentservice.client;

import group.flowbird.paymentservice.client.ClientDTO.InvoiceDTO;
import nl.yellowbrick.buckarooclient.utils.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BillingRestClient {
    public static final String BASE_URL                     = "http://localhost:5080/paymentReminder";
    public static final String GET_INVOICE_ENDPOINT         = "getInvoice?invoiceId=";
    public static final String HAS_OUTSTANDING_INVOICE      = "hasOutstandingInvoice?customerId=";
    public static final String SEND_INVOICE_PAYMENT_INFO    = "sendInvoicePaymentInfo";

    @Autowired
    RestClient restClient;

    public InvoiceDTO getInvoice(Long invoiceId){
        String url = BASE_URL + "/" + GET_INVOICE_ENDPOINT + invoiceId;
        InvoiceDTO invoiceDTO = restClient.performRequest("", HttpMethod.GET, url, InvoiceDTO.class);
        return invoiceDTO;
    }
    public boolean hasOutStandingInvoice(Long customerId){
        String url = BASE_URL + "/" + HAS_OUTSTANDING_INVOICE + customerId;
        Boolean result = restClient.performRequest("", HttpMethod.GET, url, Boolean.class);
        return result;
    }

    public Boolean updateInvoice(InvoiceDTO invoiceDTO){
        String url = BASE_URL + "/" + SEND_INVOICE_PAYMENT_INFO;
        restClient.performRequest(RestUtils.mapStringFromObject(invoiceDTO), HttpMethod.POST, url, HttpStatus.class);
        return restClient.getResponseEntity().getStatusCode() == HttpStatus.OK;
    }
}
