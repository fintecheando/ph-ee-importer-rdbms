package org.apache.fineract.infrastructure.core.service.database;

import static java.lang.String.format;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
public class MySQLQueryService implements DatabaseQueryService {
    private static final Logger LOG = LoggerFactory.getLogger(MySQLQueryService.class);

    private final DatabaseTypeResolver databaseTypeResolver;

    @Autowired
    public MySQLQueryService(DatabaseTypeResolver databaseTypeResolver) {
        this.databaseTypeResolver = databaseTypeResolver;
    }

    @Override
    public boolean isSupported() {
        return databaseTypeResolver.isMySQL();
    }

    @Override
    public boolean isTablePresent(DataSource dataSource, String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        SqlRowSet rs = jdbcTemplate.queryForRowSet(format("SHOW TABLES LIKE '%s'", tableName));
        final boolean isPresent = rs.next();
        try {
            LOG.info("Looking Table: {} in {} : {}", tableName, dataSource.getConnection().getSchema(), isPresent);
        } catch (SQLException e) {}
        return isPresent;
    }

    @Override
    public SqlRowSet getTableColumns(DataSource dataSource, String tableName) {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final String sql = "SELECT c.COLUMN_NAME, c.IS_NULLABLE, c.DATA_TYPE, c.CHARACTER_MAXIMUM_LENGTH, c.COLUMN_KEY FROM INFORMATION_SCHEMA.COLUMNS c WHERE TABLE_SCHEMA = schema() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";

        final SqlRowSet columnDefinitions = jdbcTemplate.queryForRowSet(sql, new Object[] { tableName }); // NOSONAR
        if (columnDefinitions.next()) {
            return columnDefinitions;
        } else {
            throw new IllegalArgumentException("Table " + tableName + " is not found");
        }
    }
}
