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

package me.tfeng.play.security;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;

import me.tfeng.play.plugins.SecurityPlugin;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Cookie;
import play.mvc.Result;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
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
