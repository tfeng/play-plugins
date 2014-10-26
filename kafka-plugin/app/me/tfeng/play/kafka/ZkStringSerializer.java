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

package me.tfeng.play.kafka;

import java.io.UnsupportedEncodingException;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ZkStringSerializer implements ZkSerializer {

  @Override
  public Object deserialize(byte[] bytes) throws ZkMarshallingError {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new ZkMarshallingError(e);
    }
  }

  @Override
  public byte[] serialize(Object data) throws ZkMarshallingError {
    try {
      return ((String) data).getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new ZkMarshallingError(e);
    }
  }
}
