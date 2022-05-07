package org.apache.fineract.paymenthub.importer.entity.transactionrequest;

public enum TransactionRequestState {
    IN_PROGRESS,
    RECEIVED,
    ACCEPTED,
    REJECTED,
    FAILED;
}