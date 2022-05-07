package org.apache.fineract.paymenthub.importer.entity.transactionrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRequestRepository extends JpaRepository<TransactionRequest, Long>, JpaSpecificationExecutor<TransactionRequest> {

    TransactionRequest findByWorkflowInstanceKey(Long workflowInstanceKey);

}
