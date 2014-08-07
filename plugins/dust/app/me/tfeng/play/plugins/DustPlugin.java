package me.tfeng.play.plugins;

import java.io.InputStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import play.Application;
import play.Play;
import play.libs.F.Promise;

import com.fasterxml.jackson.databind.JsonNode;

public class DustPlugin extends AbstractPlugin<DustPlugin> {

  public static DustPlugin getInstance() {
    return Play.application().plugin(DustPlugin.class);
  }

  public DustPlugin(Application application) {
    super(application);
  }

  public Promise<String> render(String template, JsonNode data) {
    if (Play.application().isProd()) {
      String dustFileName = template + ".tl";
    }
    return null;
  }

  ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

  private void compileDustTemplate(String dustTemplate) {
    InputStream dustStream = getClass().getClassLoader().getResourceAsStream(dustTemplate);
  }
}
