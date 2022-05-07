package org.apache.fineract.paymenthub.importer.entity.businesskey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BusinessKeyRepository extends JpaRepository<BusinessKey, Long>, JpaSpecificationExecutor<BusinessKey> {

    List<BusinessKey> findByBusinessKeyAndBusinessKeyType(String businessKey, String businessKeyType);

}
