package io.shiftleft.model;

import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class Customer {
  public Customer() {
  }

public Customer(String customerId, int clientId, String firstName, String lastName, Date dateOfBirth, String ssn,
      String socialInsurancenum, String tin, String phoneNumber, Address address, Set<Account> accounts) {
    super();
    
    // Validate input parameters to prevent null pointer exceptions and ensure data integrity
    if (customerId == null || firstName == null || lastName == null) {
        throw new IllegalArgumentException("Required customer fields cannot be null");
    }
    
    // Assign validated parameters to instance variables
    this.clientId = clientId;
    this.customerId = customerId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.dateOfBirth = dateOfBirth;
    this.ssn = ssn;
    this.socialInsurancenum = socialInsurancenum;
    this.tin = tin;
    this.phoneNumber = phoneNumber;
    this.address = address;
    this.accounts = accounts != null ? accounts : new HashSet<Account>();
}


private String sanitizeInput(String input) {
    // Remove any potentially dangerous characters
    if (input == null) {
        return "";
    }
    // Remove HTML tags and special characters
    return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
}


  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  private String customerId;

  private int clientId;

  private String firstName;

  private String lastName;

  private Date dateOfBirth;

  private String ssn;

  private String socialInsurancenum;

  private String tin;

  private String phoneNumber;

  @OneToOne(cascade = { CascadeType.ALL })
  private Address address;

  @OneToMany(cascade = { CascadeType.ALL })
  private Set<Account> accounts;

  public long getId() {
    return id;
  }

  public String getCustomerId() {
    return customerId;
  }

  public int getClientId() {
    return clientId;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  public String getSsn() {
    return ssn;
  }

  public String getSocialInsurancenum() {
    return socialInsurancenum;
  }

  public String getTin() {
    return tin;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public Address getAddress() {
    return address;
  }

  public Set<Account> getAccounts() {
    return accounts;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public void setClientId(int clientId) {
    this.clientId = clientId;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public void setSsn(String ssn) {
    this.ssn = ssn;
  }

  public void setSocialInsurancenum(String socialInsurancenum) {
    this.socialInsurancenum = socialInsurancenum;
  }

  public void setTin(String tin) {
    this.tin = tin;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public void setAccounts(Set<Account> accounts) {
    this.accounts = accounts;
  }

@Override
public String toString() {
    // HTML-encode all user-supplied data to prevent XSS when toString() is used in web contexts
    return "Customer [id=" + id 
        + ", customerId=" + StringEscapeUtils.escapeHtml4(customerId) 
        + ", clientId=" + clientId 
        + ", firstName=" + StringEscapeUtils.escapeHtml4(firstName)
        + ", lastName=" + StringEscapeUtils.escapeHtml4(lastName) 
        + ", dateOfBirth=" + dateOfBirth 
        + ", ssn=" + StringEscapeUtils.escapeHtml4(ssn) 
        + ", socialInsurancenum=" + StringEscapeUtils.escapeHtml4(socialInsurancenum)
        + ", tin=" + StringEscapeUtils.escapeHtml4(tin) 
        + ", phoneNumber=" + StringEscapeUtils.escapeHtml4(phoneNumber) 
        + ", address=" + (address != null ? address.toString() : "null") 
        + ", accounts=" + (accounts != null ? accounts.toString() : "null") + "]";
}


public String toSafeString() {
    // Safe version for HTML output - excludes sensitive data and uses HTML encoding
    return "Customer [id=" + Encode.forHtml(String.valueOf(id)) + 
           ", customerId=" + Encode.forHtml(customerId) + 
           ", clientId=" + clientId + 
           ", firstName=" + Encode.forHtml(firstName) + 
           ", lastName=" + Encode.forHtml(lastName) + 
           ", dateOfBirth=" + Encode.forHtml(String.valueOf(dateOfBirth)) + 
           ", phoneNumber=" + Encode.forHtml(phoneNumber) + 
           ", address=" + Encode.forHtml(String.valueOf(address)) + "]";
}


}
