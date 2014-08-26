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

import me.tfeng.play.spring.WithSpringConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import play.Application;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@WithSpringConfig("classpath:oauth2-plugin.xml")
public class OAuth2Plugin extends AbstractPlugin<OAuth2Plugin> {

  public static OAuth2Plugin getInstance() {
    return Play.application().plugin(OAuth2Plugin.class);
  }

  @Autowired
  @Qualifier("oauth2-plugin.client-authentication-manager")
  private AuthenticationManager clientAuthenticationManager;

  @Autowired
  @Qualifier("oauth2-plugin.client-details-service")
  private ClientDetailsService clientDetailsService;

  @Autowired
  @Qualifier("oauth2-plugin.authentication-manager")
  private OAuth2AuthenticationManager oauth2AuthenticationManager;

  // @Autowired
  // @Qualifier("oauth2-plugin.token-granter")
  private TokenGranter tokenGranter;

  @Autowired
  @Qualifier("oauth2-plugin.token-services")
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

  @Override
  public void onStart() {
    super.onStart();

    tokenGranter = getApplicationContext().getBean(TokenGranter.class);
  }
}
