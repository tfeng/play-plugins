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

import org.apache.avro.Protocol;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificData;

import me.tfeng.play.plugins.AvroPlugin;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class InternalSpecificResponder extends SpecificResponder {

  public InternalSpecificResponder(Class<?> iface, Object impl) {
    super(iface, impl);
  }

  public InternalSpecificResponder(Class<?> iface, Object impl, SpecificData data) {
    super(iface, impl, data);
  }

  public InternalSpecificResponder(Protocol protocol, Object impl) {
    super(protocol, impl);
  }

  public InternalSpecificResponder(Protocol protocol, Object impl, SpecificData data) {
    super(protocol, impl, data);
  }

  protected Protocol handshake(Decoder in, Encoder out, Transceiver connection) throws IOException {
    return AvroPlugin.getInstance().getProtocolVersionResolver().resolve(this, in, out, connection);
  }
}
