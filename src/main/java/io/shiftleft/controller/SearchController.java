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
public String doGetSearch(@RequestParam String foo, HttpServletResponse response, HttpServletRequest request) {
    // Initialize logger for security event tracking
    Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    // Default safe message
    String message = "Invalid search query";
    
    try {
        // Input validation: Check if parameter is null or empty
        if (foo == null || foo.trim().isEmpty()) {
            logger.warn("Empty or null search parameter received from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Search parameter cannot be empty";
        }
        
        // Input validation: Whitelist approach - only allow alphanumeric characters and spaces
        Pattern allowedPattern = Pattern.compile("^[a-zA-Z0-9\\s]{1,100}$");
        if (!allowedPattern.matcher(foo).matches()) {
            logger.warn("Invalid search parameter detected from IP: {}. Parameter: {}", 
                       request.getRemoteAddr(), Encode.forJava(foo));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Search parameter contains invalid characters";
        }
        
        // REMOVED: SpEL expression parsing which allowed remote code execution
        // SpEL should never be used with user-controlled input
        // ExpressionParser parser = new SpelExpressionParser();
        // Expression exp = parser.parseExpression(foo);
        // message = (Object) exp.getValue();
        
        // Replace with safe search functionality
        // Perform actual search operation with sanitized input
        message = performSafeSearch(foo);
        
    } catch (Exception ex) {
        // Proper exception logging without exposing sensitive details to user
        logger.error("Error processing search request from IP: {}", request.getRemoteAddr(), ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return "An error occurred while processing your search";
    }
    
    // Encode output to prevent XSS
    return Encode.forHtml(message);
}

// Safe search method that doesn't use dynamic code execution
private String performSafeSearch(String searchTerm) {
    // Implement actual search logic here (e.g., database query with parameterized statements)
    // This is a placeholder for the actual safe search implementation
    return "Search results for: " + searchTerm;
}

    return message.toString();
  }
}
