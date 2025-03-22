package com.cloudnative.webapp.controller;

import com.cloudnative.webapp.model.User;
import com.cloudnative.webapp.model.UserDTO;
import com.cloudnative.webapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("v9/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping()
    public ResponseEntity<Object> createUser(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody User requestBody) throws IOException, ExecutionException, InterruptedException, SQLException {
        final Logger logger = LoggerFactory.getLogger(UserController.class);
        if(auth == null && (requestBody.getUsername() != null && userService.getUserFromUserName(requestBody.getUsername()) == null) && userService.checkIfValidRequestBody(requestBody)) {
            User createdUser = userService.createUser(requestBody);
            UserDTO createdUserDTO = userService.userToUserDTOMapper(createdUser);
            logger.info("The User Post request is successful!");
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .body(createdUserDTO);
        }
        else {
            logger.error("The User Post request is unsuccessful. Check if you've included incorrect request body or an existing username.");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }
    }

    @GetMapping("/self")
    public ResponseEntity<Object> getUser(@RequestHeader("Authorization") String auth, @RequestBody(required = false) String requestBody) {
        final Logger logger = LoggerFactory.getLogger(UserController.class);
        User loggedInUser = userService.getUser(auth);
        UserDTO loggedInUserDTO = userService.userToUserDTOMapper(loggedInUser);
        if(userService.checkIsValidUser(auth) && loggedInUser.isVerified()) {
            if (requestBody == null) {
                logger.debug("User Details to be returned:" + loggedInUserDTO);
                logger.info("Get Request for User - successful!");
                return ResponseEntity
                        .ok()
                        .cacheControl(CacheControl.noCache().mustRevalidate())
                        .body(loggedInUserDTO);
            } else {
                logger.warn("Request Body shouldn't be provided");
                logger.error("Request Failed as body is provided");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .cacheControl(CacheControl.noCache().mustRevalidate())
                        .build();
            }
        }
        else if(!loggedInUser.isVerified()){
            logger.error("Request failed due to Forbidden User.");
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }
        else {
            logger.error("Request failed due to Unauthorized User.");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }
    }

    @PutMapping("/self")
    public ResponseEntity<Object> updateUser(@RequestHeader("Authorization") String auth, @RequestBody User requestBody){
        final Logger logger = LoggerFactory.getLogger(UserController.class);
        User loggedInUser = userService.getUser(auth);
        if(userService.checkIsValidUser(auth)  && loggedInUser.isVerified()){
            if(userService.containsNecessaryFields(requestBody) && requestBody.getUsername() == null && requestBody.getAccountCreated() == null
                    && requestBody.getAccountUpdated() == null && requestBody.getEmailVerifyExpiryTime() == null && !requestBody.isVerified()){
                userService.updateUser(auth, requestBody);
                logger.info("Put Request for User - successful!");
                return ResponseEntity
                        .status(HttpStatus.NO_CONTENT)
                        .cacheControl(CacheControl.noCache().mustRevalidate())
                        .build();
            }
            else{
                logger.warn("Fields like username, created date, updated date, isVerified, EmailVerifySentTime cannot be updated.");
                logger.error("Request Failed as the request body is incorrect.");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .cacheControl(CacheControl.noCache().mustRevalidate())
                        .build();
            }
        }
        else if(!loggedInUser.isVerified()){
            logger.error("Request failed due to Forbidden User.");
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }
        else {
            logger.error("Request failed due to Unauthorized User.");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }
    }
    @GetMapping("/verify-email")
    public ResponseEntity<Object> verifyNewUser(@RequestParam Map<String, String> queryParameter, @RequestBody(required = false) String payload, @RequestHeader(required = false, value = "isIntegrationTestCheck") boolean isIntegrationTestCheck) {
        final Logger logger = LoggerFactory.getLogger(UserController.class);
        logger.debug("Verify Email: "+ payload);
        if (null != payload && !payload.isEmpty()) {
            logger.error("Payload should not be given");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }
        String message = userService.verifyUser(queryParameter, isIntegrationTestCheck);
        return ResponseEntity
                .status(HttpStatus.OK)
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(Collections.singletonMap("UserEmailVerificationStatus", message));
    }
}