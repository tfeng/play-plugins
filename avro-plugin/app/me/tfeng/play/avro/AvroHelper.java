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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroHelper {

  public static Protocol getProtocol(Class<?> interfaceClass) {
    return new SpecificData(interfaceClass.getClassLoader()).getProtocol(interfaceClass);
  }

  public static Schema getSchema(Class<?> schemaClass) {
    return new SpecificData(schemaClass.getClassLoader()).getSchema(schemaClass);
  }

  public static String toJson(IndexedRecord record) throws IOException {
    Schema schema = record.getSchema();
    return toJson(schema, record);
  }

  public static String toJson(Schema schema, Object object) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    GenericDatumWriter<Object> writer = new GenericDatumWriter<Object>(schema);
    JsonGenerator generator =
        new JsonFactory().createJsonGenerator(outputStream, JsonEncoding.UTF8);
    generator.useDefaultPrettyPrinter();
    JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, generator);
    writer.write(object, encoder);
    encoder.flush();
    return outputStream.toString();
  }

  public static <T> T toRecord(Class<T> recordClass, String json) throws IOException {
    SpecificDatumReader<T> reader = new SpecificDatumReader<>(recordClass);
    return reader.read(null, DecoderFactory.get().jsonDecoder(getSchema(recordClass), json));
  }

  public static <T> T toRecord(Schema schema, String json) throws IOException {
    SpecificDatumReader<T> reader = new SpecificDatumReader<>(schema);
    return reader.read(null, DecoderFactory.get().jsonDecoder(schema, json));
  }
}
