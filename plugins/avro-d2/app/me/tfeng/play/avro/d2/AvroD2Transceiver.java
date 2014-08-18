package me.tfeng.play.avro.d2;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.Transceiver;

public class AvroD2Transceiver extends Transceiver {

  private final URI uri;

  public AvroD2Transceiver(Protocol protocol) {
    this(AvroD2Helper.getUri(protocol));
  }

  public AvroD2Transceiver(URI uri) {
    if (!AvroD2Helper.SCHEME.equals(uri.getScheme())) {
      throw new RuntimeException("Unexpected schema for Avro load balancing protocol: "
          + uri.getScheme());
    }
    this.uri = uri;
  }

  @Override
  public String getRemoteName() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ByteBuffer> readBuffers() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    // TODO Auto-generated method stub

  }

}
