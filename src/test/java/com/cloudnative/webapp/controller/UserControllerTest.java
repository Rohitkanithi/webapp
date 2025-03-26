package com.cloudnative.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudnative.webapp.model.User;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import java.util.Base64;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp(){
        RestAssured.port = port;
        RestAssured.basePath = "v9/user";
    }

    private String userToJsonString(User user){
        try{
            return new ObjectMapper().writeValueAsString(user);
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private String encodeBase64String(String username, String password){
        String auth = username + ":" + password;
        byte[] encodedBase64Bytes = Base64.getEncoder().encode(auth.getBytes());
        return new String(encodedBase64Bytes);
    }
    @Test
    public void createUser() {
        User user = new User();
        user.setUsername("username@gmail.com");
        user.setFirstName("Brooke");
        user.setLastName("Kuttan");
        user.setPassword("password");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Basic " + encodeBase64String("username@gmail.com", "password"));

        ValidatableResponse validatableResponse = given()
                .contentType(ContentType.JSON)
                .body(userToJsonString(user))
                .when()
                .log().all()
                .post()
                .then().log().all().statusCode(201);

        String token = validatableResponse.extract().path("id");

        given()
                .param("username", "username@gmail.com")
                .param("token", token)
                .headers("isIntegrationTestCheck", true)
                .log().all()
                .when()
                .get("/verify-email")
                .then()
                .log().all().assertThat().statusCode(200);

        given()
                .headers(httpHeaders)
                .when()
                .get("/self")
                .then()
                .statusCode(200)
                .body("username", equalTo("username@gmail.com"))
                .body("firstName", equalTo("Brooke"))
                .body("lastName", equalTo("Kuttan"));
    }

    @Test
    void updateUser() {
        User user = new User();
        user.setUsername("username2@gmail.com");
        user.setFirstName("Brooke");
        user.setLastName("Kuttan");
        user.setPassword("password");

        ValidatableResponse validatableResponse = given()
                .contentType(ContentType.JSON)
                .body(userToJsonString(user))
                .when()
                .post()
                .then()
                .statusCode(201);

        String token = validatableResponse.extract().path("id");

        given()
                .param("username", "username2@gmail.com")
                .param("token", token)
                .headers("isIntegrationTestCheck", true)
                .log().all()
                .when()
                .get("/verify-email")
                .then()
                .log().all().assertThat().statusCode(200);

        User updatedUser = new User();
        updatedUser.setFirstName("Cookie");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Basic " + encodeBase64String("username2@gmail.com", "password"));

        given()
                .headers(httpHeaders)
                .contentType(ContentType.JSON)
                .body(userToJsonString(updatedUser))
                .when()
                .put("/self")
                .then()
                .log().all()
                .statusCode(204);

        given()
                .headers(httpHeaders)
                .when()
                .get("/self")
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Cookie"));

    }
}