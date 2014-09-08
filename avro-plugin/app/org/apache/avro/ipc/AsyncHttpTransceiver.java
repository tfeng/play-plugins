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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

import me.tfeng.play.plugins.HttpPlugin;

import org.apache.avro.AvroRemoteException;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http.Request;

import com.ning.http.client.AsyncHttpClient;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncHttpTransceiver extends HttpTransceiver implements AsyncTransceiver {

  public static final String[] DEFAULT_PRESERVED_HEADERS = { "Authorization" };

  private static final ALogger LOG = Logger.of(AsyncHttpTransceiver.class);

  public static List<ByteBuffer> readBuffers(InputStream in) throws IOException {
    return HttpTransceiver.readBuffers(in);
  }

  public static void writeBuffers(List<ByteBuffer> buffers, OutputStream out) throws IOException {
    HttpTransceiver.writeBuffers(buffers, out);
  }

  private String[] preservedHeaders = DEFAULT_PRESERVED_HEADERS;

  private Promise<WSResponse> promise;
  private int timeout = HttpPlugin.getInstance().getRequestTimeout();
  private final URL url;

  public AsyncHttpTransceiver(URL url) {
    super(url);
    this.url = url;
  }

  public Promise<List<ByteBuffer>> asyncReadBuffers() throws IOException {
    return promise.map(response -> {
      try {
        int status = response.getStatus();
        if (status >= 400) {
          if (status == 404 || status == 410) {
            throw new FileNotFoundException(url.toString());
          } else {
            throw new IOException("Server returned HTTP response code: " + status + " for URL: " +
                  url.toString());
          }
        }
        InputStream stream = response.getBodyAsStream();
        return readBuffers(stream);
      } catch (Throwable t) {
        throw new AvroRemoteException(t);
      }
    });
  }

  @Override
  public Promise<List<ByteBuffer>> asyncTransceive(List<ByteBuffer> request) throws IOException {
    lockChannel();
    writeBuffers(request);
    return asyncReadBuffers().transform(
        response -> { unlockChannel(); return response; },
        throwable -> { unlockChannel(); return throwable; });
  }

  @Override
  public synchronized List<ByteBuffer> readBuffers() throws IOException {
    return asyncReadBuffers().get(timeout);
  }

  public void setPreservedHeaders(String[] preservedHeaders) {
    this.preservedHeaders = preservedHeaders;
  }

  @Override
  public void setTimeout(int timeout) {
    super.setTimeout(timeout);
    this.timeout = timeout;
  }

  @Override
  public synchronized void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writeBuffers(buffers, outputStream);
    promise = postRequest(url, outputStream.toByteArray());
  }

  protected String getContentType() {
    return CONTENT_TYPE;
  }

  protected Consumer<AsyncHttpClient.BoundRequestBuilder> getRequestPreparer(URL url, byte[] body) {
    return builder -> {
      if (preservedHeaders != null && preservedHeaders.length > 0) {
        Request request = null;
        try {
          request = Controller.request();
        } catch (RuntimeException e) {
          LOG.info("Unable to get current request; do not pass headers to downstream calls");
        }
        if (request != null) {
          for (String preservedHeader : preservedHeaders) {
            builder.setHeader(preservedHeader, request.getHeader(preservedHeader));
          }
        }
      }
    };
  }

  protected Promise<WSResponse> postRequest(URL url, byte[] body) throws IOException {
    return HttpPlugin.getInstance().postRequest(url, getContentType(), body,
        getRequestPreparer(url, body));
  }
}
