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

package me.tfeng.play.plugins;

import me.tfeng.play.security.SecurityContextStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import play.Application;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class SecurityPlugin extends AbstractPlugin<SecurityPlugin> {

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