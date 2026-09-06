package com.training.oms.customer.service;

import com.training.oms.customer.domain.Customer;
import com.training.oms.customer.dto.CustomerRequest;
import com.training.oms.customer.dto.CustomerResponse;
import com.training.oms.customer.exception.CustomerNotFoundException;
import com.training.oms.customer.exception.DuplicateEmailException;
import com.training.oms.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Beginner lesson 05: unit-testing the service layer in isolation with
 * JUnit 5 + Mockito, without starting a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequest request;

    @BeforeEach
    void setUp() {
        request = new CustomerRequest("Alice Johnson", "alice@example.com", "555-0100", "12 Baker Street");
    }

    @Test
    void create_savesNewCustomer_whenEmailNotTaken() {
        when(customerRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.create(request);

        assertThat(response.name()).isEqualTo("Alice Johnson");
        assertThat(response.email()).isEqualTo("alice@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void create_throwsDuplicateEmailException_whenEmailAlreadyExists() {
        when(customerRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(new Customer("Existing", request.email(), null, null)));

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFoundException_whenCustomerMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }
}
