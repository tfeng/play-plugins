package controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import me.tfeng.play.plugins.AvroPlugin;

import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @see org.apache.avro.ipc.HttpTransceiver
 * @author tfeng
 */
@Component
public class AvroRpcController extends Controller {

  @BodyParser.Of(BodyParser.Raw.class)
  public Result index(String beanName) {
    AvroPlugin plugin = AvroPlugin.getInstance();
    ConfigurableApplicationContext applicationContext = plugin.getApplicationContext();
    Object implementation = applicationContext.getBean(beanName);
    Class<?> interfaceClass = plugin.getInterfaceMap().get(beanName);
    if (interfaceClass == null) {
      throw new RuntimeException("Interface for bean " + beanName
          + " is not defined in interface map");
    }

    try {
      byte[] bytes = request().body().asRaw().asBytes();
      List<ByteBuffer> buffers = readBuffers(new ByteArrayInputStream(bytes));
      Responder responder = new SpecificResponder(interfaceClass, implementation);

      List<ByteBuffer> response = responder.respond(buffers);
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      try {
        writeBuffers(response, outStream);
      } finally {
        outStream.close();
      }

      return Results.ok(outStream.toByteArray());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private List<ByteBuffer> readBuffers(InputStream in) throws IOException {
    List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
    while (true) {
      int length = readLength(in);
      if (length == 0) {
        // end of buffers
        return buffers;
      }
      ByteBuffer buffer = ByteBuffer.allocate(length);
      while (buffer.hasRemaining()) {
        int p = buffer.position();
        int i = in.read(buffer.array(), p, buffer.remaining());
        if (i < 0) {
          throw new EOFException("Unexpected EOF");
        }
        buffer.position(p+i);
      }
      buffer.flip();
      buffers.add(buffer);
    }
  }

  private int readLength(InputStream in) throws IOException {
    return (in.read() << 24) + (in.read() << 16) + (in.read() << 8) + in.read();
  }

  private void writeBuffers(List<ByteBuffer> buffers, OutputStream out) throws IOException {
    for (ByteBuffer buffer : buffers) {
      writeLength(buffer.limit(), out);           // length-prefix
      out.write(buffer.array(), buffer.position(), buffer.remaining());
      buffer.position(buffer.limit());
    }
    writeLength(0, out);                          // null-terminate
  }

  private void writeLength(int length, OutputStream out) throws IOException {
    out.write(0xff & (length >>> 24));
    out.write(0xff & (length >>> 16));
    out.write(0xff & (length >>> 8));
    out.write(0xff & length);
  }
}
