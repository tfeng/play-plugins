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

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.avro.util.ByteBufferOutputStream;

import play.libs.F.Promise;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncResponder extends SpecificResponder {

  private static final Method HANDSHAKE_METHOD;

  private static final Schema META = Schema.createMap(Schema.create(Schema.Type.BYTES));

  private static final GenericDatumReader<Map<String,ByteBuffer>> META_READER =
      new GenericDatumReader<Map<String,ByteBuffer>>(META);

  private static final GenericDatumWriter<Map<String,ByteBuffer>> META_WRITER =
      new GenericDatumWriter<Map<String,ByteBuffer>>(META);

  static {
    try {
      HANDSHAKE_METHOD = Responder.class.getDeclaredMethod("handshake", Decoder.class,
          Encoder.class, Transceiver.class);
      HANDSHAKE_METHOD.setAccessible(true);
    } catch (NoSuchMethodException | SecurityException e) {
      throw new RuntimeException("Unable to get handshake method", e);
    }
  }

  public AsyncResponder(Class<?> iface, Object impl) {
    super(iface, impl);
  }

  public AsyncResponder(Class<?> iface, Object impl, SpecificData data) {
    super(iface, impl, data);
  }

  public AsyncResponder(Protocol protocol, Object impl) {
    super(protocol, impl);
  }

  public AsyncResponder(Protocol protocol, Object impl, SpecificData data) {
    super(protocol, impl, data);
  }

  public Promise<List<ByteBuffer>> asyncRespond(List<ByteBuffer> buffers) throws Exception {
    Decoder in = DecoderFactory.get().binaryDecoder(new ByteBufferInputStream(buffers), null);
    ByteBufferOutputStream bbo = new ByteBufferOutputStream();
    BinaryEncoder out = EncoderFactory.get().binaryEncoder(bbo, null);
    RPCContext context = new RPCContext();
    List<ByteBuffer> payload = null;
    List<ByteBuffer> handshake = null;
    Protocol remote = (Protocol) HANDSHAKE_METHOD.invoke(this, in, out, null);
    out.flush();
    if (remote == null) {
      // handshake failed
      return Promise.pure(bbo.getBufferList());
    }
    handshake = bbo.getBufferList();

    // read request using remote protocol specification
    context.setRequestCallMeta(META_READER.read(null, in));
    String messageName = in.readString(null).toString();
    if (messageName.equals("")) {
      // a handshake ping
      return Promise.pure(handshake);
    }
    Message rm = remote.getMessages().get(messageName);
    if (rm == null) {
      throw new AvroRuntimeException("No such remote message: " + messageName);
    }
    Message m = getLocal().getMessages().get(messageName);
    if (m == null) {
      throw new AvroRuntimeException("No message named " + messageName + " in " + getLocal());
    }

    Object request = readRequest(rm.getRequest(), m.getRequest(), in);

    context.setMessage(rm);
    for (RPCPlugin plugin : rpcMetaPlugins) {
      plugin.serverReceiveRequest(context);
    }

    Object response = null;
    try {
      response = respond(m, request);
      if (response instanceof Promise) {
        List<ByteBuffer> handshakeFinal = handshake;
        Object responseFinal = response;
        return ((Promise<?>) response)
            .map(result -> {
              context.setResponse(result);
              processResult(bbo, out, context, m, payload, handshakeFinal, responseFinal, null);
              return bbo.getBufferList();
            })
            .recover(e -> {
              if (e instanceof Exception) {
                context.setError((Exception) e);
                processResult(bbo, out, context, m, payload, handshakeFinal, responseFinal,
                    (Exception) e);
                return bbo.getBufferList();
              } else {
                throw e;
              }
            });
      } else {
        context.setResponse(response);
        processResult(bbo, out, context, m, payload, handshake, response, null);
        return Promise.pure(bbo.getBufferList());
      }
    } catch (Exception e) {
      context.setError(e);
      processResult(bbo, out, context, m, payload, handshake, response, e);
      return Promise.pure(bbo.getBufferList());
    }
  }

  private void processResult(ByteBufferOutputStream bbo, BinaryEncoder out, RPCContext context,
      Message m, List<ByteBuffer> payload, List<ByteBuffer> handshake, Object response,
      Exception error) throws Exception {
    out.writeBoolean(error != null);
    if (error == null) {
      writeResponse(m.getResponse(), response, out);
    } else {
      try {
        writeError(m.getErrors(), error, out);
      } catch (AvroRuntimeException e) {
        throw error;
      }
    }
    out.flush();
    payload = bbo.getBufferList();

    // Grab meta-data from plugins
    context.setResponsePayload(payload);
    for (RPCPlugin plugin : rpcMetaPlugins) {
      plugin.serverSendResponse(context);
    }
    META_WRITER.write(context.responseCallMeta(), out);
    out.flush();
    // Prepend handshake and append payload
    bbo.prepend(handshake);
    bbo.append(payload);
  }
}
