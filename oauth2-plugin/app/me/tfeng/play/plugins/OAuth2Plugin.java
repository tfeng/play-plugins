package me.tfeng.play.plugins;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import play.Application;
import play.Play;

@WithSpringConfig("classpath:oauth2-plugin.xml")
public class OAuth2Plugin extends AbstractPlugin<OAuth2Plugin> {

  public static OAuth2Plugin getInstance() {
    return Play.application().plugin(OAuth2Plugin.class);
  }

  @Autowired
  private AuthenticationManager clientAuthenticationManager;

  @Autowired
  private ClientDetailsService clientDetailsService;

  @Autowired
  private OAuth2AuthenticationManager oauth2AuthenticationManager;

  @Autowired
  private TokenGranter tokenGranter;

  @Autowired
  private AuthorizationServerTokenServices tokenServices;

  public OAuth2Plugin(Application application) {
    super(application);
  }

  public AuthenticationManager getClientAuthenticationManager() {
    return clientAuthenticationManager;
  }

  public ClientDetailsService getClientDetailsService() {
    return clientDetailsService;
  }

  public AuthenticationManager getOAuth2AuthenticationManager() {
    return oauth2AuthenticationManager;
  }

  public TokenGranter getTokenGranter() {
    return tokenGranter;
  }

  public AuthorizationServerTokenServices getTokenServices() {
    return tokenServices;
  }
}
