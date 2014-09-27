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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import me.tfeng.play.plugins.AvroPlugin;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.HandshakeResponse;
import org.apache.avro.ipc.IpcResponder;
import org.apache.avro.ipc.RPCContext;
import org.apache.avro.ipc.Requestor;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.generic.GenericRequestor;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.http.entity.ContentType;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class JsonIpcController extends Controller {

  private static class EmptyTransceiver extends Transceiver {

    @Override
    public String getRemoteName() throws IOException {
      return "localhost";
    }

    @Override
    public List<ByteBuffer> readBuffers() throws IOException {
      return Collections.emptyList();
    }

    @Override
    public void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    }
  }

  public static final String CONTENT_TYPE = "avro/json";

  private static final Transceiver EMPTY_TRANSCEIVER = new EmptyTransceiver();

  private static final SpecificDatumReader<HandshakeResponse> HANDSHAKE_READER =
      new SpecificDatumReader<HandshakeResponse>(HandshakeResponse.class);

  private static final Constructor<?> REQUEST_CONSTRUCTOR;

  private static final Method REQUEST_GETBYTES_METHOD;

  private static final Constructor<?> RESPONSE_CONSTRUCTOR;

  private static final Method RESPONSE_GETRESPONSE_METHOD;

  private static final Charset UTF8 = Charset.forName("utf-8");

  static {
    try {
      Class<?> requestClass =
          Requestor.class.getClassLoader().loadClass("org.apache.avro.ipc.Requestor$Request");
      Class<?> responseClass =
          Requestor.class.getClassLoader().loadClass("org.apache.avro.ipc.Requestor$Response");
      Constructor<?> requestConstructor = requestClass.getConstructor(Requestor.class, String.class,
          Object.class, RPCContext.class);
      requestConstructor.setAccessible(true);
      Constructor<?> responseConstructor = responseClass.getConstructor(Requestor.class,
          requestClass, BinaryDecoder.class);
      responseConstructor.setAccessible(true);
      Method requestGetBytesMethod = requestClass.getMethod("getBytes");
      requestGetBytesMethod.setAccessible(true);
      Method responseGetResponseMethod = responseClass.getMethod("getResponse");
      responseGetResponseMethod.setAccessible(true);

      REQUEST_CONSTRUCTOR = requestConstructor;
      RESPONSE_CONSTRUCTOR = responseConstructor;
      REQUEST_GETBYTES_METHOD = requestGetBytesMethod;
      RESPONSE_GETRESPONSE_METHOD = responseGetResponseMethod;
    } catch (Exception e) {
      throw new RuntimeException("Unable to initialize", e);
    }
  }

  @BodyParser.Of(BodyParser.Raw.class)
  public static Result post(String message, String protocol) throws Throwable {
    String contentTypeHeader = request().getHeader("content-type");
    ContentType contentType = ContentType.parse(contentTypeHeader);
    if (!CONTENT_TYPE.equals(contentType.getMimeType())) {
      throw new RuntimeException("Unable to handle content-type " + contentType + "; "
          + CONTENT_TYPE + " is expected");
    }

    AvroPlugin plugin = AvroPlugin.getInstance();

    Class<?> protocolClass = Play.application().classloader().loadClass(protocol);
    Object implementation = plugin.getProtocolImplementations().get(protocolClass);
    Protocol avroProtocol = AvroHelper.getProtocol(protocolClass);
    GenericRequestor requestor = new GenericRequestor(avroProtocol, EMPTY_TRANSCEIVER);
    byte[] bytes = request().body().asRaw().asBytes();
    Object request = getRequest(requestor, avroProtocol, message, bytes);

    try {
      List<ByteBuffer> buffers = convertToBuffers(request);
      IpcResponder responder = new IpcResponder(protocolClass, implementation);
      List<ByteBuffer> responseBuffers = responder.respond(buffers);
      Exception unexpectedError = responder.getUnexpectedError();
      if (unexpectedError != null) {
        throw unexpectedError;
      }

      Object response = getResponse(requestor, request, responseBuffers);
      return Results.ok(
          AvroHelper.toJson(avroProtocol.getMessages().get(message).getResponse(), response));
    } catch (AvroRemoteException e) {
      // AvroRemoteException is thrown if getResponse() finds the response buffers contain an error
      // defined in the interface specification. In that case, we return the error message in the
      // HTTP response body.
      Schema schema = avroProtocol.getMessages().get(message).getErrors();
      return Results.badRequest(AvroHelper.toJson(schema, e.getValue()));
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static List<ByteBuffer> convertToBuffers(Object requestObject) throws Throwable {
    try {
      return (List) REQUEST_GETBYTES_METHOD.invoke(requestObject);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static Object getRequest(Requestor requestor, Protocol protocol, String message,
      byte[] data) throws Throwable {
    Message messageObject = protocol.getMessages().get(message);
    if (messageObject == null) {
      throw new AvroRuntimeException("No message named "+ message + " in "+ protocol);
    }
    Schema schema = messageObject.getRequest();
    if (schema.getType() == Type.RECORD && schema.getFields().isEmpty()) {
      // The method takes no argument; use empty data.
      data = "{}".getBytes(UTF8);
    }
    JsonNode node = Json.parse(new ByteArrayInputStream(data));
    node = AvroHelper.convertFromSimpleRecord(schema, node);
    GenericDatumReader<Object> reader = new GenericDatumReader<Object>(schema);
    Object request = reader.read(null, DecoderFactory.get().jsonDecoder(schema, node.toString()));
    try {
      return REQUEST_CONSTRUCTOR.newInstance(requestor, message, request, new RPCContext());
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static Object getResponse(Requestor requestor, Object request, List<ByteBuffer> buffers)
      throws Throwable {
    ByteBufferInputStream bbi = new ByteBufferInputStream(buffers);
    BinaryDecoder in = DecoderFactory.get().binaryDecoder(bbi, null);
    HANDSHAKE_READER.read(null, in);
    Object responseObject = RESPONSE_CONSTRUCTOR.newInstance(requestor, request, in);
    try {
      return RESPONSE_GETRESPONSE_METHOD.invoke(responseObject);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
