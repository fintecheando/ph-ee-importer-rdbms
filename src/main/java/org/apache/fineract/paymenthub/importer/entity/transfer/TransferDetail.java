package org.apache.fineract.paymenthub.importer.entity.transfer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

import org.apache.fineract.paymenthub.importer.entity.task.Task;
import org.apache.fineract.paymenthub.importer.entity.variable.Variable;


@Getter
@Setter
@RequiredArgsConstructor
public class TransferDetail {
    private Transfer transfer;
    private List<Task> tasks;
    private List<Variable> variables;

}
