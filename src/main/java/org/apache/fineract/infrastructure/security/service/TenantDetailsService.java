package org.apache.fineract.infrastructure.security.service;

import java.util.List;
import org.apache.fineract.core.domain.FineractPlatformTenant;

public interface TenantDetailsService {

    FineractPlatformTenant loadTenantById(String tenantId);

    List<FineractPlatformTenant> findAllTenants();
}