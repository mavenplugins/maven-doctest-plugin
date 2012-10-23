package com.github.mavenplugins.doctest;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/order")
public class OrderController {
    
    AtomicInteger counter = new AtomicInteger();
    
    @RequestMapping("")
    @ResponseBody
    public Integer incrementCounter() {
        return counter.getAndIncrement();
    }
    
}
