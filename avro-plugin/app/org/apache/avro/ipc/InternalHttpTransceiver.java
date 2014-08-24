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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

/**
 * This is a custom HTTP transceiver based on {@link HttpTransceiver}. It allows user to prepare a
 * connection before the connection is used to send data to the server. An example usage of this
 * functionality is to set an "Authorization" header to the connection.
 *
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class InternalHttpTransceiver extends HttpTransceiver {

  public static List<ByteBuffer> readBuffers(InputStream in) throws IOException {
    return HttpTransceiver.readBuffers(in);
  }

  public static void writeBuffers(List<ByteBuffer> buffers, OutputStream out) throws IOException {
    HttpTransceiver.writeBuffers(buffers, out);
  }

  private Consumer<HttpURLConnection> defaultConnectionPrepairer;

  private Proxy proxy;

  private int timeout;

  private URL url;

  protected HttpURLConnection connection;

  public InternalHttpTransceiver(URL url) {
    super(url);
    this.url = url;
  }

  public InternalHttpTransceiver(URL url, Consumer<HttpURLConnection> defaultConnectionPrepairer) {
    this(url);
    this.defaultConnectionPrepairer = defaultConnectionPrepairer;
  }

  public InternalHttpTransceiver(URL url, Proxy proxy) {
    super(url, proxy);
    this.url = url;
    this.proxy = proxy;
  }

  public InternalHttpTransceiver(URL url, Proxy proxy, Consumer<HttpURLConnection> defaultConnectionPrepairer) {
    this(url, proxy);
    this.defaultConnectionPrepairer = defaultConnectionPrepairer;
  }

  @Override
  public synchronized List<ByteBuffer> readBuffers() throws IOException {
    InputStream in = connection.getInputStream();
    try {
      return readBuffers(in);
    } finally {
      in.close();
    }
  }

  @Override
  public void setTimeout(int timeout) {
    super.setTimeout(timeout);
    this.timeout = timeout;
  }

  @Override
  public synchronized void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    writeBuffers(buffers, (Consumer<HttpURLConnection>) null);
  }

  public synchronized void writeBuffers(List<ByteBuffer> buffers,
      Consumer<HttpURLConnection> connectionPrepairer) throws IOException {
    if (proxy == null) {
      connection = (HttpURLConnection)url.openConnection();
    } else {
      connection = (HttpURLConnection)url.openConnection(proxy);
    }
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", CONTENT_TYPE);
    connection.setRequestProperty("Content-Length", Integer.toString(getLength(buffers)));
    connection.setDoOutput(true);
    connection.setReadTimeout(timeout);
    connection.setConnectTimeout(timeout);

    if (connectionPrepairer != null) {
      connectionPrepairer.accept(connection);
    }

    if (defaultConnectionPrepairer != null) {
      defaultConnectionPrepairer.accept(connection);
    }

    OutputStream out = connection.getOutputStream();
    try {
      writeBuffers(buffers, out);
    } finally {
      out.close();
    }
  }
}
