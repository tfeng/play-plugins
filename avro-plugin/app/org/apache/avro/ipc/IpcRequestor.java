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
import java.util.List;

import me.tfeng.play.http.PostRequestPreparer;

import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.specific.SpecificData;

import play.libs.F.Promise;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IpcRequestor extends SpecificRequestor {

  class AsyncRequest extends Requestor.Request {

    public AsyncRequest(String messageName, Object request, RPCContext context) {
      super(messageName, request, context);
    }
  }

  public static final AuthTokenPreservingPostRequestPreparer
      AUTH_TOKEN_PRESERVING_POST_REQUEST_PREPARER =
          new AuthTokenPreservingPostRequestPreparer();

  private PostRequestPreparer postRequestPreparer = AUTH_TOKEN_PRESERVING_POST_REQUEST_PREPARER;

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
      return transceiver.asyncTransceive(asyncRequest.getBytes(), postRequestPreparer).map(
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
      List<ByteBuffer> response =
          transceiver.transceive(asyncRequest.getBytes(), postRequestPreparer);
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

  public void setPostRequestPreparer(PostRequestPreparer postRequestPreparer) {
    this.postRequestPreparer = postRequestPreparer;
  }
}
