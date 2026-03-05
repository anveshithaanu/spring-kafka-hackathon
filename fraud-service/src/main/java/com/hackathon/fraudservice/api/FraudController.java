package com.hackathon.fraudservice.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fraud")
public class FraudController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "fraud-service",
                "status", "UP"
        );
    }
}
