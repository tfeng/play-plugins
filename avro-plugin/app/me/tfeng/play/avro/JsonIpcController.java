package me.tfeng.play.avro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import me.tfeng.play.plugins.AvroPlugin;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.ipc.HandshakeResponse;
import org.apache.avro.ipc.RPCContext;
import org.apache.avro.ipc.Requestor;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.generic.GenericRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.ByteBufferInputStream;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.context.ConfigurableApplicationContext;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

public class JsonIpcController extends Controller {

  private static class EmptyTransceiver extends Transceiver {

    @Override
    public String getRemoteName() throws IOException {
      return "localhost";
    }

    @Override
    public List<ByteBuffer> readBuffers() throws IOException {
      return Collections.emptyList();
    }

    @Override
    public void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    }
  }

  public static final String CONTENT_TYPE = "avro/json";

  private static final Transceiver EMPTY_TRANSCEIVER = new EmptyTransceiver();

  private static final SpecificDatumReader<HandshakeResponse> HANDSHAKE_READER =
      new SpecificDatumReader<HandshakeResponse>(HandshakeResponse.class);

  private static final Constructor<?> REQUEST_CONSTRUCTOR;

  private static final Method REQUEST_GETBYTES_METHOD;

  private static final Constructor<?> RESPONSE_CONSTRUCTOR;

  private static final Method RESPONSE_GETRESPONSE_METHOD;

  static {
    try {
      Class<?> requestClass =
          Requestor.class.getClassLoader().loadClass("org.apache.avro.ipc.Requestor$Request");
      Class<?> responseClass =
          Requestor.class.getClassLoader().loadClass("org.apache.avro.ipc.Requestor$Response");
      Constructor<?> requestConstructor = requestClass.getConstructor(Requestor.class, String.class,
          Object.class, RPCContext.class);
      requestConstructor.setAccessible(true);
      Constructor<?> responseConstructor = responseClass.getConstructor(Requestor.class,
          requestClass, BinaryDecoder.class);
      responseConstructor.setAccessible(true);
      Method requestGetBytesMethod = requestClass.getMethod("getBytes");
      requestGetBytesMethod.setAccessible(true);
      Method responseGetResponseMethod = responseClass.getMethod("getResponse");
      responseGetResponseMethod.setAccessible(true);

      REQUEST_CONSTRUCTOR = requestConstructor;
      RESPONSE_CONSTRUCTOR = responseConstructor;
      REQUEST_GETBYTES_METHOD = requestGetBytesMethod;
      RESPONSE_GETRESPONSE_METHOD = responseGetResponseMethod;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BodyParser.Of(BodyParser.Raw.class)
  public static Result post(String message, String implementation) throws Throwable {
    String contentType = request().getHeader("content-type");
    if (!CONTENT_TYPE.equals(contentType)) {
      throw new RuntimeException("Unable to handle content-type " + contentType + "; "
          + CONTENT_TYPE + " is expected");
    }

    AvroPlugin plugin = AvroPlugin.getInstance();
    ConfigurableApplicationContext applicationContext = plugin.getApplicationContext();
    Object implementationBean = applicationContext.getBean(implementation);
    Class<?> interfaceClass = plugin.getInterfaceMap().get(implementation);
    if (interfaceClass == null) {
      throw new RuntimeException("Interface for bean " + implementation
          + " is not defined in interface map");
    }

    byte[] bytes = request().body().asRaw().asBytes();
    Protocol protocol = (Protocol) interfaceClass.getField("PROTOCOL").get(null);
    GenericRequestor requestor = new GenericRequestor(protocol, EMPTY_TRANSCEIVER);
    Object request = getRequest(requestor, protocol, message, bytes);
    List<ByteBuffer> buffers = convertToBuffers(request);
    Responder responder = new SpecificResponder(interfaceClass, implementationBean);

    List<ByteBuffer> responseBuffers = responder.respond(buffers);

    try {
      Object response = getResponse(requestor, request, responseBuffers);
      return Results.ok(convertJson(protocol.getMessages().get(message).getResponse(), response));
    } catch (AvroRemoteException e) {
      Schema schema = protocol.getMessages().get(message).getErrors();
      return Results.badRequest(convertJson(schema, e.getValue()));
    }
  }

  private static String convertJson(Schema schema, Object response) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    DatumWriter<Object> writer = new GenericDatumWriter<Object>(schema);
    JsonGenerator generator =
        new JsonFactory().createJsonGenerator(outputStream, JsonEncoding.UTF8);
    generator.useDefaultPrettyPrinter();
    JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, generator);
    writer.write(response, encoder);
    encoder.flush();
    return outputStream.toString();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static List<ByteBuffer> convertToBuffers(Object requestObject) throws Throwable {
    try {
      return (List) REQUEST_GETBYTES_METHOD.invoke(requestObject);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static Object getRequest(Requestor requestor, Protocol protocol, String message, byte[] data)
      throws Throwable {
    try {
      Message messageObject = protocol.getMessages().get(message);
      if (messageObject == null) {
        throw new AvroRuntimeException("No message named "+ message + " in "+ protocol);
      }
      Schema schema = messageObject.getRequest();
      GenericDatumReader<Object> reader = new GenericDatumReader<Object>(schema);
      Object request = reader.read(null, DecoderFactory.get().jsonDecoder(schema,
          new ByteArrayInputStream(data)));
      return REQUEST_CONSTRUCTOR.newInstance(requestor, message, request, new RPCContext());
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static Object getResponse(Requestor requestor, Object request, List<ByteBuffer> buffers)
      throws Throwable {
    try {
      ByteBufferInputStream bbi = new ByteBufferInputStream(buffers);
      BinaryDecoder in = DecoderFactory.get().binaryDecoder(bbi, null);
      HANDSHAKE_READER.read(null, in);
      Object responseObject = RESPONSE_CONSTRUCTOR.newInstance(requestor, request, in);
      return RESPONSE_GETRESPONSE_METHOD.invoke(responseObject);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
