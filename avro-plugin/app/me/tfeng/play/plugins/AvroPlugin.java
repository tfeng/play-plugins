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

package me.tfeng.play.plugins;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import me.tfeng.play.http.PostRequestPreparer;

import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.apache.avro.ipc.AsyncTransceiver;
import org.apache.avro.ipc.IpcRequestor;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;

import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.Akka;
import scala.concurrent.ExecutionContext;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroPlugin extends AbstractPlugin {

  private static final ALogger LOG = Logger.of(AvroPlugin.class);

  public static <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver) {
    return client(interfaceClass, transceiver, new SpecificData(interfaceClass.getClassLoader()));
  }

  public static <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver,
      SpecificData data) {
    return client(interfaceClass, transceiver, data, null);
  }

  @SuppressWarnings("unchecked")
  public static <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver,
      SpecificData data, PostRequestPreparer postRequestPreparer) {
    try {
      IpcRequestor requestor = new IpcRequestor(interfaceClass, transceiver, data);
      if (postRequestPreparer != null) {
        requestor.setPostRequestPreparer(postRequestPreparer);
      }
      return (T) Proxy.newProxyInstance(data.getClassLoader(), new Class[] { interfaceClass },
          requestor);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create async client", e);
    }
  }

  public static <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver,
      PostRequestPreparer postRequestPreparer) {
    return client(interfaceClass, transceiver, new SpecificData(interfaceClass.getClassLoader()),
        postRequestPreparer);
  }

  public static <T> T client(Class<T> interfaceClass, URL url) {
    return client(interfaceClass, new AsyncHttpTransceiver(url));
  }

  public static <T> T client(Class<T> interfaceClass, URL url, SpecificData data) {
    return client(interfaceClass, new AsyncHttpTransceiver(url), data);
  }

  public static <T> T client(Class<T> interfaceClass, URL url, SpecificData data,
      PostRequestPreparer postRequestPreparer) {
    return client(interfaceClass, new AsyncHttpTransceiver(url), data, postRequestPreparer);
  }

  public static <T> T client(Class<T> interfaceClass, URL url,
      PostRequestPreparer postRequestPreparer) {
    return client(interfaceClass, new AsyncHttpTransceiver(url), postRequestPreparer);
  }

  public static AvroPlugin getInstance() {
    return Play.application().plugin(AvroPlugin.class);
  }

  private ExecutionContext executionContext;

  @Value("${avro-plugin.execution-context:play.akka.actor.default-dispatcher}")
  private String executionContextId;

  private Map<Class<?>, Object> protocolImplementations;

  public AvroPlugin(Application application) {
    super(application);
  }

  public ExecutionContext getExecutionContext() {
    return executionContext;
  }

  public Map<Class<?>, Object> getProtocolImplementations() {
    return protocolImplementations;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart() {
    super.onStart();

    try {
      executionContext = Akka.system().dispatchers().lookup(executionContextId);
    } catch (Exception e) {
      LOG.warn("Unable to obtain execution context " + executionContextId + "; using default", e);
      executionContext = Akka.system().dispatchers().defaultGlobalDispatcher();
    }

    try {
      protocolImplementations =
          getApplicationContext().getBean("avro-plugin.protocol-implementations", Map.class);
    } catch (NoSuchBeanDefinitionException e) {
      protocolImplementations = Collections.emptyMap();
    }
  }
}
