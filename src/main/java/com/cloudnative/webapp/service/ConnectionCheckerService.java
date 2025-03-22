package com.cloudnative.webapp.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectionCheckerService {
    private final DataSource dataSource;

    @Autowired
    public ConnectionCheckerService(DataSource dataSource) {
        super();
        this.dataSource = dataSource;
    }

    public void checkDatabaseConnection() {
        final Logger logger = LoggerFactory.getLogger(ConnectionCheckerService.class);
        try(Connection connection = dataSource.getConnection()) {
            logger.debug("Health Check Connection Successful!!");
            System.out.println("Health Check Connection Successful!!");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Health Check Connection Failed...");
            throw new RuntimeException(e);
        }
    }

}