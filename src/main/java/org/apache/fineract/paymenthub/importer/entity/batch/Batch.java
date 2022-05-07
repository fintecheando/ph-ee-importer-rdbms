package org.apache.fineract.paymenthub.importer.entity.batch;

import org.eclipse.persistence.annotations.Index;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ph_batches")
public class Batch {

    @Id
    private Long id;

    @Column(name = "BATCH_ID")
    private String batchId;

    @Column(name = "REQUEST_ID")
    private String requestId;

    @Column(name = "REQUEST_FILE")
    private String requestFile;

    @Column(name = "TOTAL_TRANSACTIONS")
    private Long totalTransactions;

    @Column(name = "ONGOING")
    private Long ongoing;

    @Column(name = "FAILED")
    private Long failed;

    @Column(name = "COMPLETED")
    private Long completed;

    @Column(name = "RESULT_FILE")
    private String result_file;

    @Column(name = "RESULT_GENERATED_AT")
    private Date resultGeneratedAt;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "WORKFLOW_KEY")
    private Long workflowKey;

    @Column(name = "WORKFLOW_INSTANCE_KEY")
    @Index(name = "idx_workflowInstanceKey")
    private Long workflowInstanceKey;

    @Column(name = "STARTED_AT")
    private Date startedAt;

    @Column(name = "COMPLETED_AT")
    private Date completedAt;

    public Batch(Long workflowInstanceKey) {
        this.workflowInstanceKey = workflowInstanceKey;
    }
}