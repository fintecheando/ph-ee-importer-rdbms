package org.apache.fineract.paymenthub.importer.importer;

import com.jayway.jsonpath.DocumentContext;

import lombok.RequiredArgsConstructor;

import org.apache.fineract.paymenthub.importer.config.BpmnProcess;
import org.apache.fineract.paymenthub.importer.config.BpmnProcessProperties;
import org.apache.fineract.paymenthub.importer.entity.batch.Batch;
import org.apache.fineract.paymenthub.importer.entity.batch.BatchRepository;
import org.apache.fineract.paymenthub.importer.entity.task.Task;
import org.apache.fineract.paymenthub.importer.entity.task.TaskRepository;
import org.apache.fineract.paymenthub.importer.entity.transactionrequest.TransactionRequest;
import org.apache.fineract.paymenthub.importer.entity.transactionrequest.TransactionRequestRepository;
import org.apache.fineract.paymenthub.importer.entity.transfer.Transfer;
import org.apache.fineract.paymenthub.importer.entity.transfer.TransferRepository;
import org.apache.fineract.paymenthub.importer.entity.variable.Variable;
import org.apache.fineract.paymenthub.importer.entity.variable.VariableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RecordParser {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${bpmn.transfer-type}")
    private String transferType;

    @Value("${bpmn.transaction-request-type}")
    private String transactionRequestType;

    @Value("${bpmn.batch-type}")
    private String batchType;

    @Value("${bpmn.outgoing-direction}")
    private String outgoingDirection;

    private final TaskRepository taskRepository;
    private final VariableRepository variableRepository;
    private final TransferRepository transferRepository;
    private final TransactionRequestRepository transactionRequestRepository;
    private final BatchRepository batchRepository;
    private final BpmnProcessProperties bpmnProcessProperties;
    private final InflightTransferManager inflightTransferManager;
    private final InflightTransactionRequestManager inflightTransactionRequestManager;
    private final InflightBatchManager inflightBatchManager;
    private final VariableParser variableParser;

    private final Map<Long, Long> inflightCallActivities = new ConcurrentHashMap<>();

    public void addVariableToEntity(DocumentContext newVariable, String bpmnProcessId) {
        logger.debug("newVariable in RecordParser: {}", newVariable.jsonString()); //
        if (newVariable != null) {

            String name = newVariable.read("$.value.name");
            Long workflowInstanceKey = newVariable.read("$.value.processInstanceKey");
            if (inflightCallActivities.containsKey(workflowInstanceKey)) {
                Long parentInstanceKey = inflightCallActivities.get(workflowInstanceKey);
                logger.debug("variable {} in instance {} has parent workflowInstance {}", name, workflowInstanceKey,
                        parentInstanceKey);
                workflowInstanceKey = parentInstanceKey;
            }

            BpmnProcess bpmnProcess = bpmnProcessProperties.getById(bpmnProcessId);
            if (transferType.equals(bpmnProcess.getType())) {
                if (variableParser.getTransferParsers().containsKey(name)) {
                    logger.debug("add variable {} to transfer for workflow {}", name, workflowInstanceKey);
                    String value = newVariable.read("$.value.value");

                    Transfer transfer = inflightTransferManager.getOrCreateTransfer(workflowInstanceKey);
                    variableParser.getTransferParsers().get(name).accept(Pair.of(transfer, value));
                    transferRepository.save(transfer);
                }
            } else if (transactionRequestType.equals(bpmnProcess.getType())) {
                if (variableParser.getTransactionRequestParsers().containsKey(name)) {
                    logger.debug("add variable to transactionRequest {} for workflow {}", name, workflowInstanceKey);
                    String value = newVariable.read("$.value.value");

                    TransactionRequest transactionRequest = inflightTransactionRequestManager
                            .getOrCreateTransactionRequest(workflowInstanceKey);
                    variableParser.getTransactionRequestParsers().get(name).accept(Pair.of(transactionRequest, value));
                    if (transactionRequest.getDirection() == null) {
                        transactionRequest.setDirection(bpmnProcess.getDirection());
                    }
                    transactionRequestRepository.save(transactionRequest);
                }
            } else if (batchType.equals(bpmnProcess.getType())) {
                if (variableParser.getBatchParsers().containsKey(name)) {
                    logger.debug("add variable {} to batch for workflow {}", name, workflowInstanceKey);
                    String value = newVariable.read("$.value.value");

                    Batch batch = inflightBatchManager.getOrCreateBatch(workflowInstanceKey);
                    variableParser.getBatchParsers().get(name).accept(Pair.of(batch, value));
                    batchRepository.save(batch);
                }
            } else {
                logger.debug("Skip adding variable to {} and type is {}", bpmnProcessId, bpmnProcess.getType()); // xx
            }
        }
    }

    public DocumentContext processVariable(DocumentContext json) {
        Long workflowInstanceKey = json.read("$.value.processInstanceKey");
        String name = json.read("$.value.name");
        Long newTimestamp = json.read("$.timestamp");
        List<Variable> existingVariables = variableRepository.findByWorkflowInstanceKey(workflowInstanceKey);
        if (existingVariables != null && !existingVariables.isEmpty()) {
            if (existingVariables.stream().filter(existing -> {
                return name.equals(existing.getName()) && newTimestamp <= existing.getTimestamp(); // variable already
                                                                                                   // inserted before
            }).findFirst().orElse(null) != null) {
                logger.debug("Variable {} already inserted at {} for instance {}, skip processing!", name, newTimestamp,
                        workflowInstanceKey);
                return null;
            }
        }

        Variable variable = new Variable();
        variable.setWorkflowInstanceKey(workflowInstanceKey);
        variable.setTimestamp(newTimestamp);
        variable.setWorkflowKey(json.read("$.value.processDefinitionKey"));
        variable.setName(name);
        String value = json.read("$.value.value");
        variable.setValue(value);
        variableRepository.save(variable);
        return json;
    }

    public void processWorkflowInstance(DocumentContext json) {
        String bpmnProcessId = json.read("$.value.bpmnProcessId");
        BpmnProcess bpmnProcess = bpmnProcessProperties.getById(bpmnProcessId.split("-")[0]);
        Long workflowInstanceKey = json.read("$.value.processInstanceKey");
        Long timestamp = json.read("$.timestamp");
        String intent = json.read("$.intent");
        Object parentWorkflowInstanceKey = json.read("$.value.parentProcessInstanceKey");
        boolean hasParent = false;
        if (parentWorkflowInstanceKey instanceof Long && (Long) parentWorkflowInstanceKey > 0) {
            hasParent = true;
        }

        Long callActivityKey = json.read("$.key");

        if (transferType.equals(bpmnProcess.getType())) {
            if ("ELEMENT_ACTIVATING".equals(intent)) {
                if (hasParent) {
                    logger.debug("Sub process {} with key {} started from parent instance {}", bpmnProcessId,
                            callActivityKey, parentWorkflowInstanceKey);
                    inflightCallActivities.put(callActivityKey, (Long) parentWorkflowInstanceKey);
                    inflightTransferManager.transferStarted((Long) parentWorkflowInstanceKey, timestamp,
                            outgoingDirection);
                } else {
                    inflightTransferManager.transferStarted(workflowInstanceKey, timestamp, bpmnProcess.getDirection());
                }
            } else if ("ELEMENT_COMPLETED".equals(intent)) {
                if (inflightCallActivities.containsKey(workflowInstanceKey)) {
                    Long parentInstanceKey = inflightCallActivities.remove(workflowInstanceKey);
                    logger.debug("Sub process {} with key {} ended from parent instance {}", bpmnProcessId,
                            callActivityKey, parentInstanceKey);
                    workflowInstanceKey = parentInstanceKey;
                }
                inflightTransferManager.transferEnded(workflowInstanceKey, timestamp);
            }
        } else if (transactionRequestType.equals(bpmnProcess.getType())) {
            if ("ELEMENT_ACTIVATING".equals(intent)) {
                inflightTransactionRequestManager.transactionRequestStarted(workflowInstanceKey, timestamp,
                        bpmnProcess.getDirection());
            } else if ("ELEMENT_COMPLETED".equals(intent)) {
                inflightTransactionRequestManager.transactionRequestEnded(workflowInstanceKey, timestamp);
            }
        } else if (batchType.equals(bpmnProcess.getType())) {
            if ("ELEMENT_ACTIVATING".equals(intent)) {
                inflightBatchManager.batchStarted(workflowInstanceKey, timestamp, bpmnProcess.getDirection());
            } else if ("ELEMENT_COMPLETED".equals(intent)) {
                inflightBatchManager.batchEnded(workflowInstanceKey, timestamp);
            }
        } else {
            logger.error("Skip parsing bpmnProcess: {}", bpmnProcessId);
        }
    }

    public void processTask(DocumentContext json) {
        String type = json.read("$.value.type");
        if (type != null) {
            Long workflowInstanceKey = json.read("$.value.processInstanceKey");
            String newElementId = json.read("$.value.elementId");
            Long newTimestamp = json.read("$.timestamp");
            String newIntent = json.read("$.intent");
            List<Task> existingTasks = taskRepository.findByWorkflowInstanceKey(workflowInstanceKey);
            if (existingTasks != null && !existingTasks.isEmpty()) {
                if (existingTasks.stream().filter(existing -> {
                    // task intent inserts happens for only once
                    return newElementId.equals(existing.getElementId()) && newIntent.equals(existing.getIntent());
                }).findFirst().orElse(null) != null) {
                    logger.info("Task {} with intent {} already inserted at {} for instance {}, skip processing!",
                            newElementId,
                            newIntent,
                            newTimestamp,
                            workflowInstanceKey);
                    return;
                }
            }

            Task task = new Task();
            task.setWorkflowInstanceKey(workflowInstanceKey);
            task.setWorkflowKey(json.read("$.value.processDefinitionKey"));
            task.setTimestamp(newTimestamp);
            task.setIntent(newIntent);
            task.setRecordType(json.read("$.recordType"));
            task.setType(type);
            task.setElementId(newElementId);
            taskRepository.save(task);
        }
    }
}
