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
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.ByteBufferInputStream;

import me.tfeng.play.avro.AsyncTransceiver;
import me.tfeng.play.avro.AuthTokenPreservingIpcRequestPreparer;
import me.tfeng.play.avro.IpcRequestPreparerChain;
import me.tfeng.play.avro.IpcResponseProcessor;
import me.tfeng.play.http.IpcRequestPreparer;
import me.tfeng.play.plugins.HttpPlugin;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IpcRequestor extends SpecificRequestor implements IpcResponseProcessor {

  public class Request extends Requestor.Request {

    public Request(String messageName, Object request, RPCContext context) {
      super(messageName, request, context);
    }
  }

  public static final SpecificDatumReader<HandshakeResponse> HANDSHAKE_RESPONSE_READER =
      new SpecificDatumReader<>(HandshakeResponse.class);

  public static final GenericDatumReader<Map<String,ByteBuffer>> META_READER =
      new GenericDatumReader<>(Schema.createMap(Schema.create(Schema.Type.BYTES)));

  private static final ALogger LOG = Logger.of(IpcRequestor.class);

  public static void setErrorInRPCContext(RPCContext context, Exception error) {
    context.setError(error);
  }

  public static void setResponseCallMetaInRPCContext(RPCContext context,
      Map<String,ByteBuffer> meta) {
    context.setRequestCallMeta(meta);
  }

  public static void setResponseInRPCContext(RPCContext context, Object response) {
    context.setResponse(response);
  }

  private volatile IpcRequestPreparerChain requestPreparerChain = new IpcRequestPreparerChain();

  private volatile IpcResponseProcessor responseProcessor = this;

  private boolean useGenericRecord;

  public IpcRequestor(Class<?> iface, AsyncTransceiver transceiver) throws IOException {
    super(iface, (Transceiver) transceiver);
  }

  public IpcRequestor(Class<?> iface, AsyncTransceiver transceiver, SpecificData data)
      throws IOException {
    super(iface, (Transceiver) transceiver, data);
  }

  public IpcRequestor(Protocol protocol, AsyncTransceiver transceiver) throws IOException {
    super(protocol, (Transceiver) transceiver);
  }

  public IpcRequestor(Protocol protocol, AsyncTransceiver transceiver, SpecificData data)
      throws IOException {
    super(protocol, (Transceiver) transceiver, data);
  }

  public void addRequestPreparer(IpcRequestPreparer postRequestPreparer) {
    requestPreparerChain.add(postRequestPreparer);
  }

  @Override
  public DatumReader<Object> getDatumReader(Schema writer, Schema reader) {
    if (useGenericRecord) {
      return new GenericDatumReader<>(writer, reader);
    } else {
      return new SpecificDatumReader<>(writer, reader, getSpecificData());
    }
  }

  @Override
  public DatumWriter<Object> getDatumWriter(Schema schema) {
    if (useGenericRecord) {
      return new GenericDatumWriter<>(schema);
    } else {
      return new SpecificDatumWriter<>(schema, getSpecificData());
    }
  }

  public List<RPCPlugin> getRPCPlugins() {
    return Collections.unmodifiableList(rpcMetaPlugins);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Promise<Object> promise = request(method.getName(), args);
    if (Promise.class.isAssignableFrom(method.getReturnType())) {
      return promise;
    } else {
      int timeout = HttpPlugin.getInstance().getRequestTimeout();
      return promise.get(timeout);
    }
  }

  @Override
  public Object process(IpcRequestor requestor, IpcRequestor.Request request, String message,
      List<ByteBuffer> response) throws Exception {
    ByteBufferInputStream bbi = new ByteBufferInputStream(response);
    BinaryDecoder in = DecoderFactory.get().binaryDecoder(bbi, null);
    HandshakeResponse handshake = HANDSHAKE_RESPONSE_READER.read(null, in);
    Protocol localProtocol = requestor.getLocal();
    Protocol serverProtocol;
    if (handshake.getMatch() == HandshakeMatch.BOTH) {
      serverProtocol = localProtocol;
    } else {
      serverProtocol = Protocol.parse(handshake.getServerProtocol());
    }

    RPCContext context = request.getContext();
    setResponseCallMetaInRPCContext(context, META_READER.read(null, in));

    if (!in.readBoolean()) {
      Schema localSchema = localProtocol.getMessages().get(message).getResponse();
      Schema remoteSchema = serverProtocol.getMessages().get(message).getResponse();
      Object responseObject = new SpecificDatumReader<>(remoteSchema, localSchema).read(null, in);
      setResponseInRPCContext(context, responseObject);
      requestor.getRPCPlugins().forEach(plugin -> plugin.clientReceiveResponse(context));
      return responseObject;
    } else {
      Schema localSchema = localProtocol.getMessages().get(message).getErrors();
      Schema remoteSchema = serverProtocol.getMessages().get(message).getErrors();
      Object error = new SpecificDatumReader<>(remoteSchema, localSchema).read(null, in);
      Exception exception;
      if (error instanceof Exception) {
        exception = (Exception) error;
      } else {
        exception = new AvroRuntimeException(error.toString());
      }
      setErrorInRPCContext(context, exception);
      requestor.getRPCPlugins().forEach(plugin -> plugin.clientReceiveResponse(context));
      throw exception;
    }
  }

  public void removeRequestPreparer(IpcRequestPreparer requestPreparer) {
    requestPreparerChain.remove(requestPreparer);
  }

  public Promise<Object> request(String message, Object[] args) throws Exception {
    AsyncTransceiver transceiver = (AsyncTransceiver) getTransceiver();
    Request ipcRequest = new Request(message, args, new RPCContext());
    CallFuture<Object> callFuture = ipcRequest.getMessage().isOneWay() ? null : new CallFuture<>();
    IpcRequestPreparer postRequestPreparer = null;
    Http.Request controllerRequest = null;
    try {
      controllerRequest = Controller.request();
    } catch (RuntimeException e) {
      LOG.info("Unable to get current request; do not pass headers to downstream calls");
      postRequestPreparer = requestPreparerChain;
    }
    if (controllerRequest != null) {
      postRequestPreparer = new IpcRequestPreparerChain(
          new AuthTokenPreservingIpcRequestPreparer(controllerRequest), requestPreparerChain);
    }

    return transceiver.asyncTransceive(ipcRequest.getBytes(), postRequestPreparer).map(
        response -> {
          Object responseObject;
          try {
            responseObject = responseProcessor.process(this, ipcRequest, message, response);
            if (callFuture != null) {
              callFuture.handleResult(responseObject);
            }
          } catch (Exception e) {
            if (callFuture != null) {
              callFuture.handleError(e);
            }
          }

          // transceiverCallback.handleResult(response);
          if (callFuture == null) {
            return null;
          } else if (callFuture.getError() == null) {
            return callFuture.getResult();
          } else {
            throw callFuture.getError();
          }
        });
  }

  public void setResponseProcessor(IpcResponseProcessor processor) {
    responseProcessor = processor;
  }

  public void setUseGenericRecord(boolean useGenericRecord) {
    this.useGenericRecord = useGenericRecord;
  }

  public boolean useGenericRecord() {
    return useGenericRecord;
  }
}
