package com.cloudnative.webapp.controller;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.cloudnative.webapp.service.ConnectionCheckerService;

@RestController
@RequestMapping("/healthz")
public class ConnectionCheckerController {
    ConnectionCheckerService connectioncheckerService;

    @Autowired
    public ConnectionCheckerController(ConnectionCheckerService connectioncheckerService) {
        super();
        this.connectioncheckerService = connectioncheckerService;
    }

    @GetMapping
    public ResponseEntity<String> checkDatabaseConnection(@RequestParam Map<String, String> requestParam, @RequestBody(required = false) String requestBody, HttpMethod httpMethod, @RequestHeader(value = "Authorization", required = false) String auth){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setCacheControl(CacheControl.noCache().mustRevalidate());
        httpHeaders.setPragma("no-cache");
        httpHeaders.add("X-Content-Type-Options", "nosniff");
        final Logger logger = LoggerFactory.getLogger(ConnectionCheckerController.class);
        try {
            if((requestParam != null && !requestParam.isEmpty()) || (requestBody != null && !requestBody.isEmpty()) || auth != null) {
                logger.warn("Request param or Body is present in GET Request");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .headers(httpHeaders)
                        .build();
            }
            connectioncheckerService.checkDatabaseConnection();
            logger.info("The request is successful!");
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .headers(httpHeaders)
                    .build();
        } catch(RuntimeException e) {
            logger.error("The service is unavailable");
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(httpHeaders)
                    .build();
        }
    }

    @RequestMapping(method = {RequestMethod.OPTIONS, RequestMethod.HEAD})
    public ResponseEntity<Void> handleCheckDatabaseHeadAndOptionsMethod() {
        final Logger logger = LoggerFactory.getLogger(ConnectionCheckerController.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setCacheControl(CacheControl.noCache().mustRevalidate());
        httpHeaders.setPragma("no-cache");
        httpHeaders.add("X-Content-Type-Options", "nosniff");
        logger.error("The method is not allowed.");
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(httpHeaders)
                .build();
    }
}