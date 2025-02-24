package org.apache.fineract.core.service;

import static org.apache.fineract.core.domain.FineractPlatformTenantConnection.toJdbcUrl;
import static org.apache.fineract.core.domain.FineractPlatformTenantConnection.toProtocol;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.fineract.core.domain.FineractPlatformTenant;
import org.apache.fineract.core.domain.FineractPlatformTenantConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Implementation that returns a new or existing connection pool datasource based on the tenant details stored in a
 * {@link ThreadLocal} variable for this request.
 *
 * {@link ThreadLocalContextUtil} is used to retrieve the {@link FineractPlatformTenant} for the request.
 */
@Service
public class TomcatJdbcDataSourcePerTenantService implements RoutingDataSourceService {

    private final Map<Long, DataSource> tenantToDataSourceMap = new HashMap<>(1);
    private final DataSource tenantDataSource;

    @Autowired
    private HikariConfig hikariConfig;

    @Autowired
    public TomcatJdbcDataSourcePerTenantService(final @Qualifier("hikariTenantDataSource") DataSource tenantDataSource) {
        this.tenantDataSource = tenantDataSource;
    }

    @Override
    public DataSource retrieveDataSource() {
        // default to tenant database datasource
        DataSource tenantDataSource = this.tenantDataSource;

        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
        if (tenant != null) {
            final FineractPlatformTenantConnection tenantConnection = tenant.getConnection();

            synchronized (this.tenantToDataSourceMap) {
                // if tenantConnection information available switch to the
                // appropriate datasource for that tenant.
                DataSource possibleDS = this.tenantToDataSourceMap.get(tenantConnection.getConnectionId());
                if (possibleDS != null) {
                    tenantDataSource = possibleDS;
                } else {
                    tenantDataSource = createNewDataSourceFor(tenantConnection);
                    this.tenantToDataSourceMap.put(tenantConnection.getConnectionId(), tenantDataSource);
                }
            }
        }

        return tenantDataSource;
    }

    // creates the tenant data source for the oltp and report database
    private DataSource createNewDataSourceFor(final FineractPlatformTenantConnection tenantConnectionObj) {
        String protocol = toProtocol(this.tenantDataSource);
        String jdbcUrl = toJdbcUrl(protocol, tenantConnectionObj.getSchemaServer(), tenantConnectionObj.getSchemaServerPort(),
                tenantConnectionObj.getSchemaName(), tenantConnectionObj.getSchemaConnectionParameters());

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(hikariConfig.getDriverClassName());
        config.setPoolName(tenantConnectionObj.getSchemaName() + "_pool");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(tenantConnectionObj.getSchemaUsername());
        config.setPassword(tenantConnectionObj.getSchemaPassword());
        config.setMinimumIdle(tenantConnectionObj.getInitialSize());
        config.setMaximumPoolSize(tenantConnectionObj.getMaxActive());
        config.setConnectionTestQuery(hikariConfig.getConnectionTestQuery());
        config.setValidationTimeout(tenantConnectionObj.getValidationInterval());
        config.setAutoCommit(hikariConfig.isAutoCommit());

        // https://github.com/brettwooldridge/HikariCP/wiki/MBean-(JMX)-Monitoring-and-Management
        config.setRegisterMbeans(true);

        // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        // These are the properties for each Tenant DB; the same configuration
        // is also in src/main/resources/META-INF/spring/hikariDataSource.xml
        // for the all Tenants DB -->
        config.setDataSourceProperties(hikariConfig.getDataSourceProperties());

        return new HikariDataSource(config);
    }
}