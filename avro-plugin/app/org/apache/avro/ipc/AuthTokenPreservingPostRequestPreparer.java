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

package org.apache.avro.ipc;

import java.net.URL;

import me.tfeng.play.http.PostRequestPreparer;
import play.Logger;
import play.Logger.ALogger;
import play.mvc.Controller;
import play.mvc.Http.Request;

import com.ning.http.client.AsyncHttpClient;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AuthTokenPreservingPostRequestPreparer implements PostRequestPreparer {

  private static final ALogger LOG = Logger.of(AuthTokenPreservingPostRequestPreparer.class);

  @Override
  public void prepare(AsyncHttpClient.BoundRequestBuilder builder, String contentType, URL url) {
    Request currentRequest = null;
    try {
      currentRequest = Controller.request();
    } catch (RuntimeException e) {
      LOG.info("Unable to get current request; do not pass headers to downstream calls");
    }

    if (currentRequest != null) {
      builder.setHeader("Authorization", currentRequest.getHeader("Authorization"));
    }
  }
}
