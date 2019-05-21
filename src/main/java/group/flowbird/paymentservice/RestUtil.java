package group.flowbird.paymentservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RestUtil {

    public static final Logger logger = LoggerFactory.getLogger(RestUtil.class);
    public static ResponseEntity<String> redirectToURL(String redirectURL, HttpServletResponse response){
        if(null == redirectURL){
            logger.error("Couldn't process initiate payment request correctly, something went wrong");
            return ResponseEntity.ok().body("Something went wrong, please try again later");
        }
        try {
            response.sendRedirect(redirectURL);
        }catch (IOException e){
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Service Temporarily Unavailable, Please try again later");
    }
}
