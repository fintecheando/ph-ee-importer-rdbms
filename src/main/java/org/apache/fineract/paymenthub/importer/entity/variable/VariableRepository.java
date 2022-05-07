package org.apache.fineract.paymenthub.importer.entity.variable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VariableRepository extends JpaRepository<Variable, Long>, JpaSpecificationExecutor<Variable> {

    List<Variable> findByWorkflowInstanceKey(Long workflowInstanceKey);

}
