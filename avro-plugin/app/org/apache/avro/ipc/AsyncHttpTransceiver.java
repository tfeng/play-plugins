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
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Semaphore;

import me.tfeng.play.http.PostRequestPreparer;
import me.tfeng.play.plugins.AvroPlugin;
import me.tfeng.play.plugins.HttpPlugin;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.specific.SpecificRecord;

import play.libs.F.Promise;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncHttpTransceiver extends HttpTransceiver implements AsyncTransceiver {

  public static List<ByteBuffer> readBuffers(InputStream in) throws IOException {
    return HttpTransceiver.readBuffers(in);
  }

  public static void writeBuffers(List<ByteBuffer> buffers, OutputStream out) throws IOException {
    HttpTransceiver.writeBuffers(buffers, out);
  }

  private volatile Promise<WSResponse> promise;
  private final Semaphore semaphore = new Semaphore(1);
  private int timeout = HttpPlugin.getInstance().getRequestTimeout();
  private final URL url;

  public AsyncHttpTransceiver(URL url) {
    super(url);
    this.url = url;
  }

  public Promise<List<ByteBuffer>> asyncReadBuffers() throws IOException {
    return promise.transform(
        response -> {
          try {
            int status = response.getStatus();
            if (status >= 400) {
              throw new AsyncHttpException(status, url);
            }
            InputStream stream = response.getBodyAsStream();
            return readBuffers(stream);
          } catch (Throwable t) {
            throw new AvroRuntimeException(t);
          }
        },
      throwable -> {
        if (throwable instanceof SpecificRecord || throwable instanceof RuntimeException) {
          return throwable;
        } else {
          return new AvroRuntimeException(throwable);
        }
      });
  }

  @Override
  public Promise<List<ByteBuffer>> asyncTransceive(List<ByteBuffer> request,
      PostRequestPreparer postRequestPreparer) {
    return Promise.promise(() -> {
      semaphore.acquire();
      writeBuffers(request, postRequestPreparer);
      return this;
    }, AvroPlugin.getInstance().getExecutionContext()).flatMap(transceiver -> {
      return transceiver.asyncReadBuffers().<List<ByteBuffer>>transform(
          result -> {
            transceiver.semaphore.release();
            return result;
          }, throwable -> {
            transceiver.semaphore.release();
            return throwable;
          });
    });
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
  public List<ByteBuffer> transceive(List<ByteBuffer> request,
      PostRequestPreparer postRequestPreparer) throws IOException {
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      throw new IOException("Unable to acquire lock", e);
    }
    try {
      writeBuffers(request, postRequestPreparer);
      return readBuffers();
    } finally {
      semaphore.release();
    }
  }

  @Override
  public synchronized void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    writeBuffers(buffers, (PostRequestPreparer) null);
  }

  public synchronized void writeBuffers(List<ByteBuffer> buffers,
      PostRequestPreparer postRequestPreparer) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writeBuffers(buffers, outputStream);
    promise = postRequest(url, outputStream.toByteArray(), postRequestPreparer);
  }

  protected String getContentType() {
    return CONTENT_TYPE;
  }

  protected Promise<WSResponse> postRequest(URL url, byte[] body,
      PostRequestPreparer postRequestPreparer) throws IOException {
    return HttpPlugin.getInstance().postRequest(url, getContentType(), body, postRequestPreparer);
  }
}
