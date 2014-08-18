package me.tfeng.play.plugins;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jline.internal.Log;
import me.tfeng.play.avro.d2.AvroD2Server;

import org.apache.avro.Protocol;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;

import play.Application;
import play.Play;

public class AvroD2Plugin extends AbstractPlugin<AvroD2Plugin> implements Watcher {

  public static AvroD2Plugin getInstance() {
    return Play.application().plugin(AvroD2Plugin.class);
  }

  private Map<String, Class<?>> protocolMap;

  @Value("${avro-d2-plugin.protocol-map:avroD2ProtocolMap}")
  private String protocolMapName;

  @Value("${avro-d2-plugin.server-host}")
  private String serverHost;

  @Value("${avro-d2-plugin.server-port}")
  private int serverPort;

  private List<AvroD2Server> servers;

  @Value("${avro-d2-plugin.zk-connect-string}")
  private String zkConnectString;

  @Value("${avro-d2-plugin.zk-session-timeout:10000}")
  private int zkSessionTimeout;

  public AvroD2Plugin(Application application) {
    super(application);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart() {
    super.onStart();

    protocolMap = Collections.<String, Class<?>>unmodifiableMap(
        getApplicationContext().getBean(protocolMapName, Map.class));

    try {
      ZooKeeper zk = new ZooKeeper(zkConnectString, zkSessionTimeout, this);
      servers = new ArrayList<>(protocolMap.size());
      for (Entry<String, Class<?>> entry : protocolMap.entrySet()) {
        Protocol protocol = (Protocol) entry.getValue().getField("PROTOCOL").get(null);
        String path = entry.getKey();
        if (!path.startsWith("/")) {
          path = "/" + path;
        }
        URL url = new URL("http", serverHost, serverPort, path);
        AvroD2Server server = new AvroD2Server(zk, protocol, url);
        servers.add(server);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to initialize server", e);
    }
  }

  @Override
  public void process(WatchedEvent event) {
    Log.info(event);
  }
}
