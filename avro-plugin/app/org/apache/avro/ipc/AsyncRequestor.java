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

import org.apache.avro.AvroRemoteException;
import org.apache.avro.Protocol;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.specific.SpecificData;

import play.libs.F.Promise;

import com.google.common.base.Defaults;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncRequestor extends SpecificRequestor {

  public AsyncRequestor(Class<?> iface, AsyncHttpTransceiver transceiver) throws IOException {
    super(iface, transceiver);
  }

  public AsyncRequestor(Protocol protocol, AsyncHttpTransceiver transceiver) throws IOException {
    super(protocol, transceiver);
  }

  public AsyncRequestor(Class<?> iface, AsyncHttpTransceiver transceiver, SpecificData data)
      throws IOException {
    super(iface, transceiver, data);
  }

  public AsyncRequestor(Protocol protocol, AsyncHttpTransceiver transceiver, SpecificData data)
      throws IOException {
    super(protocol, transceiver, data);
  }

  private Promise<Object> promise;

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (PromiseHolder.class.isAssignableFrom(method.getDeclaringClass())
        && method.getName().equals("getPromise")) {
      return promise;
    }

    AsyncRequest asyncRequest = new AsyncRequest(method.getName(), args, new RPCContext());
    AsyncHttpTransceiver transceiver = (AsyncHttpTransceiver) getTransceiver();
    CallFuture<Object> callFuture =
        asyncRequest.getMessage().isOneWay() ? null : new CallFuture<Object>();
    TransceiverCallback<Object> transceiverCallback =
        new TransceiverCallback<Object>(asyncRequest, callFuture);
    promise = transceiver.asyncTransceive(asyncRequest.getBytes()).map(response -> {
      transceiverCallback.handleResult(response);
      return callFuture.get();
    });
    return Defaults.defaultValue(method.getReturnType());
  }

  @Override
  public <T> void request(String messageName, Object request, Callback<T> callback)
      throws Exception {
    AsyncRequest asyncRequest = new AsyncRequest(messageName, request, new RPCContext());
    AsyncHttpTransceiver t = (AsyncHttpTransceiver) getTransceiver();
    CallFuture<T> callFuture = new CallFuture<T>(callback);
    t.transceive(asyncRequest.getBytes(), new TransceiverCallback<T>(asyncRequest, callFuture));
    // Block until handshake complete
    callFuture.await();
    if (asyncRequest.getMessage().isOneWay()) {
      Throwable error = callFuture.getError();
      if (error != null) {
        if (error instanceof Exception) {
          throw (Exception) error;
        } else {
          throw new AvroRemoteException(error);
        }
      }
    }
  }

  class AsyncRequest extends Requestor.Request {

    public AsyncRequest(String messageName, Object request, RPCContext context) {
      super(messageName, request, context);
    }
  }
}
