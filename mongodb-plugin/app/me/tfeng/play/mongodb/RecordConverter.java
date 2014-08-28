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
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class RecordConverter {

  private static final ALogger LOG = Logger.of(RecordConverter.class);

  private static final JsonNodeFactory NODE_FACTORY = new JsonNodeFactory(false);

  public static DBObject toDbObject(IndexedRecord record) {
    DBObject dbObject = new BasicDBObject();
    Schema schema = record.getSchema();
    for (Field field : schema.getFields()) {
      Object value = record.get(field.pos());
      dbObject.put(field.name(), getDbObject(value));
    }

    try {
      for (java.lang.reflect.Field field : record.getClass().getFields()) {
        if (String.class.isAssignableFrom(field.getType())) {
          Id annotation = field.getAnnotation(Id.class);
          if (annotation != null) {
            dbObject.removeField(field.getName());
            field.setAccessible(true);
            String id = (String) field.get(record);
            if (id != null) {
              dbObject.put(annotation.value(), new ObjectId(id));
            }
          }
        }
      }
    } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
      throw new RuntimeException("Unable to get id field of record " + record.getClass());
    }

    return dbObject;
  }

  @SuppressWarnings("unchecked")
  private static Object getDbObject(Object object) {
    if (object instanceof IndexedRecord) {
      return toDbObject((IndexedRecord) object);
    } else if (object instanceof Collection) {
      return getDbObjects((Collection<Object>) object);
    } else if (object instanceof Map) {
      return getDbObjects((Map<Object, Object>) object);
    } else if (object instanceof ByteBuffer) {
      return new Binary(((ByteBuffer) object).array());
    } else {
      return object;
    }
  }

  private static List<Object> getDbObjects(Collection<Object> collection) {
    return collection.stream().map(object -> getDbObject(object)).collect(Collectors.toList());
  }

  private static Map<Object, Object> getDbObjects(Map<Object, Object> map) {
    Map<Object, Object> newMap = new HashMap<>(map.size());
    map.entrySet().forEach(entry -> newMap.put(entry.getKey(), getDbObject(entry.getValue())));
    return newMap;
  }

  public static <T extends IndexedRecord> T toRecord(Class<T> recordClass, DBObject dbObject) {
    mapIdFields(recordClass, dbObject);
    Schema schema = new SpecificData(recordClass.getClassLoader()).getSchema(recordClass);
    SpecificDatumReader<T> reader = new SpecificDatumReader<T>(recordClass);

    if (LOG.isDebugEnabled()) {
      JsonNode json = toAvroJson(schema, dbObject);
      String jsonString = json.toString();
      LOG.debug("Converted Avro json from MongoDB: ", jsonString);
    }

    T record;
    try {
      // Decoder decoder = new LoggingJsonDecoder(schema, jsonString);
      // Decoder decoder = DecoderFactory.get().jsonDecoder(schema, jsonString);
      Decoder decoder = new DBObjectDecoder(schema, dbObject);
      record = reader.read(null, decoder);
    } catch (IOException e) {
      throw new RuntimeException("Unable to convert MongoDB object " + dbObject
          + " into Avro record", e);
    }

    return record;
  }

  @SuppressWarnings("unchecked")
  public static JsonNode toAvroJson(Schema schema, Object dbObject) {
    switch (schema.getType()) {
    case NULL:
      return NODE_FACTORY.nullNode();
    case ENUM:
    case FIXED:
    case STRING:
      return NODE_FACTORY.textNode((String) dbObject);
    case BOOLEAN:
      return NODE_FACTORY.booleanNode((Boolean) dbObject);
    case INT:
      return NODE_FACTORY.numberNode(((Number) dbObject).intValue());
    case LONG:
      return NODE_FACTORY.numberNode(((Number) dbObject).longValue());
    case FLOAT:
      return NODE_FACTORY.numberNode(((Number) dbObject).floatValue());
    case DOUBLE:
      return NODE_FACTORY.numberNode(((Number) dbObject).doubleValue());
    case ARRAY: {
      List<Object> list = (List<Object>) dbObject;
      ArrayNode arrayNode = NODE_FACTORY.arrayNode();
      list.forEach(element -> arrayNode.add(toAvroJson(schema.getElementType(), element)));
      return arrayNode;
    }
    case MAP: {
      Map<String, Object> map = (Map<String, Object>) dbObject;
      ObjectNode mapNode = NODE_FACTORY.objectNode();
      map.entrySet().forEach(entry -> mapNode.put(entry.getKey(),
          toAvroJson(schema.getValueType(), entry.getValue())));
      return mapNode;
    }
    case RECORD: {
      Map<String, Object> map = (Map<String, Object>) dbObject;
      ObjectNode recordNode = NODE_FACTORY.objectNode();
      for (Field field : schema.getFields()) {
        Object value = map.get(field.name());
        if (value == null) {
          recordNode.put(field.name(), NullNode.instance);
        } else {
          recordNode.put(field.name(), toAvroJson(field.schema(), value));
        }
      }
      return recordNode;
    }
    case UNION: {
      List<Schema> types = schema.getTypes();
      if (types.size() != 2 || !types.stream().anyMatch(type -> type.getType() == Type.NULL)) {
        throw new RuntimeException(
            "MongoDb plugin can only handle union of null and one other type; schema " + schema
            + " is not supported");
      }
      Schema actualSchema = types.get(0).getType() == Type.NULL ? types.get(1) : types.get(0);
      ObjectNode mapNode = NODE_FACTORY.objectNode();
      mapNode.put(actualSchema.getFullName(), toAvroJson(actualSchema, dbObject));
      return mapNode;
    }
    case BYTES: {
      return NODE_FACTORY.binaryNode((byte[]) dbObject);
    }
    default:
      throw new RuntimeException("Unknown Avro type " + schema.getType());
    }
  }

  /**
   * Look for all annotated id fields in the record class, and set the ids in the object. A common
   * use case is that "id" field of the class may be mapped to "_id" in the object.
   */
  private static <T extends IndexedRecord> void mapIdFields(Class<T> recordClass, DBObject object) {
    for (java.lang.reflect.Field field : recordClass.getFields()) {
      if (String.class.isAssignableFrom(field.getType())) {
        Id annotation = field.getAnnotation(Id.class);
        if (annotation != null) {
          Object id = object.get(annotation.value());
          if (id != null) {
            object.put(field.getName(), id.toString());
          }
        }
      }
    }
  }
}
