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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import me.tfeng.play.plugins.HttpPlugin;
import play.libs.F.Promise;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncHttpTransceiver extends HttpTransceiver {

  private Promise<WSResponse> promise;
  private int timeout;
  private final URL url;

  public AsyncHttpTransceiver(URL url) {
    super(url);
    this.url = url;
  }

  public Promise<List<ByteBuffer>> asyncReadBuffers() throws IOException {
    return promise.map(response -> {
      InputStream stream = response.getBodyAsStream();
      return readBuffers(stream);
    });
  }

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

  @Override
  public void setTimeout(int timeout) {
    super.setTimeout(timeout);
    this.timeout = timeout;
  }

  @Override
  public synchronized void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writeBuffers(buffers, outputStream);
    promise = HttpPlugin.getInstance().postRequest(url, CONTENT_TYPE, outputStream.toByteArray());
  }
}
