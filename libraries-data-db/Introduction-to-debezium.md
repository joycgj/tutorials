# Introduction to Debezium

## 1. Introduction

Today's applications sometimes need a replica database, a search index to perform a search operation, a cache store to speed up data read, and a data warehouse for complex analytics on data.

The need to support different data models and data access patterns presents a common problem that most software web developers need to solve, and that's when Change Data Capture (CDC) comes to the rescue!

In this article, we'll start with a brief overview of CDC, and we'll focus on **Debezium, a platform commonly used for CDC**.

## 2. What Is a CDC?

In this section, we'll see what a CDC is, the key benefits of using it, and some common use cases.

### 2.1. Change Data Capture

Change Data Capture (CDC) is a technique and a design pattern. We often use it to replicate data between databases in real-time.

We can also track data changes written to a source database and automatically sync target databases. **CDC enables incremental loading and eliminates the need for bulk load updating**.

### 2.2. Advantages of CDC

Most companies today still use batch processing to sync data between their systems. Using batch processing:

- Data is not synced immediately
- More allocated resources are used for syncing databases
- Data replication only happens during specified batch periods

However, change data capture offers some advantages:

- Constantly tracks changes in the source database
- Instantly updates the target database
- Uses stream processing to guarantee instant changes

With CDC, the **different databases are continuously synced**, and bulk selecting is a thing of the past. Moreover, **the cost of transferring data is reduced** because CDC transfers only incremental changes.

### 2.3. Common CDC Use Cases

There are various use cases that CDC can help us solve, such as data replication by keeping different data sources in sync, updating or invalidating a cache, updating search indexes, data synchronization in microservices, and much more.

Now that we know a little bit about what a CDC can do, let's see how it's implemented in one of the well-known open-source tools.

## 3. Debezium Platform

In this section, we’ll introduce [Debezium](https://debezium.io/), discover its architecture in detail, and see the different ways of deploying it.

### 3.1. What Is Debezium?

Debezium is an open-source platform for CDC built on top of [Apache Kafka](https://kafka.apache.org/). Its primary use is to **record all row-level changes committed to each source database** table in a transaction log. Each application listening to these events can perform needed actions based on incremental data changes.

Debezium provides a library of connectors, supporting multiple databases like MySQL, MongoDB, PostgreSQL, and others.

These connectors can monitor and record the database changes and publish them to a streaming service like Kafka.

Moreover, **Debezium monitors even if our applications are down**. Upon restart, it will start consuming the events where it left off, so it misses nothing.

### 3.2. Debezium Architecture

Deploying Debezium depends on the infrastructure we have, but more commonly, we often use Apache Kafka Connect.

Kafka Connect is a framework that operates as a separate service alongside the Kafka broker. We used it for streaming data between Apache Kafka and other systems.

We can also define connectors to transfer data into and out of Kafka.

The diagram shown below shows the different parts of a change data capture pipeline based on Debezium:

![](https://www.baeldung.com/wp-content/uploads/2021/04/simple-app-debezium-embedded-arch-1.png)

First, on the left, we have a MySQL source database whose data we want to copy and use in a target database like PostgreSQL or any analytics database.

Second, the [Kafka Connect connector](https://www.baeldung.com/kafka-connectors-guide) parses and interprets the transaction log and writes it to a Kafka topic.

Next, Kafka acts as a message broker to reliably transfer the changeset to the target systems.

Then, on the right, we have Kafka connectors polling Kafka and pushing the changes to the target databases.

**Debezium utilizes Kafka in its architecture**, but it also offers other deployment methods to satisfy our infrastructure needs.

We can use it as a standalone server with the Debezium server, or we can embed it into our application code as a library.

We'll see those methods in the following sections.

### 3.3. Debezium Server

**Debezium provides a standalone server** to capture the source database changes. It's configured to use one of the Debezium source connectors.

Moreover, these connectors send change events to various messaging infrastructures like Amazon Kinesis or Google Cloud Pub/Sub.

The following image shows the architecture of a change data capture pipeline that uses the Debezium server:

![](https://debezium.io/documentation/reference/stable/_images/debezium-server-architecture.png)

### 3.4. Embedded Debezium

Kafka Connect offers fault tolerance and scalability when used to deploy Debezium. However, sometimes our applications don't need that level of reliability, and we want to minimize the cost of our infrastructure.

Thankfully, **we can do this by embedding the Debezium engine within our application**. After doing this, we must configure the connectors.

## 4. Setup

In this section, we’ll start first with the architecture of our application. Then, we’ll see how to set up our environment and follow some basic steps to integrate Debezium.

Let’s start by introducing our application.

### 4.1. Sample Application’s Architecture

To keep our application simple, we'll create a Spring Boot application for customer management.

Our customer model has _ID_, _fullname_, and _email_ fields. For the data access layer, we'll use [Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa).

Above all, our application will run the embedded version of Debezium. Let's visualize this application architecture:

![](https://www.baeldung.com/wp-content/uploads/2021/04/simple-app-debezium-embedded-arch-1.png)

First, the Debezium Engine will track a _customer_ table's transaction logs on a source MySQL database (from another system or application).

Second, whenever we perform a database operation like Insert/Update/Delete on the _customer_ table, the Debezium connector will call a service method.

Finally, based on these events, that method will sync the _customer_ table's data to a target MySQL database (our application’s primary database).

### 4.2. Maven Dependencies

Let's get started by first adding the [required dependencies](https://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22io.debezium%22%20AND%20(a%3A%22debezium-api%22%20OR%20a%3A%22debezium-embedded%22)) to our pom.xml:

```
<dependency>
    <groupId>io.debezium</groupId>
    <artifactId>debezium-api</artifactId>
    <version>1.4.2.Final</version>
</dependency>
<dependency>
    <groupId>io.debezium</groupId>
    <artifactId>debezium-embedded</artifactId>
    <version>1.4.2.Final</version>
</dependency>
```

Likewise, we add dependencies for each of the Debezium connectors that our application will use.

In our case, we’ll use the [MySQL connector](https://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22io.debezium%22%20%20AND%20a%3A%22debezium-connector-mysql%22):

```
<dependency>
    <groupId>io.debezium</groupId>
    <artifactId>debezium-connector-mysql</artifactId>
    <version>1.4.2.Final</version>
</dependency>
```

### 4.3. Installing Databases

We can install and configure our databases manually. However, to speed things up, we’ll use a _docker-compose_ file:

```
version: "3.9"
services:
  # Install Source MySQL DB and setup the Customer database
  mysql-1:
    container_name: source-database
    image: mysql
    ports:
      - 3305:3306
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_USER: user
      MYSQL_PASSWORD: password
      MYSQL_DATABASE: customerdb

  # Install Target MySQL DB and setup the Customer database
  mysql-2:
    container_name: target-database
    image: mysql
    ports:
      - 3306:3306
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_USER: user
      MYSQL_PASSWORD: password
      MYSQL_DATABASE: customerdb
```

This file will run two database instances on different ports.

We can run this file using the command _docker-compose up -d_.

Now, let’s create the customer table by running a SQL script:

```
CREATE TABLE customer
(
    id integer NOT NULL,
    fullname character varying(255),
    email character varying(255),
    CONSTRAINT customer_pkey PRIMARY KEY (id)
);
```

## 5. Configuration

In this section, we'll configure the Debezium MySQL Connector and see how to run the Embedded Debezium Engine.

### 5.1. Configuring the Debezium Connector

To configure our Debezium MySQL Connector, we’ll create a Debezium configuration bean:

```
@Bean
public io.debezium.config.Configuration customerConnector() {
    return io.debezium.config.Configuration.create()
        .with("name", "customer-mysql-connector")
        .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
        .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
        .with("offset.storage.file.filename", "/tmp/offsets.dat")
        .with("offset.flush.interval.ms", "60000")
        .with("database.hostname", customerDbHost)
        .with("database.port", customerDbPort)
        .with("database.user", customerDbUsername)
        .with("database.password", customerDbPassword)
        .with("database.dbname", customerDbName)
        .with("database.include.list", customerDbName)
        .with("include.schema.changes", "false")
        .with("database.server.id", "10181")
        .with("database.server.name", "customer-mysql-db-server")
        .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
        .with("database.history.file.filename", "/tmp/dbhistory.dat")
        .build();
}
```

Let's examine this configuration in more detail.

The create method within **this bean uses a builder to create a Properties object**.

This builder sets several [properties required by the engine](https://debezium.io/documentation/reference/1.4/development/engine.html#engine-properties) regardless of the preferred connector. To track the source MySQL database, we use the class _MySqlConnector_.

When this connector runs, it starts tracking changes from the source and records “offsets” to determine **how much data it has processed from the transaction log**.

There are several ways to save these offsets, but in this example, we'll use the class _FileOffsetBackingStore_ to store offsets on our local filesystem.

The last few parameters of the connector are the MySQL database properties.

Now that we have a configuration, we can create our engine.

### 5.2. Running the Debezium Engine

The _DebeziumEngine_ serves as a wrapper around our MySQL connector. Let’s create the engine using the connector configuration:

```
private DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine;

public DebeziumListener(Configuration customerConnectorConfiguration, CustomerService customerService) {

    this.debeziumEngine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
      .using(customerConnectorConfiguration.asProperties())
      .notifying(this::handleEvent)
      .build();

    this.customerService = customerService;
}
```

More to this, the engine will call a method for every data change – in our example, the _handleChangeEvent_.

In this method, first, **we’ll parse every event based on the format specified** when calling _create()_.

Then, we find which operation we had and invoke the _CustomerService_ to perform Create/Update/Delete functions on our target database:

```
private void handleChangeEvent(RecordChangeEvent<SourceRecord> sourceRecordRecordChangeEvent) {
    SourceRecord sourceRecord = sourceRecordRecordChangeEvent.record();
    Struct sourceRecordChangeValue= (Struct) sourceRecord.value();

    if (sourceRecordChangeValue != null) {
        Operation operation = Operation.forCode((String) sourceRecordChangeValue.get(OPERATION));

        if(operation != Operation.READ) {
            String record = operation == Operation.DELETE ? BEFORE : AFTER;
            Struct struct = (Struct) sourceRecordChangeValue.get(record);
            Map<String, Object> payload = struct.schema().fields().stream()
              .map(Field::name)
              .filter(fieldName -> struct.get(fieldName) != null)
              .map(fieldName -> Pair.of(fieldName, struct.get(fieldName)))
              .collect(toMap(Pair::getKey, Pair::getValue));

            this.customerService.replicateData(payload, operation);
        }
    }
}
```

Now that we have configured a _DebeziumEngine_ object, let’s start it asynchronously using the service executor:

```
private final Executor executor = Executors.newSingleThreadExecutor();

@PostConstruct
private void start() {
    this.executor.execute(debeziumEngine);
}

@PreDestroy
private void stop() throws IOException {
    if (this.debeziumEngine != null) {
        this.debeziumEngine.close();
    }
}
```

# 6. Debezium in Action

To see our code in action, let's make some data changes on the source database's _customer_ table.

```
INSERT INTO customerdb.customer (id, fullname, email) VALUES (1, 'John Doe', 'jd@example.com')
```

After running this query, we’ll see the corresponding output from our application:

```
23:57:57.897 [pool-1-thread-1] INFO  c.b.l.d.listener.DebeziumListener - Key = 'Struct{id=1}' value = 'Struct{after=Struct{id=1,fullname=John Doe,email=jd@example.com},source=Struct{version=1.4.2.Final,connector=mysql,name=customer-mysql-db-server,ts_ms=1617746277000,db=customerdb,table=customer,server_id=1,file=binlog.000007,pos=703,row=0,thread=19},op=c,ts_ms=1617746277422}'
Hibernate: insert into customer (email, fullname, id) values (?, ?, ?)
23:57:58.095 [pool-1-thread-1] INFO  c.b.l.d.listener.DebeziumListener - Updated Data: {fullname=John Doe, id=1, email=jd@example.com} with Operation: CREATE
```

Finally, we check that a new record was inserted into our target database:

```
id  fullname   email
1  John Doe   jd@example.com
```

# 6.2. Updating a Record

Now, let's try to update our last inserted customer and check what happens:

```
UPDATE customerdb.customer t SET t.email = 'john.doe@example.com' WHERE t.id = 1
```

After that, we'll get the same output as we got with insert, except the operation type changes to ‘UPDATE', and of course, the query that Hibernate uses is an ‘update' query:

```
00:08:57.893 [pool-1-thread-1] INFO  c.b.l.d.listener.DebeziumListener - Key = 'Struct{id=1}' value = 'Struct{before=Struct{id=1,fullname=John Doe,email=jd@example.com},after=Struct{id=1,fullname=John Doe,email=john.doe@example.com},source=Struct{version=1.4.2.Final,connector=mysql,name=customer-mysql-db-server,ts_ms=1617746937000,db=customerdb,table=customer,server_id=1,file=binlog.000007,pos=1040,row=0,thread=19},op=u,ts_ms=1617746937703}'
Hibernate: update customer set email=?, fullname=? where id=?
00:08:57.938 [pool-1-thread-1] INFO  c.b.l.d.listener.DebeziumListener - Updated Data: {fullname=John Doe, id=1, email=john.doe@example.com} with Operation: UPDATE
```

We can verify that John's email has been changed in our target database:

```
id  fullname   email
1  John Doe   john.doe@example.com
```

# 6.3. Deleting a Record

Now, we can delete an entry in the customer table by executing:

DELETE FROM customerdb.customer WHERE id = 1

Likewise, here we have a change in operation and query again:

```
00:12:16.892 [pool-1-thread-1] INFO  c.b.l.d.listener.DebeziumListener - Key = 'Struct{id=1}' value = 'Struct{before=Struct{id=1,fullname=John Doe,email=john.doe@example.com},source=Struct{version=1.4.2.Final,connector=mysql,name=customer-mysql-db-server,ts_ms=1617747136000,db=customerdb,table=customer,server_id=1,file=binlog.000007,pos=1406,row=0,thread=19},op=d,ts_ms=1617747136640}'
Hibernate: delete from customer where id=?
00:12:16.951 [pool-1-thread-1] INFO  c.b.l.d.listener.DebeziumListener - Updated Data: {fullname=John Doe, id=1, email=john.doe@example.com} with Operation: DELETE
```

We can verify that the data has been deleted on our target database:

```
select * from customerdb.customer where id= 1
0 rows retrieved
```

# 7. Conclusion

In this article, we saw the benefits of CDC and what problems it can solve. We also learned that, without it, we're left with bulk loading of the data, which is both time-consuming and costly.

We also saw Debezium, an excellent open-source platform that can help us solve CDC use cases with ease.

As always, the full source code of the article is available [over on GitHub](https://github.com/eugenp/tutorials/tree/master/libraries-data-db).