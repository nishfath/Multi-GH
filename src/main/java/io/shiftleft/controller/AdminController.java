package io.shiftleft.controller;

import io.shiftleft.model.AuthToken;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * Admin checks login
 */
@Controller
public class AdminController {
  private String fail = "redirect:/";

  // helper
private boolean isAdmin(String auth) {
    try {
        // Use JWT-based token validation instead of Java deserialization
        // Parse and validate the JWT token
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSecretKey())
            .build()
            .parseClaimsJws(auth)
            .getBody();
        
        // Verify the role claim
        String role = claims.get("role", String.class);
        return "ADMIN".equals(role);
    } catch (Exception ex) {
        System.out.println("Invalid authentication token: " + ex.getMessage());
        return false;
    }
}

  }

  //
  @RequestMapping(value = "/admin/printSecrets", method = RequestMethod.POST)
  public String doPostPrintSecrets(HttpServletResponse response, HttpServletRequest request) {
    return fail;
  }


  @RequestMapping(value = "/admin/printSecrets", method = RequestMethod.GET)
  public String doGetPrintSecrets(@CookieValue(value = "auth", defaultValue = "notset") String auth, HttpServletResponse response, HttpServletRequest request) throws Exception {

    if (request.getSession().getAttribute("auth") == null) {
      return fail;
    }

    String authToken = request.getSession().getAttribute("auth").toString();
    if(!isAdmin(authToken)) {
      return fail;
    }

    ClassPathResource cpr = new ClassPathResource("static/calculations.csv");
    try {
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      response.getOutputStream().println(new String(bdata, StandardCharsets.UTF_8));
      return null;
    } catch (IOException ex) {
      ex.printStackTrace();
      // redirect to /
      return fail;
    }
  }

  /**
   * Handle login attempt
   * @param auth cookie value base64 encoded
   * @param password hardcoded value
   * @param response -
   * @param request -
   * @return redirect to company numbers
   * @throws Exception
   */
@RequestMapping(value = "/admin/login", method = RequestMethod.POST)
public String doPostLogin(@CookieValue(value = "auth", defaultValue = "notset") String auth, @RequestBody String password, HttpServletResponse response, HttpServletRequest request) throws Exception {
    String succ = "redirect:/admin/printSecrets";

    try {
        // Check existing valid token
        if (!auth.equals("notset")) {
            if(isAdmin(auth)) {
                request.getSession().setAttribute("auth", auth);
                return succ;
            }
        }

        // Parse and validate password input
        String[] pass = password.split("=");
        if(pass.length != 2) {
            return fail;
        }
        
        // Use constant-time comparison to prevent timing attacks
        if(pass[1] != null && pass[1].length() > 0 && constantTimeEquals(pass[1], "shiftleftsecret")) {
            // Generate JWT token instead of serialized object
            String jwtToken = generateJwtToken("ADMIN");
            
            // Set secure cookie attributes
            Cookie authCookie = new Cookie("auth", jwtToken);
            authCookie.setHttpOnly(true);
            authCookie.setSecure(true); // Only send over HTTPS
            authCookie.setPath("/");
            authCookie.setMaxAge(3600); // 1 hour expiration
            response.addCookie(authCookie);

            // Store token in session
            request.getSession().setAttribute("auth", jwtToken);

            return succ;
        }
        return fail;
    } catch (Exception ex) {
        // Log error without exposing stack trace to user
        System.err.println("Login error: " + ex.getMessage());
        return fail;
    }
}

  }

  /**
   * Same as POST but just a redirect
   * @param response
   * @param request
   * @return redirect
   */
  @RequestMapping(value = "/admin/login", method = RequestMethod.GET)
  public String doGetLogin(HttpServletResponse response, HttpServletRequest request) {
    return "redirect:/";
  }
}
