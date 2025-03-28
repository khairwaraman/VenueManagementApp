package com.venue.mgmt.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venue.mgmt.repositories.LeadRegRepository;
import com.venue.mgmt.request.LoginRequest;
import com.venue.mgmt.request.VerifyUserOtpRequest;
import com.venue.mgmt.response.ValidateUserResponse;
import com.venue.mgmt.services.INotificationService;
import com.venue.mgmt.services.UserMgmtResService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Service
public class NotificationServiceImpl implements INotificationService {

    private static final Logger logger = LogManager.getLogger(NotificationServiceImpl.class);

    private final LeadRegRepository leadRegRepository;

    private final RestTemplate restTemplate;

    private final UserMgmtResService userMgmtResService;

    public NotificationServiceImpl(LeadRegRepository leadRegRepository, RestTemplate restTemplate, UserMgmtResService userMgmtResService) {
        this.leadRegRepository = leadRegRepository;
        this.restTemplate = restTemplate;
        this.userMgmtResService = userMgmtResService;
    }

    @Override
    public ValidateUserResponse sendOtpOnNumber(LoginRequest loginReq) throws JsonProcessingException,HttpClientErrorException {
        logger.info("NotificationServiceImpl - Inside sendOtpOnNumber method");

            // Call new ValidateUser API
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setMobileNumber(loginReq.getMobileNumber());
            loginRequest.setLoginUserType(loginReq.getLoginUserType());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, headers);

            // Debug log the request
            ObjectMapper mapper = new ObjectMapper();
            logger.info("Request body: {}", mapper.writeValueAsString(loginRequest));

             ResponseEntity<String> rawResponse = restTemplate.exchange(
                    "https://api-uat.wealth-right.com/api/ValidateUser",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Log raw response for debugging
            logger.info("Raw API Response: {}", rawResponse.getBody());

            ValidateUserResponse validateUserResponse = mapper.readValue(rawResponse.getBody(), ValidateUserResponse.class);
            
            // Check if the API returned an error

            return validateUserResponse;

    }

    @Override
    public ResponseEntity<String> validateOtp(VerifyUserOtpRequest verifyRequest) {
        logger.info("NotificationServiceImpl - Inside validateOtp method");

        try {
            // Set default OTP screen if not provided
            if (verifyRequest.getOtpScreen() == null) {
                verifyRequest.setOtpScreen("APP");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request body
            String requestBody = String.format(
                "{\"otp\":\"%s\",\"otpScreen\":\"%s\",\"refId\":\"%s\",\"userId\":\"%s\"}",
                verifyRequest.getOtp(),
                verifyRequest.getOtpScreen(),
                verifyRequest.getRefId(),
                verifyRequest.getUserId()
            );
            // Debug log the request
            logger.info("Request body: {}", requestBody);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Make API call and return raw response
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    "https://api-uat.wealth-right.com/api/verifyuserotp",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Parse the raw response body
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseBody = mapper.readValue(rawResponse.getBody(), Map.class);

            // Extract branchCode from userMaster
            Map<String, Object> userMaster = (Map<String, Object>) responseBody.get("response");
            Map<String, Object> userMasterDetails = (Map<String, Object>) userMaster.get("userMaster");

            String branchCode = (String) userMasterDetails.get("branchcode");

            // Get branch details from other schema
            Map<String, Object> branchDetails=null;
            if(branchCode!=null) {
                branchDetails = userMgmtResService.getDataFromOtherSchema(branchCode).get(0);
            }
            // Add branch details to the response
            userMaster.put("branchDetails", branchDetails);

            // Convert the modified response back to JSON
            String modifiedResponseBody = mapper.writeValueAsString(responseBody);

            //  Log raw response
            logger.info("Raw API Response: {}", rawResponse.getBody());

            // Return the raw response body with original status code
            return ResponseEntity.status(rawResponse.getStatusCode())
                               .contentType(MediaType.APPLICATION_JSON)
                               .body(modifiedResponseBody);

        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error while verifying OTP: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                               .contentType(MediaType.APPLICATION_JSON)
                               .body(e.getResponseBodyAsString());
        } catch (Exception exception) {
            logger.error("Exception while verifying OTP : ", exception);
            String errorJson = String.format(
                "{\"statusCode\":500,\"statusMsg\":\"Failed\",\"errorMsg\":\"%s\",\"response\":null}",
                exception.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .contentType(MediaType.APPLICATION_JSON)
                               .body(errorJson);
        }
    }
}