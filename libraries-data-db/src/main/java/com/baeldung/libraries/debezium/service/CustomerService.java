package com.baeldung.libraries.debezium.service;

import com.baeldung.libraries.debezium.entity.Customer;
import com.baeldung.libraries.debezium.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.data.Envelope.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public void replicateData(Map<String, Object> customerData, Operation operation) {

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final Customer customer = mapper.convertValue(customerData, Customer.class);

            if (Operation.DELETE == operation) {
                customerRepository.deleteById(customer.getId());
            } else {
                customerRepository.save(customer);
            }
        } catch (IllegalArgumentException e) {
            log.error("error while replicateData", e);
        }
    }
}
