package org.apache.fineract.paymenthub.importer.importer;

import org.apache.fineract.paymenthub.importer.entity.transactionrequest.TransactionRequest;
import org.apache.fineract.paymenthub.importer.entity.transactionrequest.TransactionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class InflightTransactionRequestManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<Long, TransactionRequest> inflightTransactionRequests = new HashMap<>();

    @Autowired
    private TransactionRequestRepository transactionRequestRepository;

    public void transactionRequestStarted(Long workflowInstanceKey, Long timestamp, String direction) {
        TransactionRequest transactionRequest = getOrCreateTransactionRequest(workflowInstanceKey);
        if (transactionRequest.getStartedAt() == null) {
            transactionRequest.setDirection(direction);
            transactionRequest.setStartedAt(new Date(timestamp));
            transactionRequestRepository.save(transactionRequest);
            logger.debug("started in-flight {} transactionRequest {}", transactionRequest.getDirection(), transactionRequest.getWorkflowInstanceKey());
        } else {
            logger.debug("transactionRequest {} already started at {}", workflowInstanceKey, transactionRequest.getStartedAt());
        }
    }

    public void transactionRequestEnded(Long workflowInstanceKey, Long timestamp) {
        synchronized (inflightTransactionRequests) {
            TransactionRequest transactionRequest = inflightTransactionRequests.remove(workflowInstanceKey);
            if (transactionRequest == null) {
                logger.error("failed to remove in-flight transactionRequest {}", workflowInstanceKey);
                transactionRequest = transactionRequestRepository.findByWorkflowInstanceKey(workflowInstanceKey);
                if (transactionRequest == null || transactionRequest.getCompletedAt() != null) {
                    logger.error("completed event arrived for non existent transactionRequest {} or it was already finished!", workflowInstanceKey);
                    return;
                }
            }

            transactionRequest.setCompletedAt(new Date(timestamp));

            transactionRequestRepository.save(transactionRequest);
            logger.debug("transactionRequest {} finished", transactionRequest.getWorkflowInstanceKey());
        }
    }

    public TransactionRequest getOrCreateTransactionRequest(Long workflowInstanceKey) {
        synchronized (inflightTransactionRequests) {
            TransactionRequest transactionRequest = inflightTransactionRequests.get(workflowInstanceKey);
            if (transactionRequest == null) {
                transactionRequest = transactionRequestRepository.findByWorkflowInstanceKey(workflowInstanceKey);
                if (transactionRequest == null) {
                    transactionRequest = new TransactionRequest(workflowInstanceKey);
                }
                inflightTransactionRequests.put(workflowInstanceKey, transactionRequest);
            }
            return transactionRequest;
        }
    }
}
