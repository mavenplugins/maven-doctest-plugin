package com.github.mavenplugins.doctest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/cross-request")
public class CrossRequestController {
    
    @RequestMapping("/setHeader")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setHeader(HttpServletResponse response) {
        response.addHeader("X-Header", "X-Header-Value");
    }
    
    @RequestMapping("/withHeader/{header}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withHeader(HttpServletRequest request, @PathVariable("header") String headerValue) {
        if (!request.getHeader("X-Header").equals("X-Header-Value") || !headerValue.equals("X-Header-Value")) {
            throw new BadRequestException();
        }
    }
    
    @RequestMapping("/setCookie")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setCookie(HttpServletResponse response) {
        response.addCookie(new Cookie("X-Cookie", "X-Cookie-Value"));
    }
    
    @RequestMapping("/withCookie/{cookie}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withCookie(HttpServletRequest request, @PathVariable("cookie") String cookieValue) {
        Cookie cookie = null;
        
        for (Cookie tmp : request.getCookies()) {
            if (tmp.getName().equals("X-Cookie")) {
                cookie = tmp;
                break;
            }
        }
        
        if (cookie == null || !cookie.getValue().equals("X-Cookie-Value") || !cookieValue.equals("X-Cookie-Value")) {
            throw new BadRequestException();
        }
    }
    
    @RequestMapping("/getContent")
    public User getContent(HttpServletResponse response) {
        User user = new User();
        
        user.setFirstName("X-FirstName");
        user.setLastName("X-LastName");
        
        return user;
    }
    
    @RequestMapping("/withContent/{firstName}/{lastName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withContent(HttpServletRequest request, @PathVariable("firstName") String firstName,
            @PathVariable("lastName") String lastName) {
        if (!firstName.equals("X-FirstName") || !lastName.equals("X-LastName")) {
            throw new BadRequestException();
        }
    }
    
}
