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

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.entity.ContentType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.JsonNode;

import me.tfeng.play.plugins.AvroPlugin;
import me.tfeng.play.utils.Constants;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class JsonIpcController extends Controller {

  public static final String CONTENT_TYPE = "avro/json";

  private static final ALogger LOG = Logger.of(JsonIpcController.class);

  @BodyParser.Of(BodyParser.Raw.class)
  public static Promise<Result> post(String message, String protocol) throws Throwable {
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
    Message avroMessage = avroProtocol.getMessages().get(message);
    byte[] bytes = request().body().asRaw().asBytes();
    SpecificResponder responder = new SpecificResponder(protocolClass, implementation);
    Object request = getRequest(responder, avroMessage, bytes);
    if (AvroHelper.isAvroClient(protocolClass)) {
      Promise<?> promise = (Promise<?>) responder.respond(avroMessage, request);
      return promise
          .<Result>map(result -> Results.ok(AvroHelper.toJson(avroMessage.getResponse(), result)))
          .recover(e -> {
            try {
              LOG.warn("Exception thrown while processing request; returning bad request", e);
              return Results.badRequest(AvroHelper.toJson(avroMessage.getErrors(), e));
            } catch (Exception e2) {
              throw e;
            }
          });
    } else {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      return Promise.promise(() -> {
        Authentication currentAuthentication =
            SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
          Object result = responder.respond(avroMessage, request);
          return Results.ok(AvroHelper.toJson(avroMessage.getResponse(), result));
        } catch (Exception e) {
          try {
            LOG.warn("Exception thrown while processing request; returning bad request", e);
            return Results.badRequest(AvroHelper.toJson(avroMessage.getErrors(), e));
          } catch (Exception e2) {
            throw e;
          }
        } finally {
          SecurityContextHolder.getContext().setAuthentication(currentAuthentication);
        }
      }, AvroPlugin.getInstance().getExecutionContext());
    }
  }

  private static Object getRequest(Responder responder, Message message, byte[] data)
      throws IOException {
    Schema schema = message.getRequest();
    if (ArrayUtils.isEmpty(data)) {
      // The method takes no argument; use empty data.
      data = "{}".getBytes(Constants.UTF8);
    }
    JsonNode node = Json.parse(new ByteArrayInputStream(data));
    node = AvroHelper.convertFromSimpleRecord(schema, node);
    return responder.readRequest(message.getRequest(), message.getRequest(),
        DecoderFactory.get().jsonDecoder(schema, node.toString()));
  }
}
