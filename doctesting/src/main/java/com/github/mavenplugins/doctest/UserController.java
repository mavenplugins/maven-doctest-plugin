package com.github.mavenplugins.doctest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserController {
    
    @RequestMapping("/jack")
    public User user() {
        User user = new User();
        
        user.setFirstName("Jack");
        user.setLastName("Daniels");
        
        return user;
    }
    
}
