import me.tfeng.play.plugins.SpringPlugin;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

import play.GlobalSettings;
import play.libs.F.Promise;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

public class Global extends GlobalSettings {

  @Override
  public <A> A getControllerInstance(Class<A> clazz) {
    try {
      return SpringPlugin.getInstance().getApplicationContext().getBean(clazz);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Promise<Result> onError(RequestHeader request, Throwable t) {
    Throwable cause = t.getCause();
    if (cause instanceof AccessDeniedException
        || cause instanceof AuthenticationException
        || cause instanceof ClientRegistrationException) {
      return Promise.pure(Results.unauthorized());
    } else if (cause instanceof OAuth2Exception) {
      return Promise.pure(Results.status(((OAuth2Exception) cause).getHttpErrorCode()));
    } else {
      return Promise.pure(Results.internalServerError());
    }
  }
}
