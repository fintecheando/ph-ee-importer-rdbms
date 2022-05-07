package org.apache.fineract.infrastructure.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("#{ systemEnvironment['fineract_tenants_driver'] == null }")
public class HikariCpConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean(destroyMethod = "close")
    public DataSource hikariTenantDataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }
}