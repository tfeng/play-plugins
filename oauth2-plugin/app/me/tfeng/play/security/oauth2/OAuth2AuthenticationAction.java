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

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    Authentication currentAuthentication =
        SecurityContextHolder.getContext().getAuthentication();
    try {
      Request request = context.request();
      String token = getAuthorizationToken(request);
      if (token == null) {
        SecurityContextHolder.clearContext();
        try {
          return delegate.call(context).recover(t -> handleAuthenticationError(t));
        } catch (Throwable t) {
          return Promise.pure(handleAuthenticationError(t));
        }
      } else {
        Promise<me.tfeng.play.security.oauth2.Authentication> promise =
            OAuth2Plugin.getInstance().getAuthenticationManager().authenticate(token);
        return promise.flatMap(authentication -> {
          org.springframework.security.oauth2.provider.OAuth2Authentication oauth2Authentication =
              new org.springframework.security.oauth2.provider.OAuth2Authentication(
                  getOAuth2Request(authentication.getClient()),
                  getAuthentication(authentication.getUser()));
          SecurityContextHolder.getContext().setAuthentication(oauth2Authentication);
          try {
            return delegate.call(context).recover(t -> handleAuthenticationError(t));
          } catch (Throwable t) {
            return Promise.pure(handleAuthenticationError(t));
          }
        }).recover(t -> handleAuthenticationError(t));
      }
    } finally {
      SecurityContextHolder.getContext().setAuthentication(currentAuthentication);
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

  private static String getAuthorizationToken(Request request) {
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

  private static OAuth2Request getOAuth2Request(ClientAuthentication client) {
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

  private static Result handleAuthenticationError(Throwable t) throws Throwable {
    if (OAuth2Plugin.isAuthenticationError(t)) {
      LOG.warn("Authentication failed", t);
      return Results.unauthorized();
    } else {
      throw t;
    }
  }

  @Override
  public Promise<Result> call(Context context) throws Throwable {
    return authorizeAndCall(context, delegate);
  }
}
