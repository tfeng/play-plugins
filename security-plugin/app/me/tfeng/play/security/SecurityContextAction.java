package me.tfeng.play.security;

import java.util.UUID;

import me.tfeng.play.plugins.SecurityPlugin;

import org.springframework.security.core.context.SecurityContextHolder;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Cookie;
import play.mvc.Result;

public class SecurityContextAction extends Action<SecurityContext> {

  @Override
  public Promise<Result> call(Context context) throws Throwable {
    SecurityPlugin securityPlugin = SecurityPlugin.getInstance();
    SecurityContextStore securityContextStore = securityPlugin.getSecurityContextStore();

    String cookieName = configuration.value().isEmpty() ? securityPlugin.getSecurityCookie()
        : configuration.value();
    Cookie cookie = context == null ? null : context.request().cookie(cookieName);
    String id = cookie == null ? null : cookie.value();

    if (id != null) {
      SecurityContextHolder.setContext(securityContextStore.load(id));
    }

    return delegate.call(context).map(result -> {
      org.springframework.security.core.context.SecurityContext currentContext =
          SecurityContextHolder.getContext();
      SecurityContextHolder.clearContext();
      org.springframework.security.core.context.SecurityContext emptyContext =
          SecurityContextHolder.getContext();
      boolean hasSecurityContext = !currentContext.equals(emptyContext);

      if (id != null) {
        securityContextStore.remove(id);
        if (!hasSecurityContext) {
          context.response().discardCookie(cookieName);
        }
      }

      if (hasSecurityContext) {
        int expirationInSeconds = configuration.expirationInSeconds();
        if (expirationInSeconds < 0) {
          expirationInSeconds = securityPlugin.getExpirationInSeconds();
        }

        String newId = UUID.randomUUID().toString();
        context.response().setCookie(cookieName, newId, expirationInSeconds);
        securityContextStore.save(newId, SecurityContextHolder.getContext(), expirationInSeconds);
      }

      return result;
    });
  }
}
