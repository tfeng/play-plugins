package org.apache.avro.ipc;

import java.io.IOException;

import org.apache.avro.Protocol;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificData;

import me.tfeng.play.plugins.AvroPlugin;

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
