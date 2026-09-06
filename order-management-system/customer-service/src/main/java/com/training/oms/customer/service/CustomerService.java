package com.training.oms.customer.service;

import com.training.oms.customer.domain.Customer;
import com.training.oms.customer.dto.CustomerRequest;
import com.training.oms.customer.dto.CustomerResponse;
import com.training.oms.customer.exception.CustomerNotFoundException;
import com.training.oms.customer.exception.DuplicateEmailException;
import com.training.oms.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse create(CustomerRequest request) {
        customerRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new DuplicateEmailException(request.email());
        });
        Customer saved = customerRepository.save(
                new Customer(request.name(), request.email(), request.phone(), request.address()));
        return CustomerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(CustomerResponse::from).toList();
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOrThrow(id);
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        return CustomerResponse.from(customer);
    }

    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteById(id);
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
