package org.apache.fineract.infrastructure.core.service.database;

import javax.sql.DataSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public interface DatabaseQueryService {

    boolean isSupported();

    boolean isTablePresent(DataSource dataSource, String tableName);

    // TODO: This needs to be improved to have a custom POJO return type instead of the raw SqlRowSet
    SqlRowSet getTableColumns(DataSource dataSource, String tableName);
}
