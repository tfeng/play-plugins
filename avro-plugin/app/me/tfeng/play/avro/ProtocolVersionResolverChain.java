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

import java.io.IOException;
import java.util.Arrays;

import org.apache.avro.Protocol;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.ipc.Transceiver;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ProtocolVersionResolverChain extends Chain<ProtocolVersionResolver>
    implements ProtocolVersionResolver {

  public ProtocolVersionResolverChain(ProtocolVersionResolver... resolvers) {
    Arrays.stream(resolvers).forEach(this::add);
  }

  @Override
  public Protocol resolve(Decoder in, Encoder out, Transceiver connection) throws IOException {
    for (ProtocolVersionResolver resolver: getAll()) {
      Protocol protocol = resolver.resolve(in, out, connection);
      if (protocol != null) {
        return protocol;
      }
    }
    return null;
  }
}
