package group.flowbird.paymentservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "payment-reminder")
public class PaymentReminderConfiguration {
    private String urlPrefix;
    private String noOutstandingInvoice;
    private String currentInvoicePaidStillOutstanding;
    private String paymentSuccessful;
    private String paymentUnsuccessful;
    private String paymentSuccessfulStillOutstanding;
    private String invalidInvoiceId;
    private String paymentReminderLinkExpired;
    private String serverError;
}
