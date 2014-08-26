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

package me.tfeng.play.mongodb;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class RecordConverter {

  public static DBObject getDbObject(IndexedRecord record) {
    DBObject dbObject = new BasicDBObject();
    Schema schema = record.getSchema();
    for (Field field : schema.getFields()) {
      Object value = record.get(field.pos());
      dbObject.put(field.name(), getDbObject(value));
    }
    return dbObject;
  }

  @SuppressWarnings("unchecked")
  public static Object getDbObject(Object object) {
    if (object instanceof IndexedRecord) {
      return getDbObject(object);
    } else if (object instanceof Collection) {
      return getDbObjects((Collection<Object>) object);
    } else if (object instanceof Map) {
      return getDbObjects((Map<Object, Object>) object);
    } else {
      return object;
    }
  }

  public static List<Object> getDbObjects(Collection<Object> collection) {
    return collection.stream().map(object -> getDbObject(object)).collect(Collectors.toList());
  }

  public static Map<Object, Object> getDbObjects(Map<Object, Object> map) {
    Map<Object, Object> newMap = new HashMap<>(map.size());
    map.entrySet().forEach(entry -> newMap.put(entry.getKey(), getDbObject(entry.getValue())));
    return newMap;
  }

  public static <T extends IndexedRecord> T getRecord(Class<T> clazz, DBObject object) {
    Schema schema = new SpecificData(clazz.getClassLoader()).getSchema(clazz);
    String json = JSON.serialize(object);
    SpecificDatumReader<T> reader = new SpecificDatumReader<T>(clazz);
    try {
      return reader.read(null, DecoderFactory.get().jsonDecoder(schema, json));
    } catch (IOException e) {
      throw new RuntimeException("Unable to convert json " + json + " into Avro record", e);
    }
  }
}
