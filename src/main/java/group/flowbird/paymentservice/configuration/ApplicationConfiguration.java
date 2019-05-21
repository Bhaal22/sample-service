package group.flowbird.paymentservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "app")
public class ApplicationConfiguration {
    private String name;
    private String serverIp;
    private String billingServerPrefix;
    private String myYellowbrickServerPrefix;
    private String yellowsoapServerPrefix;
    private String activateCustomerEndpointPrefix;
    private String yellowsoapToken;
}
