package org.apache.fineract.paymenthub.importer.entity.task;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.fineract.paymenthub.importer.entity.parent.AbstractPersistableCustom;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ph_tasks")
public class Task extends AbstractPersistableCustom {

    @Column(name = "WORKFLOW_KEY")
    private Long workflowKey;
    @Column(name = "WORKFLOW_INSTANCE_KEY")
    private Long workflowInstanceKey;
    @Column(name = "TIMESTAMP")
    private Long timestamp;
    @Column(name = "INTENT")
    private String intent;
    @Column(name = "RECORD_TYPE")
    private String recordType;
    @Column(name = "TYPE")
    private String type;
    @Column(name = "ELEMENT_ID")
    private String elementId;

}
