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
import java.util.HashMap;
import java.util.Map;

import me.tfeng.play.plugins.OAuth2Plugin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.stereotype.Service;

import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Service
@OAuth2Authentication
@BodyParser.Of(BodyParser.Json.class)
public class OAuth2AuthenticationController extends Controller {

  public Promise<Result> authenticateClient() {
    JsonNode json = request().body().asJson();
    String clientId = json.findPath("clientId").textValue();
    String clientSecret = json.findPath("clientSecret").textValue();

    OAuth2Plugin oauth2Plugin = OAuth2Plugin.getInstance();
    UsernamePasswordAuthenticationToken authRequest =
        new UsernamePasswordAuthenticationToken(clientId, clientSecret);
    oauth2Plugin.getClientAuthenticationManager().authenticate(authRequest);

    ClientDetails clientDetails =
        oauth2Plugin.getClientDetailsService().loadClientByClientId(clientId);
    TokenRequest tokenRequest = new TokenRequest(Collections.emptyMap(), clientId,
        clientDetails.getScope(), "password");
    OAuth2AccessToken token =
        oauth2Plugin.getTokenGranter().grant("client_credentials", tokenRequest);

    ObjectNode result = Json.newObject();
    result.setAll(ImmutableMap.of(
        "accessToken", result.textNode(token.getValue()),
        "clientId", result.textNode(clientId),
        "expiration", result.numberNode(token.getExpiration().getTime())));
    return Promise.pure(ok(result));
  }

  @PreAuthorize("#oauth2.clientHasRole('ROLE_CLIENT') and #oauth2.hasScope('trust')")
  public Promise<Result> authenticateUser() {
    JsonNode json = request().body().asJson();
    String username = json.findPath("username").textValue();
    String password = json.findPath("password").textValue();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OAuth2Request clientAuthenticationRequest =
        ((org.springframework.security.oauth2.provider.OAuth2Authentication) authentication)
            .getOAuth2Request();
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("username", username);
    requestParameters.put("password", password);
    TokenRequest tokenRequest = new TokenRequest(requestParameters,
            clientAuthenticationRequest.getClientId(), clientAuthenticationRequest.getScope(),
            "password");
    OAuth2AccessToken token =
        OAuth2Plugin.getInstance().getTokenGranter().grant("password", tokenRequest);
    ObjectNode result = Json.newObject();
    result.setAll(ImmutableMap.of(
        "accessToken", result.textNode(token.getValue()),
        "username", result.textNode(username),
        "expiration", result.numberNode(token.getExpiration().getTime()),
        "refreshToken", result.textNode(token.getRefreshToken().getValue())));
    return Promise.pure(ok(result));
  }

  @PreAuthorize("#oauth2.clientHasRole('ROLE_CLIENT') and #oauth2.hasScope('trust')")
  public Promise<Result> refreshUserAccessToken() {
    JsonNode body = request().body().asJson();
    String refreshToken = body.findPath("refreshToken").textValue();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OAuth2Request clientAuthenticationRequest =
        ((org.springframework.security.oauth2.provider.OAuth2Authentication) authentication)
            .getOAuth2Request();
    TokenRequest tokenRequest =
        new TokenRequest(Collections.emptyMap(), clientAuthenticationRequest.getClientId(),
            clientAuthenticationRequest.getScope(), "refresh");
    OAuth2AccessToken token =
        OAuth2Plugin.getInstance().getTokenServices().refreshAccessToken(refreshToken,
            tokenRequest);
    ObjectNode result = Json.newObject();
    result.setAll(ImmutableMap.of(
        "accessToken", result.textNode(token.getValue()),
        "expiration", result.numberNode(token.getExpiration().getTime()),
        "refreshToken", result.textNode(token.getRefreshToken().getValue())));
    return Promise.pure(ok(result));
  }
}
