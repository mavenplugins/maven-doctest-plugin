package com.github.mavenplugins.doctest;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/redirect")
public class RedirectController {
    
    AtomicInteger counter = new AtomicInteger();
    
    @RequestMapping("")
    public String incrementCounter() {
        return "redirect:/index.html";
    }
    
}
