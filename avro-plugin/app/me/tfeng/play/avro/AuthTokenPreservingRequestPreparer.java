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

package me.tfeng.play.avro;

import java.net.URL;

import com.ning.http.client.AsyncHttpClient;

import me.tfeng.play.http.RequestPreparer;
import play.mvc.Http.Request;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AuthTokenPreservingRequestPreparer implements RequestPreparer {

  private Request request;

  public AuthTokenPreservingRequestPreparer(Request request) {
    this.request = request;
  }

  @Override
  public void prepare(AsyncHttpClient.BoundRequestBuilder builder, String contentType, URL url) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null) {
      builder.setHeader("Authorization", authorization);
    }
  }
}
