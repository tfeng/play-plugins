package me.tfeng.play.security.oauth2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.tfeng.play.security.SecurityContext;
import play.mvc.With;

@SecurityContext
@With(OAuth2AuthenticationAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface OAuth2Authentication {

}
