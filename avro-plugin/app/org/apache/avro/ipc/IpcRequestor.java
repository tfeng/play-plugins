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

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import me.tfeng.play.avro.AsyncTransceiver;
import me.tfeng.play.avro.AuthTokenPreservingPostRequestPreparer;
import me.tfeng.play.avro.PostRequestPreparerChain;
import me.tfeng.play.http.PostRequestPreparer;
import me.tfeng.play.plugins.HttpPlugin;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Request;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IpcRequestor extends SpecificRequestor {

  class AsyncRequest extends Requestor.Request {

    public AsyncRequest(String messageName, Object request, RPCContext context) {
      super(messageName, request, context);
    }
  }

  private static final ALogger LOG = Logger.of(IpcRequestor.class);
  private boolean isGeneric;
  private volatile PostRequestPreparerChain postRequestPreparerChain =
      new PostRequestPreparerChain();

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

  public void addPostRequestPreparer(PostRequestPreparer postRequestPreparer) {
    postRequestPreparerChain.add(postRequestPreparer);
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

  public boolean isGeneric() {
    return isGeneric;
  }

  public void removePostRequestPreparer(PostRequestPreparer postRequestPreparer) {
    postRequestPreparerChain.remove(postRequestPreparer);
  }

  public Promise<Object> request(String message, Object[] args) throws Exception {
    AsyncTransceiver transceiver = (AsyncTransceiver) getTransceiver();
    AsyncRequest asyncRequest = new AsyncRequest(message, args, new RPCContext());
    CallFuture<Object> callFuture =
        asyncRequest.getMessage().isOneWay() ? null : new CallFuture<>();
    TransceiverCallback<Object> transceiverCallback =
        new TransceiverCallback<>(asyncRequest, callFuture);

    PostRequestPreparer postRequestPreparer = null;
    Request controllerRequest = null;
    try {
      controllerRequest = Controller.request();
    } catch (RuntimeException e) {
      LOG.info("Unable to get current request; do not pass headers to downstream calls");
      postRequestPreparer = postRequestPreparerChain;
    }
    if (controllerRequest != null) {
      postRequestPreparer = new PostRequestPreparerChain(
          new AuthTokenPreservingPostRequestPreparer(controllerRequest), postRequestPreparerChain);
    }

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
  }

  public void setGeneric(boolean isGeneric) {
    this.isGeneric = isGeneric;
  }

  @Override
  protected DatumReader<Object> getDatumReader(Schema writer, Schema reader) {
    if (isGeneric) {
      return new GenericDatumReader<>(writer, reader);
    } else {
      return new SpecificDatumReader<>(writer, reader, getSpecificData());
    }
  }

  @Override
  protected DatumWriter<Object> getDatumWriter(Schema schema) {
    if (isGeneric) {
      return new GenericDatumWriter<>(schema);
    } else {
      return new SpecificDatumWriter<>(schema, getSpecificData());
    }
  }
}
