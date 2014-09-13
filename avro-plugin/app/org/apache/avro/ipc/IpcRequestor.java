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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.specific.SpecificData;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Request;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IpcRequestor extends SpecificRequestor {

  public static class RequestHeadersSupplier implements Supplier<Map<String, String>> {

    @Override
    public Map<String, String> get() {
      Request currentRequest = null;
      try {
        currentRequest = Controller.request();
      } catch (RuntimeException e) {
        LOG.info("Unable to get current request; do not pass headers to downstream calls");
      }

      if (currentRequest == null) {
        return Collections.emptyMap();
      } else {
        Map<String, String> headers = new HashMap<>(DEFAULT_PRESERVED_HEADERS.length);
        for (String preservedHeader : DEFAULT_PRESERVED_HEADERS) {
          headers.put(preservedHeader, currentRequest.getHeader(preservedHeader));
        }
        return headers;
      }
    }
  }

  class AsyncRequest extends Requestor.Request {

    public AsyncRequest(String messageName, Object request, RPCContext context) {
      super(messageName, request, context);
    }
  }

  public static final RequestHeadersSupplier DEFAULT_HEADERS_SUPPLIER =
      new RequestHeadersSupplier();

  public static final String[] DEFAULT_PRESERVED_HEADERS = { "Authorization" };

  private static final ALogger LOG = Logger.of(IpcRequestor.class);

  public IpcRequestor(Class<?> iface, AsyncTransceiver transceiver) throws IOException {
    super(iface, (Transceiver) transceiver);
  }

  public IpcRequestor(Class<?> iface, AsyncTransceiver transceiver, SpecificData data)
      throws IOException {
    super(iface, (Transceiver) transceiver, data);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    AsyncTransceiver transceiver = (AsyncTransceiver) getTransceiver();
    AsyncRequest asyncRequest = new AsyncRequest(method.getName(), args, new RPCContext());
    CallFuture<Object> callFuture =
        asyncRequest.getMessage().isOneWay() ? null : new CallFuture<Object>();
    TransceiverCallback<Object> transceiverCallback =
        new TransceiverCallback<Object>(asyncRequest, callFuture);
    if (Promise.class.isAssignableFrom(method.getReturnType())) {
      return transceiver.asyncTransceive(asyncRequest.getBytes()).map(
          response -> {
            transceiverCallback.handleResult(response);
            if (callFuture == null) {
              return null;
            } else if (callFuture.getError() == null) {
              return callFuture.getResult();
            } else {
              throw callFuture.getError();
            }
          });
    } else {
      List<ByteBuffer> response = transceiver.transceive(asyncRequest.getBytes());
      transceiverCallback.handleResult(response);
      if (callFuture == null) {
        return null;
      } else if (callFuture.getError() == null) {
        return callFuture.getResult();
      } else {
        throw callFuture.getError();
      }
    }
  }

  public void setHeadersSupplier(Supplier<Map<String, String>> headersSupplier) {
    ((AsyncTransceiver) getTransceiver()).setHeadersSupplier(
        headersSupplier == null ? DEFAULT_HEADERS_SUPPLIER: headersSupplier);
  }
}
