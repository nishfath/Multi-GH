package io.shiftleft.controller;

import io.shiftleft.model.Account;
import io.shiftleft.model.Address;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import io.shiftleft.data.DataLoader;
import io.shiftleft.exception.CustomerNotFoundException;
import io.shiftleft.exception.InvalidCustomerRequestException;
import io.shiftleft.model.Customer;
import io.shiftleft.repository.CustomerRepository;

import org.springframework.web.util.HtmlUtils;

/**
 * Customer Controller exposes a series of RESTful endpoints
 */

@Configuration
@EnableEncryptableProperties
@PropertySource({ "classpath:config/application-sfdc.properties" })
@RestController
public class CustomerController {

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	Environment env;
	
	private static Logger log = LoggerFactory.getLogger(CustomerController.class);

	@PostConstruct
	public void init() {
		log.info("Start Loading SalesForce Properties");
		log.info("Url is {}", env.getProperty("sfdc.url"));
		log.info("UserName is {}", env.getProperty("sfdc.username"));
		log.info("Password is {}", env.getProperty("sfdc.password"));
		log.info("End Loading SalesForce Properties");
	}

	private void dispatchEventToSalesForce(String event)
			throws ClientProtocolException, IOException, AuthenticationException {
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(env.getProperty("sfdc.url"));
		httpPost.setEntity(new StringEntity(event));
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(env.getProperty("sfdc.username"),
				env.getProperty("sfdc.password"));
		httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));

		CloseableHttpResponse response = client.execute(httpPost);
		log.info("Response from SFDC is {}", response.getStatusLine().getStatusCode());
		client.close();
	}

	/**
	 * Get customer using id. Returns HTTP 404 if customer not found
	 *
	 * @param customerId
	 * @return retrieved customer
	 */
	@RequestMapping(value = "/customers/{customerId}", method = RequestMethod.GET)
	public Customer getCustomer(@PathVariable("customerId") Long customerId) {

		/* validate customer Id parameter */
      if (null == customerId) {
        throw new InvalidCustomerRequestException();
      }

      Customer customer = customerRepository.findOne(customerId);
		if (null == customer) {
		  throw new CustomerNotFoundException();
	  }

	  Account account = new Account(4242l,1234, "savings", 1, 0);
	  log.info("Account Data is {}", account);
	  log.info("Customer Data is {}", customer);

      try {
        dispatchEventToSalesForce(String.format(" Customer %s Logged into SalesForce", customer));
      } catch (Exception e) {
        log.error("Failed to Dispatch Event to SalesForce . Details {} ", e.getLocalizedMessage());

      }

      return customer;
    }

    /**
     * Handler for / loads the index.tpl
     * @param httpResponse
     * @param request
     * @return
     * @throws IOException
     */
      @RequestMapping(value = "/", method = RequestMethod.GET)
      public String index(HttpServletResponse httpResponse, WebRequest request) throws IOException {
	  	ClassPathResource cpr = new ClassPathResource("static/index.html");
	  	String ret = "";
		  try {
			  byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
			  ret= new String(bdata, StandardCharsets.UTF_8);
		  } catch (IOException e) {
			  //LOG.warn("IOException", e);
		  }
		  return ret;
      }

      /**
       * Check if settings= is present in cookie
       * @param request
       * @return
       */
      private boolean checkCookie(WebRequest request) throws Exception {
      	try {
			return request.getHeader("Cookie").startsWith("settings=");
		}
		catch (Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return false;
      }

      /**
       * restores the preferences on the filesystem
       *
       * @param httpResponse
       * @param request
       * @throws Exception
       */
      @RequestMapping(value = "/loadSettings", method = RequestMethod.GET)
      public void loadSettings(HttpServletResponse httpResponse, WebRequest request) throws Exception {
        // get cookie values
        if (!checkCookie(request)) {
          httpResponse.getOutputStream().println("Error");
          throw new Exception("cookie is incorrect");
        }
        String md5sum = request.getHeader("Cookie").substring("settings=".length(), 41);
    	ClassPathResource cpr = new ClassPathResource("static");
    	File folder = new File(cpr.getPath());
		File[] listOfFiles = folder.listFiles();
        String filecontent = new String();
        for (File f : listOfFiles) {
          // not efficient, i know
          filecontent = new String();
          byte[] encoded = Files.readAllBytes(f.toPath());
          filecontent = new String(encoded, StandardCharsets.UTF_8);
          if (filecontent.contains(md5sum)) {
            // this will send me to the developer hell (if exists)

            // encode the file settings, md5sum is removed
            String s = new String(Base64.getEncoder().encode(filecontent.replace(md5sum, "").getBytes()));
            // setting the new cookie
            httpResponse.setHeader("Cookie", "settings=" + s + "," + md5sum);
            return;
          }
        }
      }


  /**
   * Saves the preferences (screen resolution, language..) on the filesystem
   *
   * @param httpResponse
   * @param request
   * @throws Exception
   */
@RequestMapping(value = "/saveSettings", method = RequestMethod.POST)
public void saveSettings(HttpServletResponse httpResponse, WebRequest request) throws Exception {
    // Logger for security events
    Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    // "Settings" will be stored in a cookie
    // schema: base64(filename,value1,value2...), md5sum(base64(filename,value1,value2...))
    
    try {
        if (!checkCookie(request)) {
            logger.warn("Cookie validation failed");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getOutputStream().println("Authentication error");
            return;
        }

        String settingsCookie = request.getHeader("Cookie");
        if (settingsCookie == null || settingsCookie.isEmpty()) {
            logger.warn("Cookie header is missing");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getOutputStream().println("Invalid request");
            return;
        }

        String[] cookie = settingsCookie.split(",");
        if (cookie.length < 2) {
            logger.warn("Malformed cookie received");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getOutputStream().println("Malformed cookie");
            return;
        }

        String base64txt = cookie[0].replace("settings=", "");

        // Check md5sum for integrity verification
        String cookieMD5sum = cookie[1];
        String calcMD5Sum = DigestUtils.md5Hex(base64txt);
        if (!cookieMD5sum.equals(calcMD5Sum)) {
            logger.warn("MD5 checksum validation failed");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getOutputStream().println("Integrity check failed");
            return;
        }

        // Decode and parse settings
        String[] settings = new String(Base64.getDecoder().decode(base64txt)).split(",");
        if (settings.length < 1) {
            logger.warn("No filename provided in settings");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getOutputStream().println("Invalid settings format");
            return;
        }

        // Validate and sanitize filename to prevent directory traversal
        String filename = sanitizeFilename(settings[0]);
        if (filename == null || filename.isEmpty()) {
            logger.warn("Invalid filename after sanitization: " + settings[0]);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getOutputStream().println("Invalid filename");
            return;
        }

        // Storage will have ClassPathResource as basepath
        ClassPathResource cpr = new ClassPathResource("./static/");
        Path basePath = Paths.get(cpr.getPath()).normalize().toAbsolutePath();
        Path targetPath = basePath.resolve(filename).normalize().toAbsolutePath();

        // Ensure the resolved path is within the base directory (prevent directory traversal)
        if (!targetPath.startsWith(basePath)) {
            logger.error("Directory traversal attempt detected: " + settings[0]);
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.getOutputStream().println("Access denied");
            return;
        }

        File file = targetPath.toFile();
        
        // Validate file extension (whitelist approach)
        if (!isAllowedFileExtension(filename)) {
            logger.warn("Disallowed file extension: " + filename);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getOutputStream().println("File type not allowed");
            return;
        }

        // Create parent directories if they don't exist (within allowed base path)
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                logger.error("Failed to create directory: " + parentDir.getAbsolutePath());
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                httpResponse.getOutputStream().println("Failed to create directory");
                return;
            }
        }

        // Write settings to file
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            // First entry is the filename -> remove it
            String[] settingsArr = Arrays.copyOfRange(settings, 1, settings.length);
            
            // Validate settings content before writing
            for (String setting : settingsArr) {
                if (!isValidSettingContent(setting)) {
                    logger.warn("Invalid setting content detected");
                    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    httpResponse.getOutputStream().println("Invalid setting content");
                    return;
                }
            }
            
            // Write one setting per line
            fos.write(String.join("\n", settingsArr).getBytes("UTF-8"));
            fos.write(("\n" + cookie[cookie.length - 1]).getBytes("UTF-8"));
        }
        
        logger.info("Settings saved successfully for file: " + filename);
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        httpResponse.getOutputStream().println("Settings Saved");
        
    } catch (IllegalArgumentException e) {
        logger.error("Invalid input data: " + e.getMessage());
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.getOutputStream().println("Invalid input");
    } catch (IOException e) {
        logger.error("I/O error during file operation: " + e.getMessage());
        httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        httpResponse.getOutputStream().println("Internal server error");
    } catch (Exception e) {
        logger.error("Unexpected error: " + e.getMessage());
        httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        httpResponse.getOutputStream().println("Internal server error");
    }
}

private String sanitizeFilename(String filename) {
    if (filename == null) {
        return null;
    }
    
    // Remove any path separators and null bytes
    String sanitized = filename.replaceAll("[\\x00/\\\\:]", "");
    
    // Remove parent directory references
    sanitized = sanitized.replaceAll("\\.\\.", "");
    
    // Remove leading/trailing whitespace and dots
    sanitized = sanitized.trim().replaceAll("^\\.+", "");
    
    // Only allow alphanumeric characters, hyphens, underscores, and single dots
    if (!Pattern.matches("^[a-zA-Z0-9._-]+$", sanitized)) {
        return null;
    }
    
    // Ensure filename is not empty after sanitization
    if (sanitized.isEmpty() || sanitized.length() > 255) {
        return null;
    }
    
    return sanitized;
}

private boolean isAllowedFileExtension(String filename) {
    if (filename == null) {
        return false;
    }
    
    // Whitelist of allowed file extensions
    String[] allowedExtensions = {".txt", ".json", ".xml", ".properties", ".conf"};
    
    String lowerFilename = filename.toLowerCase();
    for (String ext : allowedExtensions) {
        if (lowerFilename.endsWith(ext)) {
            return true;
        }
    }
    
    return false;
}

private boolean isValidSettingContent(String content) {
    if (content == null) {
        return false;
    }
    
    // Prevent excessively long content
    if (content.length() > 10000) {
        return false;
    }
    
    // Check for null bytes or other control characters that might cause issues
    if (content.contains("\u0000")) {
        return false;
    }
    
    return true;
}


  /**
   * Debug test for saving and reading a customer
   *
   * @param firstName String
   * @param lastName String
   * @param dateOfBirth String
   * @param ssn String
   * @param tin String
   * @param phoneNumber String
   * @param httpResponse
   * @param request
   * @return String
   * @throws IOException
   */
@RequestMapping(value = "/debug", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
public ResponseEntity<String> debug(@RequestParam String customerId,
                  @RequestParam int clientId,
                  @RequestParam String firstName,
                  @RequestParam String lastName,
                  @RequestParam String dateOfBirth,
                  @RequestParam String ssn,
                  @RequestParam String socialSecurityNum,
                  @RequestParam String tin,
                  @RequestParam String phoneNumber,
                  HttpServletResponse httpResponse,
                  WebRequest request) throws IOException {

    // Validate and sanitize input parameters to prevent injection attacks
    if (customerId == null || firstName == null || lastName == null || dateOfBirth == null) {
        return ResponseEntity.badRequest().body("Missing required parameters");
    }

    // Create empty account set for debug purposes
    Set<Account> accounts1 = new HashSet<Account>();
    
    // Parse date and create customer object with user-supplied data
    Customer customer1 = new Customer(customerId, clientId, firstName, lastName, 
                                      DateTime.parse(dateOfBirth).toDate(),
                                      ssn, socialSecurityNum, tin, phoneNumber, 
                                      new Address("Debug str", "", "Debug city", "CA", "12345"),
                                      accounts1);

    // Save customer to repository
    customerRepository.save(customer1);
    
    // Set response headers
    httpResponse.setStatus(HttpStatus.CREATED.value());
    httpResponse.setHeader("Location", String.format("%s/customers/%s",
                           request.getContextPath(), customer1.getId()));

    // Return plain text response instead of HTML to prevent XSS
    // Return only the customer ID instead of full toString() with user data
    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body("Customer created with ID: " + customer1.getId());
}

                                      new Address("Debug str", "", "Debug city", "CA", "12345"),
                                      accounts1);

    // Save customer to repository
    customerRepository.save(customer1);
    
    // Set HTTP response status and headers
    httpResponse.setStatus(HttpStatus.CREATED.value());
    httpResponse.setHeader("Location", String.format("%s/customers/%s",
                           request.getContextPath(), customer1.getId()));

    // Properly encode output to prevent XSS
    String customerInfo = customer1.toSafeString();
    return Encode.forHtml(customerInfo);
}


	/**
	 * Debug test for saving and reading a customer
	 *
	 * @param firstName String
	 * @param httpResponse
	 * @param request
	 * @return void
	 * @throws IOException
	 */
	@RequestMapping(value = "/debugEscaped", method = RequestMethod.GET)
	public void debugEscaped(@RequestParam String firstName, HttpServletResponse httpResponse,
					  WebRequest request) throws IOException{
		String escaped = HtmlUtils.htmlEscape(firstName);
		System.out.println(escaped);
		httpResponse.getOutputStream().println(escaped);
	}
	/**
	 * Gets all customers.
	 *
	 * @return the customers
	 */
	@RequestMapping(value = "/customers", method = RequestMethod.GET)
	public List<Customer> getCustomers() {
		return (List<Customer>) customerRepository.findAll();
	}

	/**
	 * Create a new customer and return in response with HTTP 201
	 *
	 * @param the
	 *            customer
	 * @return created customer
	 */
	@RequestMapping(value = { "/customers" }, method = { RequestMethod.POST })
	public Customer createCustomer(@RequestParam Customer customer, HttpServletResponse httpResponse,
								   WebRequest request) {

		Customer createdcustomer = null;
		createdcustomer = customerRepository.save(customer);
		httpResponse.setStatus(HttpStatus.CREATED.value());
		httpResponse.setHeader("Location",
				String.format("%s/customers/%s", request.getContextPath(), customer.getId()));

		return createdcustomer;
	}

	/**
	 * Update customer with given customer id.
	 *
	 * @param customer
	 *            the customer
	 */
	@RequestMapping(value = { "/customers/{customerId}" }, method = { RequestMethod.PUT })
	public void updateCustomer(@RequestBody Customer customer, @PathVariable("customerId") Long customerId,
			HttpServletResponse httpResponse) {

		if (!customerRepository.exists(customerId)) {
			httpResponse.setStatus(HttpStatus.NOT_FOUND.value());
		} else {
			customerRepository.save(customer);
			httpResponse.setStatus(HttpStatus.NO_CONTENT.value());
		}
	}

	/**
	 * Deletes the customer with given customer id if it exists and returns
	 * HTTP204.
	 *
	 * @param customerId
	 *            the customer id
	 */
	@RequestMapping(value = "/customers/{customerId}", method = RequestMethod.DELETE)
	public void removeCustomer(@PathVariable("customerId") Long customerId, HttpServletResponse httpResponse) {

		if (customerRepository.exists(customerId)) {
			customerRepository.delete(customerId);
		}

		httpResponse.setStatus(HttpStatus.NO_CONTENT.value());
	}

}
