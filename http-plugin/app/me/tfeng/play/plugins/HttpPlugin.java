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

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.F.Promise;
import play.libs.ws.WSResponse;
import play.libs.ws.ning.NingWSResponse;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfigBean;
import com.ning.http.client.Response;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class HttpPlugin extends AbstractPlugin {

  public static final String[] PRESERVED_HEADERS = { "Authorization" };

  private static final ALogger LOG = Logger.of(HttpPlugin.class);

  public static HttpPlugin getInstance() {
    return Play.application().plugin(HttpPlugin.class);
  }

  @Autowired(required = false)
  private AsyncHttpClient asyncHttpClient;

  @Autowired(required = false)
  private AsyncHttpClientConfigBean asyncHttpClientConfig;

  @Value("${http-plugin.compression-enabled:false}")
  private boolean compressionEnabled;

  @Value("${http-plugin.connection-timeout-ms:60000}")
  private int connectionTimeoutMs;

  @Value("${http-plugin.max-total-connections:200}")
  private int maxTotalConnections;

  @Value("${http-plugin.request-timeout-ms:10000}")
  private int requestTimeoutInMs;

  public HttpPlugin(Application application) {
    super(application);
  }

  public int getRequestTimeout() {
    return requestTimeoutInMs;
  }

  @Override
  public void onStart() {
    super.onStart();

    if (asyncHttpClient == null) {
      if (asyncHttpClientConfig == null) {
        asyncHttpClientConfig = new AsyncHttpClientConfigBean();
        asyncHttpClientConfig.setCompressionEnabled(compressionEnabled);
        asyncHttpClientConfig.setConnectionTimeOutInMs(connectionTimeoutMs);
        asyncHttpClientConfig.setMaxTotalConnections(maxTotalConnections);
        asyncHttpClientConfig.setRequestTimeoutInMs(requestTimeoutInMs);
      } else {
        LOG.info("Async http client config is provided through Spring wiring; "
            + "ignoring explicit properties");
      }
      asyncHttpClient = new AsyncHttpClient(asyncHttpClientConfig);
    } else {
      LOG.info("Async http client is provided through Spring wiring; ignoring configuration");
    }
  }

  public Promise<WSResponse> postRequest(URL url, String contentType, byte[] body)
      throws IOException {
    return postRequest(url, contentType, body, null);
  }

  public Promise<WSResponse> postRequest(URL url, String contentType, byte[] body,
      Consumer<BoundRequestBuilder> preparer) throws IOException {
    final scala.concurrent.Promise<WSResponse> scalaPromise =
        scala.concurrent.Promise$.MODULE$.apply();
    BoundRequestBuilder builder = asyncHttpClient.preparePost(url.toString())
        .setHeader("Content-Type", contentType)
        .setContentLength(body.length)
        .setBody(body);
    if (preparer != null) {
      preparer.accept(builder);
    }
    builder.execute(new AsyncCompletionHandler<Response>() {
      @Override
      public Response onCompleted(Response response) {
        scalaPromise.success(new NingWSResponse(response));
        return response;
      }
      @Override
      public void onThrowable(Throwable t) {
        scalaPromise.failure(t);
      }
    });
    return Promise.wrap(scalaPromise.future());
  }
}
