package org.apache.fineract.paymenthub.importer.entity.businesskey;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.fineract.paymenthub.importer.entity.parent.AbstractPersistableCustom;
import org.eclipse.persistence.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ph_businesskeys")
public class BusinessKey extends AbstractPersistableCustom {

    @Column(name = "BUSINESS_KEY")
    @Index(name = "idx_businessKey")
    private String businessKey;

    @Column(name = "BUSINESS_KEY_TYPE")
    @Index(name = "idx_businessKeyType")
    private String businessKeyType;

    @Column(name = "WORKFLOW_INSTANCE_KEY")
    @Index(name = "idx_workflowInstanceKey")
    private Long workflowInstanceKey;

    @Column(name = "TIMESTAMP")
    private Long timestamp;

}
