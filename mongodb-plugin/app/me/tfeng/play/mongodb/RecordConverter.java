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
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.bson.types.Binary;
import org.mortbay.util.ajax.JSON;

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

  public static final String MONGO_CLASS_PROPERTY = "mongo-class";

  public static final String MONGO_NAME_PROPERTY = "mongo-name";

  public static final String MONGO_TYPE_PROPERTY = "mongo-type";

  private static final JsonNodeFactory NODE_FACTORY = new JsonNodeFactory(false);

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

  public static DBObject toDbObject(IndexedRecord record) {
    DBObject dbObject = new BasicDBObject();
    Schema schema = record.getSchema();
    for (Field field : schema.getFields()) {
      Object value = record.get(field.pos());
      if (value != null) {
        dbObject.put(getFieldName(field), getDbObject(field.schema(), value));
      }
    }
    return dbObject;
  }

  public static <T extends IndexedRecord> T toRecord(Class<T> recordClass, DBObject dbObject) {
    SpecificDatumReader<T> reader = new SpecificDatumReader<T>(recordClass);
    try {
      Decoder decoder = new DBObjectDecoder(recordClass, dbObject);
      return reader.read(null, decoder);
    } catch (IOException e) {
      throw new RuntimeException("Unable to convert MongoDB object " + dbObject
          + " into Avro record", e);
    }
  }

  public static Record toRecord(Schema schema, DBObject object, ClassLoader classLoader)
      throws IOException {
    GenericDatumReader<Record> reader = new GenericDatumReader<>(schema);
    return reader.read(null, new DBObjectDecoder(schema, object, classLoader));
  }

  protected static String getFieldName(Field field) {
    String mongoName = field.getProp(MONGO_NAME_PROPERTY);
    if (mongoName != null) {
      return mongoName;
    } else {
      return field.name();
    }
  }

  @SuppressWarnings("unchecked")
  private static Object getDbObject(Schema schema, Object object) {
    if (object == null) {
      return null;
    } else if (object instanceof IndexedRecord) {
      return toDbObject((IndexedRecord) object);
    } else if (object instanceof Collection) {
      return getDbObjects(schema, (Collection<Object>) object);
    } else if (object instanceof Map) {
      return getDbObjects(schema, (Map<String, Object>) object);
    } else if (object instanceof ByteBuffer) {
      return new Binary(((ByteBuffer) object).array());
    } else {
      String mongoClassName = schema.getProp(MONGO_CLASS_PROPERTY);
      String mongoType = schema.getProp(MONGO_TYPE_PROPERTY);
      if (object instanceof CharSequence) {
        object = ((CharSequence) object).toString();
      }
      if (mongoClassName == null && mongoType == null) {
        return object;
      } else if (mongoClassName != null && mongoType != null) {
        throw new RuntimeException("mongo-class and mongo-type should not be both specified: "
            + schema);
      } else {
        try {
          Class<?> mongoClass;
          if (mongoClassName != null) {
            mongoClass = schema.getClass().getClassLoader().loadClass(mongoClassName);
          } else {
            mongoClass = MongoType.valueOf(mongoType).getMongoClass();
          }
          if (object instanceof String && mongoClass.isAssignableFrom(Object.class)) {
            return JSON.parse((String) object);
          } else {
            return MongoDbTypeConverter.convertToMongoDbType(mongoClass, object);
          }
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Unable to load mongo-class " + mongoClassName, e);
        }
      }
    }
  }

  private static List<Object> getDbObjects(Schema schema, Collection<Object> collection) {
    return collection.stream().map(object -> getDbObject(schema.getElementType(), object))
        .collect(Collectors.toList());
  }

  private static Map<String, Object> getDbObjects(Schema schema, Map<String, Object> map) {
    Map<String, Object> newMap = new HashMap<>(map.size());
    map.entrySet().forEach(
        entry -> newMap.put(entry.getKey(), getDbObject(schema.getValueType(), entry.getValue())));
    return newMap;
  }
}
