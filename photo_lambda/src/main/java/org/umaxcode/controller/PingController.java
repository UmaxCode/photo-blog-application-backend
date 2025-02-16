package org.umaxcode.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.HashMap;
import java.util.Map;


@RestController
@EnableWebMvc
public class PingController {

    @Value("${application.aws.region}")
    private String awsRegion;

    @RequestMapping(path = "/ping", method = RequestMethod.GET)
    public Map<String, String> ping() {
        Map<String, String> pong = new HashMap<>();
        pong.put("pong", "Hello, World!");
        return pong;
    }

    @RequestMapping(path = "/health", method = RequestMethod.GET)
    public Map<String, String> health() {
        Map<String, String> health = new HashMap<>();
        health.put("message", "Application is healthy");
        health.put("status", "UP");
        health.put("region", awsRegion);
        return health;
    }
}
