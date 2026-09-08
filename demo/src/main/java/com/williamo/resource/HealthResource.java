package com.williamo.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthResource {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("status", "UP");
        return body;
    }
}
