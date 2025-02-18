package com.baeldung.libraries.debezium.listener;

import com.baeldung.libraries.debezium.service.CustomerService;
import io.debezium.data.Envelope;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.List;
import java.util.Map;

import static io.debezium.data.Envelope.FieldName.AFTER;
import static io.debezium.data.Envelope.FieldName.BEFORE;
import static io.debezium.data.Envelope.FieldName.OPERATION;
import static java.util.stream.Collectors.toMap;

@Slf4j
public class MyChangeConsumer implements DebeziumEngine.ChangeConsumer<RecordChangeEvent<SourceRecord>> {
    private final CustomerService customerService;

    public MyChangeConsumer(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public void handleBatch(List<RecordChangeEvent<SourceRecord>> records, DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> committer) throws InterruptedException {
        for (RecordChangeEvent<SourceRecord> recordChangeEvent : records) {
            SourceRecord sourceRecord = recordChangeEvent.record();
            log.info("Key = '" + sourceRecord.key() + "' value = '" + sourceRecord.value() + "'");

            Struct sourceRecordChangeValue= (Struct) sourceRecord.value();

            if (sourceRecordChangeValue != null) {
                Envelope.Operation operation = Envelope.Operation.forCode((String) sourceRecordChangeValue.get(OPERATION));

                if(operation != Envelope.Operation.READ) {
                    String record = operation == Envelope.Operation.DELETE ? BEFORE : AFTER; // Handling Update & Insert operations.

                    Struct struct = (Struct) sourceRecordChangeValue.get(record);
                    Map<String, Object> payload = struct.schema().fields().stream()
                            .map(Field::name)
                            .filter(fieldName -> struct.get(fieldName) != null)
                            .map(fieldName -> Pair.of(fieldName, struct.get(fieldName)))
                            .collect(toMap(Pair::getKey, Pair::getValue));

                    this.customerService.replicateData(payload, operation);
                    log.info("Updated Data: {} with Operation: {}", payload, operation.name());
                }
            }
            committer.markProcessed(recordChangeEvent);
        }
        committer.markBatchFinished();
    }
}
