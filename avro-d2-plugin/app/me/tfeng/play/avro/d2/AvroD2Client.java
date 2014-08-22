package me.tfeng.play.avro.d2;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import play.Logger;
import play.Logger.ALogger;

public class AvroD2Client implements Watcher, InvocationHandler {

  private static final ALogger LOG = Logger.of(AvroD2Client.class);

  private int lastIndex = -1;
  private final Protocol protocol;
  private final SpecificRequestor requestor;
  private final List<URL> serverUrls = new ArrayList<>();
  private final ZooKeeper zk;

  public AvroD2Client(ZooKeeper zk, Class<?> interfaceClass) {
    this.zk = zk;

    protocol = AvroD2Helper.getProtocol(interfaceClass);
    refresh();

    try {
      requestor = new SpecificRequestor(interfaceClass, new AvroD2Transceiver(this));
    } catch (IOException e) {
      throw new RuntimeException("Unable to initialize Avro requestor for "
          + AvroD2Helper.getUri(protocol), e);
    }
  }

  public URL getNextServerUrl() {
    if (serverUrls.isEmpty()) {
      throw new RuntimeException("No server is found for " + AvroD2Helper.getUri(protocol));
    } else {
      lastIndex = (lastIndex + 1) % serverUrls.size();
      return serverUrls.get(lastIndex);
    }
  }

  public Protocol getProtocol() {
    return protocol;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return requestor.invoke(proxy, method, args);
  }

  @Override
  public void process(WatchedEvent event) {
    try {
      refresh();
    } catch (Exception e) {
      LOG.error("Unable to get server URL from path " + AvroD2Helper.getZkPath(protocol));
    }
  }

  public void refresh() {
    String path = AvroD2Helper.getZkPath(protocol);
    List<String> children;
    try {
      children = zk.getChildren(path, this);
    } catch (Exception e) {
      throw new RuntimeException("Unable to list servers for " + AvroD2Helper.getUri(protocol));
    }

    serverUrls.clear();
    for (String child : children) {
      String childPath = path + "/" + child;
      try {
        byte[] data = zk.getData(childPath, false, null);
        String serverUrl = new String(data, Charset.forName("utf8"));
        serverUrls.add(new URL(serverUrl));
      } catch (Exception e) {
        LOG.warn("Unable to get server URL from node " + childPath);
      }
    }
  }
}
