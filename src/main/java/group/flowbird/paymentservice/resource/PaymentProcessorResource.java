package group.flowbird.paymentservice.resource;

import group.flowbird.paymentservice.RestUtil;
import group.flowbird.paymentservice.processor.PaymentProcessor;
import nl.yellowbrick.buckarooclient.dto.BuckarooPushRequest;
import nl.yellowbrick.buckarooclient.utils.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/v1/payment_processor")
public class PaymentProcessorResource {

    @Autowired
    PaymentProcessor paymentProcessor;

    public static final Logger logger = LoggerFactory.getLogger(PaymentProcessorResource.class);

    /**
     *
     * @param id : Test endpoint, provide any Long value as id param
     * @return : expect a simple return string 'OK'
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> getPaymentStatus(@PathVariable("id") Long id){
        logger.info(String.format("Rest Request start : endpoint = /%d", id));
        logger.info(String.format("Rest Request end : endpoint = /%d", id));
        return ResponseEntity.ok().body("OK");
    }


    /**
     *
     * @param callbackKey : callbackKey sent to Buckaroo while creating the request, check IbanValidtionRequest.callbackURL column
     * @param response
     * @return : redirects to proper status message in MyYellowbrick
     */
    @RequestMapping(value = "/process_post_payment/callback/{callbackKey}", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<String> processCallback(@PathVariable("callbackKey") String callbackKey, HttpServletResponse response){
        logger.info(String.format("Rest Request start : endpoint = %s,  callbackKey = %s", "/process_post_payment/callback/{callbackKey}", callbackKey));
        String redirectURL = paymentProcessor.processCallbackByCallbackKey(callbackKey);
        logger.info(String.format("Rest Request end : endpoint = %s,  callbackKey = %s", "/process_post_payment/callback/{callbackKey}", callbackKey));
        return RestUtil.redirectToURL(redirectURL, response);
    }

    /**
     *
     * @param status : success or fail
     * @param jsonBody : contains TransactionResponse JsonString
     * @return : nothing.
     */
    @PostMapping("/process_post_payment/push/{status}")
    public ResponseEntity processPushURL(@PathVariable("status") String status, @RequestBody String jsonBody){
        logger.info(String.format("Rest Request start : endpoint = %s/%s", "/process_post_payment/push", status));
        if(null == status || !status.equals("success") && !status.equals("fail")){
            return ResponseEntity.badRequest().body("Invalid Endpoint");
        }
        BuckarooPushRequest request = RestUtils.mapObjectFromString(jsonBody, BuckarooPushRequest.class);
        if(null == request || null == request.getTransaction() || null == request.getTransactionId()){
           return ResponseEntity.badRequest().body("Invalid Data");
        }
        paymentProcessor.processPushURL(request);
        logger.info(String.format("Rest Request end : endpoint = %s/%s", "/process_post_payment/push", status));
        return ResponseEntity.ok("Processed");
    }

    /**
     * initiates the buckaroo transaction and redirects the user to Buckaroo Payment gateway
     * @param invoiceId : FactuurId,
     * @param response
     * @return
     */
    @GetMapping("/initiate_payment/{invoiceId}")
    public ResponseEntity<String> initiatePayment(@PathVariable("invoiceId") Long invoiceId, HttpServletResponse response){
        logger.info(String.format("Rest Request start : endpoint = %s/%d", "/initiate_payment", invoiceId));
        String redirectURL = paymentProcessor.processRedirectURL(invoiceId);
        logger.info(String.format("Rest Request end : endpoint = %s/%d", "/initiate_payment", invoiceId));
        return RestUtil.redirectToURL(redirectURL, response);
    }
}
