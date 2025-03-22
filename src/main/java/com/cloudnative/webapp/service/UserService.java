package com.cloudnative.webapp.service;

import com.cloudnative.webapp.model.User;
import com.cloudnative.webapp.model.UserDTO;
import com.cloudnative.webapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Environment environment;
    //    private static final String GOOGLE_PROJECT_ID = System.getenv("GOOGLE_PROJECT_ID");
//    private static final String PUB_SUB_TOPIC_ID = System.getenv("PUB_SUB_TOPIC_ID");
    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    @Autowired
    public UserService(UserRepository userRepository, Environment environment) {

        this.userRepository = userRepository;
        this.environment = environment;
    }

    public User createUser(User user) throws IOException, ExecutionException, InterruptedException, SQLException {
        String pass = passwordEncoderBCrypt(user.getPassword());
        user.setPassword(pass);
        user.setToken(UUID.randomUUID().toString());
        User createdUser =  userRepository.save(user);
        publisherExample(createdUser.getUsername()+":"+createdUser.getToken());
        return createdUser;
    }
    public String[] decodeBase64String(String s){
        byte[] decodedBase64Bytes = Base64.getDecoder().decode(s);
        String decodedBase64String = new String(decodedBase64Bytes, StandardCharsets.UTF_8);
        return decodedBase64String.split(":");
    }

    public String[] getSubStrings(String s){
        String base64SubStrings = s.substring("Basic ".length());
        String[] subStrings = decodeBase64String(base64SubStrings);
        return subStrings;
    }

    public boolean checkIsValidUser(String s){
        String[] subStrings = getSubStrings(s);
        String userName;
        String password;
        if(subStrings.length == 2){
            userName = subStrings[0];
            password = subStrings[1];
            User loggedInUser = userRepository.findByUsername(userName);
            return loggedInUser != null && bCryptPasswordEncoder.matches(password, loggedInUser.getPassword());
        }
        return false;
    }
    public String getUserNameFromAuth(String auth){
        String[] subStrings = getSubStrings(auth);
        return subStrings[0];
    }

    public User getUser(String auth) {
        String userName = getUserNameFromAuth(auth);
        return userRepository.findByUsername(userName);
    }

    public User getUserFromUserName(String userName) {
        return userRepository.findByUsername(userName);
    }

    public String passwordEncoderBCrypt(String pass){
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.encode(pass);
    }
    public UserDTO userToUserDTOMapper(User user){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setAccountCreated(user.getAccountCreated());
        userDTO.setAccountUpdated(user.getAccountUpdated());
        return userDTO;
    }
    public UserDTO updateUser(String auth, User requestBodyUser){
        User user = getUser(auth);
        if(null != requestBodyUser.getFirstName() && !requestBodyUser.getFirstName().isBlank()){
            user.setFirstName(requestBodyUser.getFirstName());
        }
        if(null != requestBodyUser.getLastName() && !requestBodyUser.getLastName().isBlank()){
            user.setLastName(requestBodyUser.getLastName());
        }
        if(null != requestBodyUser.getPassword() && !requestBodyUser.getPassword().isBlank()){
            user.setPassword(passwordEncoderBCrypt(requestBodyUser.getPassword()));
        }
        User savedUser = userRepository.save(user);
        return userToUserDTOMapper(savedUser);

    }

    public boolean checkIfValidRequestBody(User requestBodyUser){
        String userName = requestBodyUser.getUsername();
        String password = requestBodyUser.getPassword();
        String firstName = requestBodyUser.getFirstName();
        String lastName = requestBodyUser.getLastName();
        final Logger logger = LoggerFactory.getLogger(UserService.class);
        if(userName.isBlank() || password.isBlank() || firstName.isBlank() || lastName.isBlank()){
            logger.warn("Password, First Name, Last Name must not be blank.");
            return false;
        }
        if(requestBodyUser.getAccountCreated() != null || requestBodyUser.getAccountUpdated() != null || requestBodyUser.getEmailVerifyExpiryTime() != null
                || requestBodyUser.isVerified()) {
            return false;
        }
        return true;
    }

    public boolean containsNecessaryFields(User requestBodyUser){
        String password = requestBodyUser.getPassword();
        String firstName = requestBodyUser.getFirstName();
        String lastName = requestBodyUser.getLastName();
        final Logger logger = LoggerFactory.getLogger(UserService.class);
        if(password == null && firstName == null && lastName == null){
            logger.warn("Password, First Name, Last Name must not be null.");
            return false;
        }
        else if(password != null && password.isBlank() || firstName != null && firstName.isBlank() || lastName != null && lastName.isBlank()){
            logger.warn("Password, First Name, Last Name must not be blank.");
            return false;
        }
        return true;
    }

// public class PublisherExample {
//   public static void main(String... args) throws Exception {
    // TODO(developer): Replace these variables before running the sample.
    // String projectId = "csye6225-dev-123";
    // String topicId = "verify-email";

    // publisherExample(projectId, topicId);
//   }
    public String verifyUser(Map<String, String> queryParameter, boolean isIntegrationTestCheck) {
        final Logger logger = LoggerFactory.getLogger(UserService.class);
        if (queryParameter.containsKey("username") && queryParameter.containsKey("token")) {
            String username = queryParameter.get("username");
            String token = queryParameter.get("token");
            User requestedUser = userRepository.findByUsername(username);
            if(isIntegrationTestCheck){
                requestedUser.setVerified(true);
                userRepository.save(requestedUser);
                return "User is verified as part of integration test";
            }
            if(requestedUser.isVerified()){
                logger.info("User already verified:"+username);
                return "User already verified";
            }
            else if(token.equals(requestedUser.getToken())){
                Instant instantVerificationTime = requestedUser.getEmailVerifyExpiryTime().toInstant();
                logger.info("instant(now) time: "+Instant.now()+"for user:"+requestedUser.getUsername());
                logger.info("database time: "+instantVerificationTime);
                Duration duration = Duration.between(instantVerificationTime, Instant.now());
                if(duration.toSeconds() < 0) {
                    requestedUser.setVerified(true);
                    userRepository.save(requestedUser);
                    return "User Email is Verified";
                } else{
                    return "Sorry, link has expired";
                }
            } else{
                requestedUser.setVerified(false);
                userRepository.save(requestedUser);
                logger.error("The given token is incorrect. Current given token: "+token+", Actual token: "+requestedUser.getId()+"for user: "+username);
                return "User is not Verified";
            }
        } else {
            logger.error("Username/Token is missing");
            return "User is not Verified";
        }
    }
    public void publisherExample(String token)
            throws IOException, ExecutionException, InterruptedException {
        String projectId = environment.getProperty("GOOGLE_PROJECT_ID");
        String topicId = environment.getProperty("PUB_SUB_TOPIC_ID");
//    TopicName topicName = TopicName.of(GOOGLE_PROJECT_ID, PUB_SUB_TOPIC_ID);
        TopicName topicName = TopicName.of(projectId, topicId);
        final Logger logger = LoggerFactory.getLogger(UserService.class);

        Publisher publisher = null;
        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build();

            ByteString data = ByteString.copyFromUtf8(token);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get();
            System.out.println("Published message ID: " + messageId);
        } catch(Exception e){
            logger.error("Error thrown while publishing:" + e);
        }
        finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }
}