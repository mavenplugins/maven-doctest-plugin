package com.github.mavenplugins.doctest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/user")
public class UserController {
    
    @RequestMapping("/jack")
    public User jack() {
        User user = new User();
        Friend friend = new Friend();
        
        Address address = new Address();
        
        address.setCity("New York");
        address.setCountry("USA");
        address.setNumber("7A");
        address.setStreet("Main Ave.");
        address.setZipcode("7A1234");
        
        friend.setFirstName("Freddy");
        friend.setLastName("Johnson");
        
        user.setFirstName("Jack");
        user.setLastName("Daniels");
        user.setAddress(address);
        user.getFriends().add(friend);
        
        return user;
    }
    
    @RequestMapping(value = "/johnny", method = RequestMethod.GET)
    public User johnny() {
        User user = new User();
        
        Address address = new Address();
        
        address.setCity("Denver");
        address.setCountry("USA");
        address.setNumber("1110");
        address.setStreet("Main Ave.");
        address.setZipcode("1384H");
        
        user.setFirstName("Johnny");
        user.setLastName("Walker");
        user.setAddress(address);
        user.getFriends().add(new Friend(jack()));
        
        return user;
    }
    
    @RequestMapping(value = "/setJohnny", method = RequestMethod.PUT)
    public String setUser(@RequestBody User user) {
        return "{}";
    }
    
}
