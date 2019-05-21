package group.flowbird.paymentservice.configuration;


import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;

@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfiguration {

    @Autowired
    private DataSourceProperties properties;

    @Bean
    public DataSource dataSource() throws PropertyVetoException {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setUser(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setJdbcUrl(properties.getUrl());
        ds.setDriverClass(properties.getDriverClassName());
        return ds;
    }
}
