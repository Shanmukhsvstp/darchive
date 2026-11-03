package com.conferit.darchive.APIs.EndPoints;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class Default {

    private static final String DEF_TEXT =
            "Hello there! Glad you were curious enough to come here.";

    @GetMapping("/")
    public String home() {
        return DEF_TEXT;
    }

    @GetMapping("/private")
    public String privateEndpoint() {
        return "You are authenticated!";
    }
}