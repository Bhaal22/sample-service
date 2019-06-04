package group.flowbird.paymentservice.client;

import group.flowbird.paymentservice.configuration.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public static final Logger logger = LoggerFactory.getLogger(YellowSoapRestClient.class);

    public Boolean activateCustomer(Long customerId) throws ResourceAccessException {
        String url = applicationConfiguration.getActivateCustomerEndpointPrefix() + "/" + customerId + "/" + applicationConfiguration.getYellowsoapToken();
        try{
            restClient.performRequest("", HttpMethod.POST, url, String.class);
            return null != restClient.getResponseEntity() &&
                   restClient.getResponseEntity().getStatusCode().equals(HttpStatus.OK);

        }catch (ResourceAccessException ex){
            logger.error("YellowSoap Server is down, please check");
            ex.printStackTrace();
        }catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Some unknown exception occurred, couldn't communicate with yellow  soap");
        }
        return false;
    }
}


