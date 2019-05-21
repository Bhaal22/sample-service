package group.flowbird.paymentservice.client;

import group.flowbird.paymentservice.configuration.ApplicationConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
public class YellowSoapRestClient {

    @Autowired
    RestClient restClient;

    @Autowired
    ApplicationConfiguration applicationConfiguration;

    public Boolean activateCustomer(Long customerId) throws ResourceAccessException {
        String url = applicationConfiguration.getActivateCustomerEndpointPrefix() + "/" + customerId + "/" + applicationConfiguration.getYellowsoapToken();
        restClient.performRequest("", HttpMethod.POST, url, String.class);
        return restClient.getResponseEntity().getStatusCode() == HttpStatus.OK;
    }
}


