package org.apache.fineract.infrastructure.core.persistence;

import java.util.Map;
import org.apache.fineract.infrastructure.core.service.database.DatabaseType;
import org.apache.fineract.infrastructure.core.service.database.DatabaseTypeResolver;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

public class DatabaseSelectingPersistenceUnitPostProcessor implements PersistenceUnitPostProcessor {

    private static final Map<DatabaseType, String> TARGET_DATABASE_MAP = Map.of(DatabaseType.MYSQL, TargetDatabase.MySQL,
            DatabaseType.POSTGRESQL, TargetDatabase.PostgreSQL);

    private final DatabaseTypeResolver databaseTypeResolver;

    public DatabaseSelectingPersistenceUnitPostProcessor(DatabaseTypeResolver databaseTypeResolver) {
        this.databaseTypeResolver = databaseTypeResolver;
    }

    @Override
    public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
        DatabaseType databaseType = databaseTypeResolver.databaseType();
        String targetDatabase = TARGET_DATABASE_MAP.get(databaseType);
        if (targetDatabase == null) {
            throw new IllegalStateException("Unsupported database: " + databaseType);
        }
        pui.addProperty(PersistenceUnitProperties.TARGET_DATABASE, targetDatabase);
    }
}
