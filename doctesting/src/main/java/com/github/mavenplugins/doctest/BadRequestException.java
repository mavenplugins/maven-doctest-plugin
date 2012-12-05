package com.github.mavenplugins.doctest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    
    private static final long serialVersionUID = -8603261745346821395L;
    
}
