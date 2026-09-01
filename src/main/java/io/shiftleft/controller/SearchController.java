package io.shiftleft.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Search login
 */
@Controller
public class SearchController {

@RequestMapping(value = "/search/user", method = RequestMethod.GET)
@ResponseBody
public String doGetSearch(@RequestParam String foo, HttpServletResponse response, HttpServletRequest request) {
    // Set secure response headers to prevent XSS attacks
    response.setHeader("Content-Type", "text/plain; charset=UTF-8");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-XSS-Protection", "1; mode=block");
    
    // Input validation: check if the input is null or empty
    if (foo == null || foo.trim().isEmpty()) {
        return Encode.forHtml("Invalid input provided");
    }
    
    // Input validation: restrict input length to prevent abuse
    if (foo.length() > 100) {
        return Encode.forHtml("Input exceeds maximum allowed length");
    }
    
    // Input validation: whitelist allowed characters (alphanumeric and spaces only)
    Pattern allowedPattern = Pattern.compile("^[a-zA-Z0-9\\s]+$");
    if (!allowedPattern.matcher(foo).matches()) {
        return Encode.forHtml("Input contains invalid characters");
    }
    
    String message = "Search result for: " + foo;
    
    // Apply HTML encoding to prevent XSS attacks before returning the response
    return Encode.forHtml(message);
}

    return message.toString();
  }
}
