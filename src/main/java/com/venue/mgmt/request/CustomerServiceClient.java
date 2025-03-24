package com.venue.mgmt.request;

import com.venue.mgmt.dto.UserDetailsResponse;
import com.venue.mgmt.exception.CustomerNotSavedException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CustomerServiceClient {

    private final RestTemplate restTemplate;

    public CustomerServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> saveCustomerData(CustomerRequest customerRequest) {
        String url = "https://api.dev.wealth-right.com/Customer/api/CreateAndUpdateProspect";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<CustomerRequest> request = new HttpEntity<>(customerRequest, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CustomerNotSavedException("Failed to save customer data");
        }
        return response;
    }

    public UserDetailsResponse.UserDetails getUserDetails(String userId) {
        String url = "https://api.dev.wealth-right.com/Usermgt/api/GetUserDetails/" + userId + "/USERID";
        UserDetailsResponse response = restTemplate.getForObject(url, UserDetailsResponse.class);
        return response != null ? response.getResponse() : null;
    }
}