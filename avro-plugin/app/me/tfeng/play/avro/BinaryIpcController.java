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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import me.tfeng.play.plugins.AvroPlugin;

import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.apache.avro.ipc.IpcResponder;
import org.apache.http.entity.ContentType;

import play.Play;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class BinaryIpcController extends Controller {

  public static final String CONTENT_TYPE = "avro/binary";

  @BodyParser.Of(BodyParser.Raw.class)
  public static Result post(String protocol) throws Throwable {
    String contentTypeHeader = request().getHeader("content-type");
    ContentType contentType = ContentType.parse(contentTypeHeader);
    if (!CONTENT_TYPE.equals(contentType.getMimeType())) {
      throw new RuntimeException("Unable to handle content-type " + contentType + "; "
          + CONTENT_TYPE + " is expected");
    }

    AvroPlugin plugin = AvroPlugin.getInstance();
    Class<?> protocolClass = Play.application().classloader().loadClass(protocol);
    Object implementation = plugin.getProtocolImplementations().get(protocolClass);
    byte[] bytes = request().body().asRaw().asBytes();

    List<ByteBuffer> buffers = AsyncHttpTransceiver.readBuffers(new ByteArrayInputStream(bytes));
    IpcResponder responder = new IpcResponder(protocolClass, implementation);
    List<ByteBuffer> response = responder.respond(buffers);
    Exception unexpectedError = responder.getUnexpectedError();
    if (unexpectedError != null) {
      throw unexpectedError;
    }

    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    try {
      AsyncHttpTransceiver.writeBuffers(response, outStream);
    } finally {
      outStream.close();
    }
    return Results.ok(outStream.toByteArray());
  }
}
