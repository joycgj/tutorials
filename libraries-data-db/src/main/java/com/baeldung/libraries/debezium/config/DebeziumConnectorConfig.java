package com.baeldung.libraries.debezium.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class DebeziumConnectorConfig {

    /**
     * Database details.
     */
    @Value("${customer.datasource.host}")
    private String customerDbHost;

    @Value("${customer.datasource.database}")
    private String customerDbName;

    @Value("${customer.datasource.port}")
    private String customerDbPort;

    @Value("${customer.datasource.username}")
    private String customerDbUsername;

    @Value("${customer.datasource.password}")
    private String customerDbPassword;

    /**
     * Customer Database Connector Configuration
     */
    @Bean
    public io.debezium.config.Configuration customerConnector() throws IOException {
        String rootPath = System.getProperty("user.dir");

        File dir = new File(rootPath + "/metadata");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File offsetStorageFile = new File(rootPath + "/metadata/offsets.dat");
        File dbHistoryFile = new File(rootPath + "/metadata/dbhistory.dat");
        if (!offsetStorageFile.exists()) {
            try {
                offsetStorageFile.createNewFile();
                log.info("Create offsetStorageFile: {}", offsetStorageFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Error while creating offsetStorageFile: {}", offsetStorageFile.getAbsolutePath(), e);
            }
        }
        if (!dbHistoryFile.exists()) {
            try {
                dbHistoryFile.createNewFile();
                log.info("Create dbHistoryFile: {}", dbHistoryFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Error while creating dbHistoryFile: {}", dbHistoryFile.getAbsolutePath(), e);
            }
        }
//        File offsetStorageTempFile = File.createTempFile("offsets_", ".dat");
//        File dbHistoryTempFile = File.createTempFile("dbhistory_", ".dat");
        return io.debezium.config.Configuration.create()
            .with("name", "customer-mysql-connector")
            .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
            .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
            .with("offset.storage.file.filename", offsetStorageFile.getAbsolutePath())
            .with("offset.flush.interval.ms", "60000")
            .with("database.hostname", customerDbHost)
            .with("database.port", customerDbPort)
            .with("database.user", customerDbUsername)
            .with("database.password", customerDbPassword)
            .with("database.dbname", customerDbName)
            .with("database.include.list", customerDbName)
            .with("include.schema.changes", "false")
            .with("database.allowPublicKeyRetrieval", "true")
            .with("database.server.id", "10181")
            .with("database.server.name", "customer-mysql-db-server")
            .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
            .with("database.history.file.filename", dbHistoryFile.getAbsolutePath())
                .with("snapshot.mode", "schema_only") // Add by Joy
            .build();
    }
}
