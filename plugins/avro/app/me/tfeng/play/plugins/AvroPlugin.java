package me.tfeng.play.plugins;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import play.Application;
import play.Play;

public class AvroPlugin extends AbstractPlugin<AvroPlugin> {

  public static AvroPlugin getInstance() {
    return Play.application().plugin(AvroPlugin.class);
  }

  private Map<String, Class<?>> interfaceMap;

  @Value("${avro-plugin.ipc-interface-map:avroIpcInterfaceMap}")
  private String interfaceMapName;

  public AvroPlugin(Application application) {
    super(application);
  }

  public Map<String, Class<?>> getInterfaceMap() {
    return interfaceMap;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart() {
    super.onStart();
    interfaceMap = Collections.<String, Class<?>>unmodifiableMap(
        getApplicationContext().getBean(interfaceMapName, Map.class));
  }
}
