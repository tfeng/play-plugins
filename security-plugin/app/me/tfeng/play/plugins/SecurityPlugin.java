package me.tfeng.play.plugins;

import me.tfeng.play.security.SecurityContextStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import play.Application;
import play.Play;

public class SecurityPlugin extends AbstractPlugin<SecurityPlugin> {

  public static final String DEFAULT_SECURITY_COOKIE = "seco";

  public static SecurityPlugin getInstance() {
    return Play.application().plugin(SecurityPlugin.class);
  }

  @Value("${security-plugin.expiration-in-seconds:3600}")
  private int expirationInSeconds;

  @Autowired
  private SecurityContextStore securityContextStore;

  @Value("${security-plugin.security-cookie:seco}")
  private String securityCookie;

  public SecurityPlugin(Application application) {
    super(application);
  }

  public int getExpirationInSeconds() {
    return expirationInSeconds;
  }

  public SecurityContextStore getSecurityContextStore() {
    return securityContextStore;
  }

  public String getSecurityCookie() {
    return securityCookie;
  }
}
