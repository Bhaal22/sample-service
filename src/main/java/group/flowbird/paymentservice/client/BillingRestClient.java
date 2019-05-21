package group.flowbird.paymentservice.client;

import group.flowbird.paymentservice.client.ClientDTO.InvoiceDTO;
import group.flowbird.paymentservice.configuration.BillingConfiguration;
import nl.yellowbrick.buckarooclient.utils.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BillingRestClient {
    @Autowired
    RestClient restClient;

    @Autowired
    BillingConfiguration billingConfiguration;

    public InvoiceDTO getInvoice(Long invoiceId){
        String url = billingConfiguration.getGetInvoice() + "/" + invoiceId;
        InvoiceDTO invoiceDTO = restClient.performRequest("", HttpMethod.GET, url, InvoiceDTO.class);
        return invoiceDTO;
    }
    public boolean hasOutStandingInvoice(Long customerId){
        String url = billingConfiguration.getHasOutstandingInvoice() + "/" + customerId;
        Boolean result = restClient.performRequest("", HttpMethod.GET, url, Boolean.class);
        return result;
    }

    public Boolean updateInvoice(InvoiceDTO invoiceDTO){
        String url = billingConfiguration.getSendInvoicePaymentInfo();
        restClient.performRequest(RestUtils.mapStringFromObject(invoiceDTO), HttpMethod.POST, url, HttpStatus.class);
        return restClient.getResponseEntity().getStatusCode() == HttpStatus.OK;
    }
}
