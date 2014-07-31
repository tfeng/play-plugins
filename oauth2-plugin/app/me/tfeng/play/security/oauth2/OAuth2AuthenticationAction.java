package me.tfeng.play.security.oauth2;

import me.tfeng.play.plugins.OAuth2Plugin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;

public class OAuth2AuthenticationAction extends Action<OAuth2Authentication> {

  private static final ALogger LOG = Logger.of(OAuth2AuthenticationAction.class);

  @Override
  public Promise<Result> call(Context context) throws Throwable {
    Request request = context.request();
    String token = getAuthorizationToken(request);
    if (token == null) {
      token = request.getQueryString(OAuth2AccessToken.ACCESS_TOKEN);
    }
    if (token == null) {
      LOG.info("Authentication skipped");
    } else {
      Authentication authRequest = new PreAuthenticatedAuthenticationToken(token, "");
      Authentication authResult =
          OAuth2Plugin.getInstance().getOAuth2AuthenticationManager().authenticate(authRequest);
      SecurityContextHolder.getContext().setAuthentication(authResult);
      LOG.info("Authenticated successfully");
    }
    return delegate.call(context);
  }

  protected String getAuthorizationToken(Request request) {
    String[] headers = request.headers().get("Authorization");
    if (headers != null) {
      for (String header : headers) {
        if (header.toLowerCase().startsWith(OAuth2AccessToken.BEARER_TYPE.toLowerCase())) {
          String authHeaderValue = header.substring(OAuth2AccessToken.BEARER_TYPE.length()).trim();
          return authHeaderValue.split(",")[0];
        }
      }
    }
    return null;
  }
}
