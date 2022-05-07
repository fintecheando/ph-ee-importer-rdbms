package org.apache.fineract.paymenthub.importer.importer;

import com.jayway.jsonpath.DocumentContext;

import lombok.RequiredArgsConstructor;

import org.apache.fineract.core.domain.FineractPlatformTenant;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KafkaConsumer implements ConsumerSeekAware {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${importer.kafka.topic}")
    private String kafkaTopic;

    @Value("${importer.kafka.reset}")
    private boolean reset;

    private final RecordParser recordParser;
    private final TenantDetailsService tenantDetailsService;
    private final TempDocumentStore tempDocumentStore;

    @KafkaListener(topics = "${importer.kafka.topic}")
    public void listen(String rawData) {
        try {
            DocumentContext incomingRecord = JsonPathReader.parse(rawData);
            logger.debug("from kafka: {}", incomingRecord.jsonString());
            if("DEPLOYMENT".equals(incomingRecord.read("$.valueType"))) {
                logger.info("Deployment event arrived for bpmn: {}, skip processing", incomingRecord.read("$.value.deployedWorkflows[0].bpmnProcessId", String.class));
                return;
            }

            if(incomingRecord.read("$.valueType").equals("VARIABLE_DOCUMENT")) {
                logger.info("Skipping VARIABLE_DOCUMENT record ");
                return;
            }

            Long workflowKey = incomingRecord.read("$.value.processDefinitionKey");
            String bpmnprocessIdWithTenant = incomingRecord.read("$.value.bpmnProcessId");
            Long recordKey = incomingRecord.read("$.key");

            if(bpmnprocessIdWithTenant == null) {
                bpmnprocessIdWithTenant = tempDocumentStore.getBpmnprocessId(workflowKey);
                if (bpmnprocessIdWithTenant == null) {
                    tempDocumentStore.storeDocument(workflowKey, incomingRecord);
                    logger.info("Record with key {} workflowkey {} has no associated bpmn, stored temporarly", recordKey, workflowKey);
                    return;
                }
            } else {
                tempDocumentStore.setBpmnprocessId(workflowKey, bpmnprocessIdWithTenant);
            }

            String tenantName = bpmnprocessIdWithTenant.substring(bpmnprocessIdWithTenant.indexOf("-") + 1);
            String bpmnprocessId = bpmnprocessIdWithTenant.substring(0, bpmnprocessIdWithTenant.indexOf("-"));
            FineractPlatformTenant tenant = tenantDetailsService.loadTenantById(tenantName);

            ThreadLocalContextUtil.setTenant(tenant);

            List<DocumentContext> documents = new ArrayList<>();
            List<DocumentContext> storedDocuments = tempDocumentStore.takeStoredDocuments(workflowKey);
            if(!storedDocuments.isEmpty()) {
                logger.info("Reprocessing {} previously stored records with workflowKey {}", storedDocuments.size(), workflowKey);
                documents.addAll(storedDocuments);
            }
            documents.add(incomingRecord);

            for(DocumentContext doc : documents) {
                try {
                    String valueType = doc.read("$.valueType");
                    switch (valueType) {
                        case "VARIABLE":
                            DocumentContext processedVariable = recordParser.processVariable(doc); // TODO prepare for parent workflow
                            recordParser.addVariableToEntity(processedVariable, bpmnprocessId); // Call to store transfer
                            break;
                        case "JOB":
                            recordParser.processTask(doc);
                            break;
                        case "PROCESS_INSTANCE":
                            if ("PROCESS".equals(doc.read("$.value.bpmnElementType"))) {
                                recordParser.processWorkflowInstance(doc);
                            }
                            break;
                    }
                } catch (Exception ex) {
                    logger.error("Failed to process document:\n{}\nerror: {}\ntrace: {}",
                            doc,
                            ex.getMessage(),
                            limitStackTrace(ex));
                    tempDocumentStore.storeDocument(workflowKey, doc);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not parse zeebe event:\n{}\nerror: {}\ntrace: {}",
                    rawData,
                    ex.getMessage(),
                    limitStackTrace(ex));
        } finally {
            ThreadLocalContextUtil.clearTenant();
        }
    }

    private String limitStackTrace(Exception ex) {
        return Arrays.stream(ex.getStackTrace())
                .limit(10)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        if (reset) {
            assignments.keySet().stream()
                    .filter(partition -> partition.topic().equals(kafkaTopic))
                    .forEach(partition -> {
                        callback.seekToBeginning(partition.topic(), partition.partition());
                        logger.info("seeked {} to beginning", partition);
                    });
        } else {
            logger.info("no reset, consuming kafka topics from latest");
        }
    }
}
