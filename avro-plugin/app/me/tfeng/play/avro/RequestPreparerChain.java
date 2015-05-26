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

import java.net.URL;
import java.util.Arrays;

import com.ning.http.client.AsyncHttpClient;

import me.tfeng.play.common.Chain;
import me.tfeng.play.http.RequestPreparer;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class RequestPreparerChain extends Chain<RequestPreparer> implements RequestPreparer {

  public RequestPreparerChain(RequestPreparer... preparers) {
    Arrays.stream(preparers).forEach(this::add);
  }

  @Override
  public void prepare(AsyncHttpClient.BoundRequestBuilder builder, String contentType, URL url) {
    getAll().forEach(preparer -> preparer.prepare(builder, contentType, url));
  }
}
