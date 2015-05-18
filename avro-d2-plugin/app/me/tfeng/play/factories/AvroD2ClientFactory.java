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

package me.tfeng.play.factories;

import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.FactoryBean;

import com.google.common.base.MoreObjects;

import me.tfeng.play.plugins.AvroD2Plugin;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2ClientFactory implements FactoryBean<Object> {

  private Class<?> interfaceClass;
  private SpecificData specificData;

  @Override
  public Object getObject() throws Exception {
    return AvroD2Plugin.client(interfaceClass,
        MoreObjects.firstNonNull(specificData, new SpecificData(interfaceClass.getClassLoader())));
  }

  @Override
  public Class<?> getObjectType() {
    return interfaceClass;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  public void setInterfaceClass(Class<?> interfaceClass) {
    this.interfaceClass = interfaceClass;
  }

  public void setSpecificData(SpecificData specificData) {
    this.specificData = specificData;
  }
}
