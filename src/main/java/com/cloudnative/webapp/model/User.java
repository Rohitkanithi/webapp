package com.cloudnative.webapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(name = "password", nullable = false)
    private String password;

    @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}")
    @Column(name = "username", nullable = false, unique = true, updatable = false)
    private String username;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "account_created", nullable = false, updatable = false)
    private Date accountCreated;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "account_updated", nullable = false)
    private Date accountUpdated;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "email_verify_expiry_time")
    private Date emailVerifyExpiryTime;

    @Column(name = "token")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String token;
}