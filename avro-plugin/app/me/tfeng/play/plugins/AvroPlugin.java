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

import me.tfeng.play.avro.AvroHelper;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.apache.avro.ipc.IpcRequestor;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import play.Application;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroPlugin extends AbstractPlugin {

  public static AvroPlugin getInstance() {
    return Play.application().plugin(AvroPlugin.class);
  }

  private Map<Class<?>, Object> protocolImplementations;

  public AvroPlugin(Application application) {
    super(application);
  }

  public <T> T client(Class<T> interfaceClass, URL url) {
    return client(interfaceClass, url, new SpecificData(interfaceClass.getClassLoader()));
  }

  @SuppressWarnings("unchecked")
  public <T> T client(Class<T> interfaceClass, URL url, SpecificData data) {
    try {
      Protocol protocol = AvroHelper.getProtocol(interfaceClass);
      return (T) Proxy.newProxyInstance(data.getClassLoader(), new Class[] { interfaceClass },
          new IpcRequestor(protocol, new AsyncHttpTransceiver(url), data));
    } catch (IOException e) {
      throw new RuntimeException("Unable to create async client", e);
    }
  }

  public Map<Class<?>, Object> getProtocolImplementations() {
    return protocolImplementations;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart() {
    super.onStart();

    try {
      protocolImplementations =
          getApplicationContext().getBean("avro-plugin.protocol-implementations", Map.class);
    } catch (NoSuchBeanDefinitionException e) {
      protocolImplementations = Collections.emptyMap();
    }
  }
}
