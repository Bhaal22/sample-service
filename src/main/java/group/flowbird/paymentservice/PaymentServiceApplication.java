package group.flowbird.paymentservice;

import com.sun.org.apache.bcel.internal.generic.DADD;
import group.flowbird.paymentservice.client.BillingRestClient;
import group.flowbird.paymentservice.client.ClientDTO.InvoiceDTO;
import group.flowbird.paymentservice.client.YellowSoapRestClient;
import group.flowbird.paymentservice.configuration.DataSourceConfiguration;
import group.flowbird.paymentservice.processor.PaymentProcessor;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@SpringBootApplication
@ComponentScan(basePackages = {"group.flowbird"})
@Configuration
public class PaymentServiceApplication {

    @Autowired
    IbanValidationService ibanValidationService;
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceApplication.class);
	public static void main(String[] args) {
        logger.info("Starting Spring Boot application : PaymentService");
        ConfigurableApplicationContext context = SpringApplication.run(PaymentServiceApplication.class, args);

        PaymentProcessor paymentProcessor = context.getBean(PaymentProcessor.class);
        YellowSoapRestClient yellowSoapRestClient = context.getBean(YellowSoapRestClient.class);
        yellowSoapRestClient.activateCustomer(234353L);
        //paymentProcessor.processRedirectURL(6354551L);
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
