package com.fyp.digitaltwin.controller;

import com.fyp.digitaltwin.engine.DigitalTwinEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DigitalTwinController {

    @Autowired
    private DigitalTwinEngine engine;

    // Endpoint: http://localhost:8080/api/status
    @GetMapping("/status")
    public String getStatus() {
        return engine.getLiveStatus();
    }

    // Endpoint: http://localhost:8080/api/hello
    @GetMapping("/hello")
    public String sayHello() {
        return "Digital Twin Server is Online!";
    }

    // Endpoint: http://localhost:8080/api/validate
    @GetMapping(value = "/validation",produces = MediaType.TEXT_PLAIN_VALUE) //MediaType.TEXT_PLAIN_VALUE mean this api return plain
    public String getValidation() {
        return engine.getValidationReport();
    }
    // Endpoint: http://localhost:8080/api/dashboard
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE) // Tells browser this is JSON
    /*ResponseEntity is a springboot class that represents a full HTTP response
      <String> : the type of data in the body in this case a JSON string
     */
    public ResponseEntity<String> getDashboard(){
        //Get raw JSON string from the engine
        String json = engine.getDashboardData();

        //Send it to the web browser as json
        return ResponseEntity.ok(json);
    }
}