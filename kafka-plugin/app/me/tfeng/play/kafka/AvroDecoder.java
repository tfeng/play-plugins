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

import java.io.IOException;
import java.nio.charset.Charset;

import kafka.serializer.Decoder;
import kafka.utils.VerifiableProperties;
import me.tfeng.play.avro.AvroHelper;

import org.apache.avro.generic.IndexedRecord;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroDecoder<T extends IndexedRecord> implements Decoder<T> {

  private Class<? extends T> recordClass;

  private static final Charset UTF8 = Charset.forName("UTF-8");

  public AvroDecoder() {
    this((VerifiableProperties) null);
  }

  public AvroDecoder(Class<T> recordClass) {
    this((VerifiableProperties) null);
    this.recordClass = recordClass;
  }

  public AvroDecoder(VerifiableProperties verifiableProperties) {
  }

  @Override
  public T fromBytes(byte[] data) {
    try {
      return AvroHelper.decodeRecord(recordClass, data);
    } catch (IOException e) {
      throw new RuntimeException("Unable to decode Kafka event " + new String(data, UTF8));
    }
  }

  public AvroDecoder<T> setRecordClass(Class<? extends T> recordClass) {
    this.recordClass = recordClass;
    return this;
  }
}
