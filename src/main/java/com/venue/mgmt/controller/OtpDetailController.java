package com.venue.mgmt.controller;

import com.venue.mgmt.constant.GeneralMsgConstants;
import com.venue.mgmt.request.ValidateOtpRequest;
import com.venue.mgmt.response.VerifyUserOtpResponse;
import com.venue.mgmt.services.OTPService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/venue-app/v1/leads")
public class OtpDetailController {
    private static final Logger logger = LogManager.getLogger(OtpDetailController.class);

    private final OTPService otpService;
    private final HttpServletRequest request;


    public OtpDetailController(OTPService otpService, HttpServletRequest request) {
        this.otpService = otpService;
        this.request = request;
    }

    @PostMapping("/sendOtp")
    public ResponseEntity<VerifyUserOtpResponse> sendOtp(
            @RequestBody @Valid ValidateOtpRequest validateOtpRequest) {
        logger.info("VenueManagementApp - Inside send otp method with lead Id : {}", validateOtpRequest.getLeadId());
            String userId = (String) request.getAttribute(GeneralMsgConstants.USER_ID);
            String messageSent = otpService.generateAndSendOTP(validateOtpRequest, userId);
            VerifyUserOtpResponse verifyUserOtpResponse;
            verifyUserOtpResponse = new VerifyUserOtpResponse();
            verifyUserOtpResponse.setStatusCode(200);
            verifyUserOtpResponse.setStatusMsg(GeneralMsgConstants.OTP_SENT_SUCCESS);
            verifyUserOtpResponse.setErrorMsg(null);
            verifyUserOtpResponse.setResponse(!messageSent.isEmpty());
            return ResponseEntity.ok(verifyUserOtpResponse);
        }

    @PostMapping("/validateOtp")
    public ResponseEntity<VerifyUserOtpResponse> validateOtp(
            @RequestBody @Valid ValidateOtpRequest validateOtpRequest) {
        logger.info("VenueManagementApp - Inside validate otp method");
        VerifyUserOtpResponse verifyUserOtpResponse = new VerifyUserOtpResponse();
            boolean otpVerifiedSuccessfully = otpService.validateOtp(validateOtpRequest);
            if(otpVerifiedSuccessfully) {
                verifyUserOtpResponse.setStatusCode(200);
                verifyUserOtpResponse.setStatusMsg(GeneralMsgConstants.OTP_VERIFIED_SUCCESS);
                verifyUserOtpResponse.setErrorMsg(null);
                verifyUserOtpResponse.setResponse(true);
            }else{
                verifyUserOtpResponse.setStatusCode(400);
                verifyUserOtpResponse.setStatusMsg("Failed");
                verifyUserOtpResponse.setErrorMsg("OTP verification failed. Please try again.");
                verifyUserOtpResponse.setResponse(false);
            }
            return ResponseEntity.ok(verifyUserOtpResponse);
    }


}
