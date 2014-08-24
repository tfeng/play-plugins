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

package me.tfeng.play.avro.d2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.avro.ipc.InternalHttpTransceiver;
import org.apache.avro.ipc.Transceiver;

import play.mvc.Controller;
import play.mvc.Http.Request;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Transceiver extends Transceiver {

  public static final String[] PRESERVED_HEADERS = { "Authorization" };

  private final AvroD2Client client;

  private final InternalHttpTransceiver transceiver;

  public AvroD2Transceiver(AvroD2Client client) {
    this.client = client;
    transceiver = new InternalHttpTransceiver(client.getNextServerUrl());
  }

  @Override
  public String getRemoteName() throws IOException {
    return AvroD2Helper.getUri(client.getProtocol()).toString();
  }

  @Override
  public List<ByteBuffer> readBuffers() throws IOException {
    return transceiver.readBuffers();
  }

  @Override
  public void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    transceiver.writeBuffers(buffers, connection -> {
      Request request = Controller.request();
      for (String preservedHeader : PRESERVED_HEADERS) {
        connection.setRequestProperty(preservedHeader, request.getHeader(preservedHeader));
      }
    });
  }
}