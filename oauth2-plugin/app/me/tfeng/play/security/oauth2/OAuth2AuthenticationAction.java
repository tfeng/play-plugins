/**
 * Copyright 2014 Thomas Feng
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tfeng.play.security.oauth2;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import me.tfeng.play.plugins.OAuth2Plugin;
import me.tfeng.play.plugins.SpringPlugin;

import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Request;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;


/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class OAuth2AuthenticationAction extends Action<OAuth2Authentication> {

  public static String ACCESS_TOKEN = "access_token";

  public static String AUTHORIZATION = "Authorization";

  public static String BEARER = "Bearer";

  private static final ALogger LOG = Logger.of(OAuth2AuthenticationAction.class);

  protected static Promise<Result> authorizeAndCall(Context context, Action<?> delegate)
      throws Throwable {
    Request request = context.request();
    String token = getAuthorizationToken(request);
    ApplicationEventMulticaster multicaster =
        SpringPlugin.getInstance().getApplicationContext().getBean(
            AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
            ApplicationEventMulticaster.class);

    if (token == null) {
      LOG.info("Authentication skipped");
      SecurityContextHolder.clearContext();
      AuthenticationCredentialsNotFoundEventListener
          authenticationCredentialsNotFoundEventListener =
              new AuthenticationCredentialsNotFoundEventListener(context.id());
      multicaster.addApplicationListener(authenticationCredentialsNotFoundEventListener);
      try {
        return delegate.call(context).recover(t -> {
          if (OAuth2Plugin.isAuthenticationError(t)) {
            LOG.warn("Authentication failed", t);
            return Results.unauthorized();
          } else {
            throw t;
          }
        });
      } catch (Exception e) {
        AuthenticationCredentialsNotFoundException authenticationCredentialsNotFoundException =
            authenticationCredentialsNotFoundEventListener.getException();
        if (authenticationCredentialsNotFoundException == null) {
          throw e;
        } else {
          return Promise.pure(Results.unauthorized());
        }
      } finally {
        multicaster.removeApplicationListener(authenticationCredentialsNotFoundEventListener);
      }

    } else {
      Promise<me.tfeng.play.security.oauth2.Authentication> promise =
          OAuth2Plugin.getInstance().getAuthenticationManager().authenticate(token);
      return promise.flatMap(authentication -> {
        LOG.info("Authentication completed");
        org.springframework.security.oauth2.provider.OAuth2Authentication oauth2Authentication =
            new org.springframework.security.oauth2.provider.OAuth2Authentication(
                getOAuth2Request(authentication.getClient()),
                getAuthentication(authentication.getUser()));
        SecurityContextHolder.getContext().setAuthentication(oauth2Authentication);
        AuthorizationFailureEventListener authorizationFailureEventListener =
            new AuthorizationFailureEventListener(context.id());
        multicaster.addApplicationListener(authorizationFailureEventListener);
        try {
          return delegate.call(context).recover(t -> {
            if (OAuth2Plugin.isAuthenticationError(t)) {
              LOG.warn("Authentication failed", t);
              return Results.unauthorized();
            } else {
              throw t;
            }
          });
        } catch (Exception e) {
          AccessDeniedException accessDeniedException =
              authorizationFailureEventListener.getException();
          if (accessDeniedException == null) {
            throw e;
          } else {
            LOG.warn("Authentication failed", e);
            return Promise.pure(Results.unauthorized());
          }
        } finally {
          multicaster.removeApplicationListener(authorizationFailureEventListener);
          SecurityContextHolder.clearContext();
        }
      }).recover(t -> {
        if (OAuth2Plugin.isAuthenticationError(t)) {
          LOG.warn("Authentication failed", t);
          return Results.unauthorized();
        } else {
          throw t;
        }
      });
    }
  }

  protected static UsernamePasswordAuthenticationToken getAuthentication(UserAuthentication user) {
    if (user == null) {
      return null;
    } else {
      List<GrantedAuthority> authorities = user.getAuthorities().stream()
          .map(authority -> new SimpleGrantedAuthority(authority.toString()))
          .collect(Collectors.toList());
      return new UsernamePasswordAuthenticationToken(user.getId().toString(), null, authorities);
    }
  }

  protected static String getAuthorizationToken(Request request) {
    String[] headers = request.headers().get(AUTHORIZATION);
    if (headers != null) {
      for (String header : headers) {
        if (header.toLowerCase().startsWith(BEARER.toLowerCase())) {
          String authHeaderValue = header.substring(BEARER.length()).trim();
          return authHeaderValue.split(",")[0];
        }
      }
    }
    return request.getQueryString(ACCESS_TOKEN);
  }

  protected static OAuth2Request getOAuth2Request(ClientAuthentication client) {
    List<GrantedAuthority> authorities = client.getAuthorities().stream()
        .map(authority -> new SimpleGrantedAuthority(authority.toString()))
        .collect(Collectors.toList());
    Set<String> scopes = client.getScopes().stream()
        .map(scope -> scope.toString())
        .collect(Collectors.toSet());
    return new OAuth2Request(
        Collections.emptyMap(),
        client.getId().toString(),
        authorities,
        true,
        scopes,
        Collections.emptySet(),
        null,
        Collections.emptySet(),
        Collections.emptyMap());
  }

  @Override
  public Promise<Result> call(Context context) throws Throwable {
    return authorizeAndCall(context, delegate);
  }
}
