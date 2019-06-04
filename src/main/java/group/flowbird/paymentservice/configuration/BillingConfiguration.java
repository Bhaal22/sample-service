package group.flowbird.paymentservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "billing-rest")
public class BillingConfiguration {
    private String endpointPrefix;
    private String getInvoice;
    private String hasOutstandingInvoice;
    private String sendInvoicePaymentInfo;
}
