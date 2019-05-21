package group.flowbird.paymentservice.resource;

import group.flowbird.paymentservice.processor.PaymentProcessor;
import nl.yellowbrick.buckarooclient.dto.BuckarooPushRequest;
import nl.yellowbrick.buckarooclient.utils.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@RestController
@RequestMapping("/v1/payment_processor")
public class PaymentProcessorResource {

    @Autowired
    PaymentProcessor paymentProcessor;

    public static final Logger logger = LoggerFactory.getLogger(PaymentProcessorResource.class);

    @GetMapping("/{id}")
    public ResponseEntity<String> getPaymentStatus(@PathVariable("id") Long id){
        return ResponseEntity.ok().body("OK");
    }


    @GetMapping("/process_post_payment/callback/{callbackKey}")
    public ResponseEntity<String> processCallback(@PathVariable("callbackKey") String callbackKey, HttpServletResponse response){
        String redirectURL = paymentProcessor.processCallbackByCallbackKey(callbackKey);
        return redirectToURL(redirectURL, response);
    }


    /**
     * This end point should only be called to process payment through push url
     *
     * @param status
     * @param jsonBody
     * @return HTTP response code 200 when message processed successfully
     */
    /*
    @POST
    @Unauthenticated
    @Path("/{status : (success|fail)}")
    public Response pushSuccess(@PathParam("status") String status, @RequestBody String jsonBody){
        logger.info("Received Push Request from Buckaroo...");
        BuckarooPushRequest request = RestUtils.mapObjectFromString(jsonBody, BuckarooPushRequest.class);
        if(null == request || null == request.getTransactionId()){
            logger.info("Invalid Push Request content");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        paymentInfoService.processTransactions(request, status.equals("success"));
        return Response.status(Response.Status.OK).build();
    }
    */

    @PostMapping("/process_post_payment/push/{status : (success|fail)}")
    public void processPushURL(@PathVariable("status") String status, @RequestBody String jsonBody){
        BuckarooPushRequest request = RestUtils.mapObjectFromString(jsonBody, BuckarooPushRequest.class);
        paymentProcessor.processPushURL(request);
    }

    @GetMapping("/initiate_payment/{invoiceId}")
    public ResponseEntity<String> initiatePayment(@PathVariable("invoiceId") Long invoiceId, HttpServletResponse response){
        logger.info("Initiating payment for the invoice : " + invoiceId);
        String redirectURL = paymentProcessor.processRedirectURL(invoiceId);
        return redirectToURL(redirectURL, response);
    }


    private ResponseEntity<String> redirectToURL(String redirectURL, HttpServletResponse response){
        if(null == redirectURL){
            logger.error("Couldn't process initiate payment request correctly, something went wrong");
            return ResponseEntity.ok().body("Something went wrong, please try again later");
        }
        try {
            response.sendRedirect(redirectURL);
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
