package org.apache.fineract.infrastructure.security.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.apache.fineract.core.domain.FineractPlatformTenant;
import org.apache.fineract.core.domain.FineractPlatformTenantConnection;
import org.apache.fineract.infrastructure.security.exception.InvalidTenantIdentiferException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * A JDBC implementation of {@link TenantDetailsService} for loading a tenants details by a
 * <code>tenantIdentifier</code>.
 */
@Service
public class JdbcTenantDetailsService implements TenantDetailsService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcTenantDetailsService(@Qualifier("hikariTenantDataSource") final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final class TenantMapper implements RowMapper<FineractPlatformTenant> {

        private final StringBuilder sqlBuilder = new StringBuilder("t.id, ts.id as connectionId , ")//
                .append(" t.timezone_id as timezoneId , t.name,t.identifier, ts.schema_name as schemaName, ts.schema_server as schemaServer,")//
                .append(" ts.schema_server_port as schemaServerPort, ts.schema_connection_parameters as schemaConnectionParameters, ts.auto_update as autoUpdate,")//
                .append(" ts.schema_username as schemaUsername, ts.schema_password as schemaPassword , ts.pool_initial_size as initialSize,")//
                .append(" ts.pool_validation_interval as validationInterval, ts.pool_remove_abandoned as removeAbandoned, ts.pool_remove_abandoned_timeout as removeAbandonedTimeout,")//
                .append(" ts.pool_log_abandoned as logAbandoned, ts.pool_abandon_when_percentage_full as abandonedWhenPercentageFull, ts.pool_test_on_borrow as testOnBorrow,")//
                .append(" ts.pool_max_active as poolMaxActive, ts.pool_min_idle as poolMinIdle, ts.pool_max_idle as poolMaxIdle,")//
                .append(" ts.pool_suspect_timeout as poolSuspectTimeout, ts.pool_time_between_eviction_runs_millis as poolTimeBetweenEvictionRunsMillis,")//
                .append(" ts.pool_min_evictable_idle_time_millis as poolMinEvictableIdleTimeMillis,")//
                .append(" ts.deadlock_max_retries as maxRetriesOnDeadlock,")//
                .append(" ts.deadlock_max_retry_interval as maxIntervalBetweenRetries ")//
                .append(" from tenants t left join tenant_server_connections ts on t.oltp_Id=ts.id ");

        public String schema() {
            return this.sqlBuilder.toString();
        }

        @Override
        public FineractPlatformTenant mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String tenantIdentifier = rs.getString("identifier");
            final String name = rs.getString("name");
            final String timezoneId = rs.getString("timezoneId");
            final FineractPlatformTenantConnection connection = getDBConnection(rs);

            return new FineractPlatformTenant(id, tenantIdentifier, name, timezoneId, connection);
        }

        // gets the DB connection
        private FineractPlatformTenantConnection getDBConnection(ResultSet rs) throws SQLException {

            final Long connectionId = rs.getLong("connectionId");
            final String schemaName = rs.getString("schemaName");
            final String schemaServer = rs.getString("schemaServer");
            final String schemaServerPort = rs.getString("schemaServerPort");
            final String schemaConnectionParameters = rs.getString("schemaConnectionParameters");
            final String schemaUsername = rs.getString("schemaUsername");
            final String schemaPassword = rs.getString("schemaPassword");
            final boolean autoUpdateEnabled = rs.getBoolean("autoUpdate");
            final int initialSize = rs.getInt("initialSize");
            final boolean testOnBorrow = rs.getBoolean("testOnBorrow");
            final long validationInterval = rs.getLong("validationInterval");
            final boolean removeAbandoned = rs.getBoolean("removeAbandoned");
            final int removeAbandonedTimeout = rs.getInt("removeAbandonedTimeout");
            final boolean logAbandoned = rs.getBoolean("logAbandoned");
            final int abandonWhenPercentageFull = rs.getInt("abandonedWhenPercentageFull");
            final int maxActive = rs.getInt("poolMaxActive");
            final int minIdle = rs.getInt("poolMinIdle");
            final int maxIdle = rs.getInt("poolMaxIdle");
            final int suspectTimeout = rs.getInt("poolSuspectTimeout");
            final int timeBetweenEvictionRunsMillis = rs.getInt("poolTimeBetweenEvictionRunsMillis");
            final int minEvictableIdleTimeMillis = rs.getInt("poolMinEvictableIdleTimeMillis");
            int maxRetriesOnDeadlock = rs.getInt("maxRetriesOnDeadlock");
            int maxIntervalBetweenRetries = rs.getInt("maxIntervalBetweenRetries");

            maxRetriesOnDeadlock = bindValueInMinMaxRange(maxRetriesOnDeadlock, 0, 15);
            maxIntervalBetweenRetries = bindValueInMinMaxRange(maxIntervalBetweenRetries, 1, 15);

            return new FineractPlatformTenantConnection(connectionId, schemaName, schemaServer, schemaServerPort,
                    schemaConnectionParameters, schemaUsername, schemaPassword, autoUpdateEnabled, initialSize, validationInterval,
                    removeAbandoned, removeAbandonedTimeout, logAbandoned, abandonWhenPercentageFull, maxActive, minIdle, maxIdle,
                    suspectTimeout, timeBetweenEvictionRunsMillis, minEvictableIdleTimeMillis, maxRetriesOnDeadlock,
                    maxIntervalBetweenRetries, testOnBorrow);

        }

        private int bindValueInMinMaxRange(final int value, int min, int max) {
            if (value < min) {
                return min;
            } else if (value > max) {
                return max;
            }
            return value;
        }
    }

    @Override
    @Cacheable(value = "tenantsById")
    public FineractPlatformTenant loadTenantById(final String tenantIdentifier) {

        try {
            final TenantMapper rm = new TenantMapper();
            final String sql = "select " + rm.schema() + " where t.identifier = ?";

            return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { tenantIdentifier }); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            throw new InvalidTenantIdentiferException("The tenant identifier: " + tenantIdentifier + " is not valid.", e);
        }
    }

    @Override
    public List<FineractPlatformTenant> findAllTenants() {
        final TenantMapper rm = new TenantMapper();
        final String sql = "select  " + rm.schema();

        final List<FineractPlatformTenant> fineractPlatformTenants = this.jdbcTemplate.query(sql, rm); // NOSONAR
        return fineractPlatformTenants;
    }
}
