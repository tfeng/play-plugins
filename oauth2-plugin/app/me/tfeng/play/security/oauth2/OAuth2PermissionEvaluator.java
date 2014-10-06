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

import java.io.Serializable;

import me.tfeng.play.plugins.OAuth2Plugin;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class OAuth2PermissionEvaluator implements PermissionEvaluator {

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    PermissionEvaluator permissionEvaluator = OAuth2Plugin.getInstance().getPermissionEvaluator();
    if (permissionEvaluator == null) {
      throw new RuntimeException("Permission evaluator is not configured");
    } else {
      return permissionEvaluator.hasPermission(authentication, targetDomainObject, permission);
    }
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId,
      String targetType, Object permission) {
    PermissionEvaluator permissionEvaluator = OAuth2Plugin.getInstance().getPermissionEvaluator();
    if (permissionEvaluator == null) {
      throw new RuntimeException("Permission evaluator is not configured");
    } else {
      return permissionEvaluator.hasPermission(authentication, targetId, targetType, permission);
    }
  }
}
