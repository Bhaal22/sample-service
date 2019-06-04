package group.flowbird.paymentservice.client;

import lombok.Getter;
import nl.yellowbrick.buckarooclient.utils.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


@Component
public class RestClient {
    public static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    @Autowired
    RestTemplate restTemplate;
    @Getter
    ResponseEntity<String> responseEntity;


    public boolean remoteServerErrorOccurred = false;

    private HttpEntity<String> getEntitiTemplate(String paramString){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json;charset=UTF-8");
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(paramString, headers);
        return entity;
    }

    protected <T> T performRequest(String params, HttpMethod httpMethod, String serviceUrl, Class<T> classType){
        remoteServerErrorOccurred = false;
        HttpEntity<String> entity = getEntitiTemplate(params);
        try {
            responseEntity = null;
            switch (httpMethod){
                case GET:
                    responseEntity = restTemplate.exchange(serviceUrl, HttpMethod.GET, entity, String.class);
                    break;
                case POST:
                    responseEntity = restTemplate.postForEntity(serviceUrl, entity, String.class);
                    break;
                default:
                    logger.error("Not implemented yet");
                    break;
            }
            String responseString = responseEntity.getBody();
            if(null == responseString)return null;

            return RestUtils.mapObjectFromString(responseString, classType);
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            logger.error("HttpClientError Exception occurred, msg = " + e.getMessage());
            remoteServerErrorOccurred = true;
        }catch (ResourceAccessException ex){
            ex.printStackTrace();
            logger.error("Remote server is down. msg = " + ex.getMessage());
            remoteServerErrorOccurred = true;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error("Unexpected error happened , msg = ", e.getMessage());
            remoteServerErrorOccurred = true;
        }
        return null;
    }
}
