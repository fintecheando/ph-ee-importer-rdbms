package org.apache.fineract.infrastructure.core.service.database;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.sql.SQLException;
import java.util.Collection;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndependentQueryService implements DatabaseQueryService {

    private final Collection<DatabaseQueryService> queryServices;

    @Autowired
    public DatabaseIndependentQueryService(Collection<DatabaseQueryService> queryServices) {
        this.queryServices = queryServices;
    }

    private DatabaseQueryService choose(DataSource dataSource) {
        try {
            DatabaseQueryService result = null;
            if (isNotEmpty(queryServices)) {
                result = queryServices.stream().filter(DatabaseQueryService::isSupported).findAny().orElse(null);
            }
            if (result == null) {
                throw new IllegalStateException("DataSource not supported: " + dataSource.getConnection().getMetaData().getURL());
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error while trying to choose the proper query service", e);
        }
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean isTablePresent(DataSource dataSource, String tableName) {
        return choose(dataSource).isTablePresent(dataSource, tableName);
    }

    @Override
    public SqlRowSet getTableColumns(DataSource dataSource, String tableName) {
        return choose(dataSource).getTableColumns(dataSource, tableName);
    }
}
