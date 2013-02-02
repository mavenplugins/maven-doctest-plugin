package com.github.mavenplugins.doctest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cookie")
public class CookieController {
    
    @RequestMapping("/{name}/{value}")
    public Cookie[] incrementCounter(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("name") String name, @PathVariable("value") String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        response.addCookie(cookie);
        
        return request.getCookies();
    }
    
}
