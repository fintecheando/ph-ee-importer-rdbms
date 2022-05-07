package org.apache.fineract.paymenthub.importer.config;

import java.util.Map;
import org.apache.fineract.infrastructure.core.persistence.DatabaseSelectingPersistenceUnitPostProcessor;
import org.apache.fineract.infrastructure.core.persistence.ExtendedJpaTransactionManager;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.core.service.database.DatabaseTypeResolver;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;

@Configuration
@EntityScan(basePackages = "org.apache.fineract")
@EnableJpaRepositories(basePackages = "org.apache.fineract")
@EnableConfigurationProperties(JpaProperties.class)
public class JpaConfig extends JpaBaseConfiguration {

    private final DatabaseTypeResolver databaseTypeResolver;

    public JpaConfig(RoutingDataSource dataSource, JpaProperties properties,
            ObjectProvider<JtaTransactionManager> jtaTransactionManager,
            DatabaseTypeResolver databaseTypeResolver) {
        super(dataSource, properties, jtaTransactionManager);
        this.databaseTypeResolver = databaseTypeResolver;
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(PersistenceUnitProperties.WEAVING, "static");
        /*
        map.put(PersistenceUnitProperties.WEAVING, "static");
        map.put(PersistenceUnitProperties.DDL_GENERATION, "none");
        map.put(PersistenceUnitProperties.LOGGING_LEVEL, "INFO");
        map.put(PersistenceUnitProperties.DDL_GENERATION_MODE, "sql-script");
        map.put("eclipselink.jdbc.batch-writing", "JDBC");
        map.put("eclipselink.jdbc.batch-writing.size", "1000");
        map.put("eclipselink.cache.shared.default", "false");

        map.put("eclipselink.logging.level.sql", "INFO");
        map.put("eclipselink.logging.parameters", "true");
        map.put("eclipselink.logging.session", "true");
        map.put("eclipselink.logging.thread", "true");
        map.put("eclipselink.logging.timestamp", "false");
        */
        return map;
    }

    @Override
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter,
            ObjectProvider<PersistenceUnitManager> persistenceUnitManager,
            ObjectProvider<EntityManagerFactoryBuilderCustomizer> customizers) {
        EntityManagerFactoryBuilder builder = super.entityManagerFactoryBuilder(jpaVendorAdapter, persistenceUnitManager, customizers);
        builder.setPersistenceUnitPostProcessors(new DatabaseSelectingPersistenceUnitPostProcessor(databaseTypeResolver));
        return builder;
    }

    @Override
    @Bean
    public PlatformTransactionManager transactionManager(ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        ExtendedJpaTransactionManager transactionManager = new ExtendedJpaTransactionManager();
        transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
        return transactionManager;
    }

    @Bean
    public TransactionTemplate txTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate tt = new TransactionTemplate();
        tt.setTransactionManager(transactionManager);
        return tt;
    }
}
