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

package org.apache.avro.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;

import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class LoggingJsonDecoder extends JsonDecoder {

  private static final ALogger LOG = Logger.of(LoggingJsonDecoder.class);

  public LoggingJsonDecoder(Schema schema, InputStream in) throws IOException {
    super(schema, in);
  }

  public LoggingJsonDecoder(Schema schema, String in) throws IOException {
    super(schema, in);
  }

  @Override
  public long arrayNext() throws IOException {
    long result = super.arrayNext();
    LOG.info("arrayNext() = " + result);
    return result;
  }

  @Override
  public long mapNext() throws IOException {
    long result = super.mapNext();
    LOG.info("mapNext() = " + result);
    return result;
  }

  @Override
  public long readArrayStart() throws IOException {
    long result = super.readArrayStart();
    LOG.info("readArrayStart() = " + result);
    return result;
  }

  @Override
  public boolean readBoolean() throws IOException {
    boolean result = super.readBoolean();
    LOG.info("readBoolean() = " + result);
    return result;
  }

  @Override
  public ByteBuffer readBytes(ByteBuffer old) throws IOException {
    ByteBuffer result = super.readBytes(old);
    LOG.info("readBytes(...) = ...");
    return result;
  }

  @Override
  public double readDouble() throws IOException {
    double result = super.readDouble();
    LOG.info("readDouble() = " + result);
    return result;
  }

  @Override
  public int readEnum() throws IOException {
    int result = super.readEnum();
    LOG.info("readEnum() = " + result);
    return result;
  }

  @Override
  public void readFixed(byte[] bytes, int start, int length) throws IOException {
    super.readFixed(bytes, start, length);
    LOG.info("readFixed(..., " + start + ", " + length + ")");
  }

  @Override
  public float readFloat() throws IOException {
    float result = super.readFloat();
    LOG.info("readFloat() = " + result);
    return result;
  }

  @Override
  public int readIndex() throws IOException {
    int result = super.readIndex();
    LOG.info("readIndex() = " + result);
    return result;
  }

  @Override
  public int readInt() throws IOException {
    int result = super.readInt();
    LOG.info("readInt() = " + result);
    return result;
  }

  @Override
  public long readLong() throws IOException {
    long result = super.readLong();
    LOG.info("readLong() = " + result);
    return result;
  }

  @Override
  public long readMapStart() throws IOException {
    long result = super.readMapStart();
    LOG.info("readMapStart() = " + result);
    return result;
  }

  @Override
  public void readNull() throws IOException {
    super.readNull();
    LOG.info("readNull()");
  }

  @Override
  public String readString() throws IOException {
    String result = super.readString();
    LOG.info("readString() = " + result);
    return result;
  }

  @Override
  public Utf8 readString(Utf8 old) throws IOException {
    Utf8 result = super.readString(old);
    LOG.info("readString(" + old + ") = " + result);
    return result;
  }

  @Override
  public long skipArray() throws IOException {
    long result = super.skipArray();
    LOG.info("skipArray() = " + result);
    return result;
  }

  @Override
  public void skipBytes() throws IOException {
    super.skipBytes();
    LOG.info("skipBytes()");
  }

  @Override
  public void skipFixed(int length) throws IOException {
    super.skipFixed(length);
    LOG.info("skipFixed(" + length + ")");
  }

  @Override
  public long skipMap() throws IOException {
    long result = super.skipMap();
    LOG.info("skipMap() = " + result);
    return result;
  }

  @Override
  public void skipString() throws IOException {
    super.skipString();
    LOG.info("skipString()");
  }
}
