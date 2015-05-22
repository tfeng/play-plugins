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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.avro.Protocol;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.ipc.HandshakeMatch;
import org.apache.avro.ipc.HandshakeRequest;
import org.apache.avro.ipc.HandshakeResponse;
import org.apache.avro.ipc.MD5;
import org.apache.avro.ipc.RPCContext;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.zookeeper.KeeperException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import me.tfeng.play.avro.ProtocolVersionResolver;
import me.tfeng.play.plugins.AvroD2Plugin;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2ProtocolVersionResolver implements ProtocolVersionResolver {

  private static final ALogger LOG = Logger.of(AvroD2ProtocolVersionResolver.class);

  private SpecificDatumReader<HandshakeRequest> handshakeReader =
      new SpecificDatumReader<>(HandshakeRequest.class);

  private SpecificDatumWriter<HandshakeResponse> handshakeWriter =
      new SpecificDatumWriter<>(HandshakeResponse.class);

  private Map<List<String>, Protocol> protocolCache = Maps.newHashMap();

  @Override
  public Protocol resolve(Responder responder, Decoder in, Encoder out, Transceiver connection)
      throws IOException {
    Protocol serverProtocol = responder.getLocal();
    byte[] serverMD5 = serverProtocol.getMD5();
    String namespace = serverProtocol.getNamespace();
    String name = serverProtocol.getName();
    HandshakeRequest request = handshakeReader.read(null, in);
    MD5 clientHash = request.getClientHash();
    HandshakeResponse response = new HandshakeResponse();
    Protocol protocol = null;
    if (clientHash == null) {
      LOG.error("Client protocol MD5 is missing from request (namespace=" + namespace + ", name=" +
          name + ")");
    } else {
      byte[] clientMD5 = clientHash.bytes();
      if (Arrays.equals(clientMD5, serverMD5)) {
        protocol = serverProtocol;
        response.setMatch(HandshakeMatch.BOTH);
      } else {
        String clientMD5String = DatatypeConverter.printHexBinary(clientMD5);
        protocol = getProtocol(namespace, name, clientMD5String);
        if (protocol == null) {
          try {
            protocol = AvroD2Helper.readProtocolFromZk(
                AvroD2Plugin.getInstance().getZooKeeper(), namespace, name, clientMD5String);
            response.setMatch(HandshakeMatch.CLIENT);
            setProtocol(namespace, name, clientMD5String, protocol);
          } catch (InterruptedException | KeeperException e) {
            LOG.error("Unable to read schema from ZooKeeper for protocol (namespace=" + namespace +
                ", name=" + name + ", MD5=" + clientMD5String + ")", e);
          }
        } else {
          response.setMatch(HandshakeMatch.CLIENT);
        }
      }
    }

    if (protocol == null) {
      response.setMatch(HandshakeMatch.NONE);
    }

    if (response.getMatch() != HandshakeMatch.BOTH) {
      response.setServerProtocol(serverProtocol.toString());
      response.setServerHash(new MD5(serverMD5));
    }

    RPCContext context = new RPCContext();
    context.setHandshakeRequest(request);
    context.setHandshakeResponse(response);
    handshakeWriter.write(response, out);

    return protocol;
  }

  private synchronized Protocol getProtocol(String namespace, String name, String md5) {
    return protocolCache.get(ImmutableList.of(namespace, name, md5));
  }

  private synchronized Protocol setProtocol(String namespace, String name, String md5,
      Protocol protocol) {
    return protocolCache.put(ImmutableList.of(namespace, name, md5), protocol);
  }
}
