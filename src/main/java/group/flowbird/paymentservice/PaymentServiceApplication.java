package group.flowbird.paymentservice;

import group.flowbird.paymentservice.configuration.ApplicationConfiguration;
import group.flowbird.paymentservice.configuration.PaymentReminderConfiguration;
import nl.yellowbrick.buckarooclient.dao.ConfigDao;
import nl.yellowbrick.buckarooclient.dao.IbanValidationDao;
import nl.yellowbrick.buckarooclient.dao.impl.ConfigDaoImpl;
import nl.yellowbrick.buckarooclient.dao.impl.IbanValidationDaoImpl;
import nl.yellowbrick.buckarooclient.service.IbanValidationService;
import nl.yellowbrick.buckarooclient.service.impl.IbanValidationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
@ComponentScan(basePackages = {"group.flowbird"})
@Configuration
public class PaymentServiceApplication {

    @Autowired
    ApplicationConfiguration applicationConfiguration;

    @Autowired
    PaymentReminderConfiguration paymentReminderConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceApplication.class);
	public static void main(String[] args) {
        logger.info("Starting Spring Boot application : PaymentService");
        ConfigurableApplicationContext context = SpringApplication.run(PaymentServiceApplication.class, args);
	}

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public IbanValidationService ibanValidationService(){
        return new IbanValidationServiceImpl();
    }

    @Bean
    public ConfigDao configDao(){
	    return new ConfigDaoImpl();
    }

    @Bean
    public IbanValidationDao ibanValidationDao(){
	    return new IbanValidationDaoImpl();
    }
}
