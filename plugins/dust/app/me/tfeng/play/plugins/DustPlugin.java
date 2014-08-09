package me.tfeng.play.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.springframework.beans.factory.annotation.Value;
import org.webjars.WebJarAssetLocator;

import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.F.Promise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

public class DustPlugin extends AbstractPlugin<DustPlugin> {

  private static final String DUST_JS_NAME = "dust-full.min.js";

  private static final ALogger LOG = Logger.of(DustPlugin.class);

  private static final String RENDER_SCRIPT =
      "{dust.render(name, JSON.parse(json), function(err, data) {"
          + "if (err) throw new Error(err); else writer.write(data, 0, data.length); });}";

  public static DustPlugin getInstance() {
    return Play.application().plugin(DustPlugin.class);
  }

  private final WebJarAssetLocator assetLocator = new WebJarAssetLocator();

  private Object dustJs;

  private ScriptEngine engine;

  private Set<String> loadedTemplates = new HashSet<String>();

  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${dust-plugin.templates-directory:templates}")
  private String templatesDirectory;

  public DustPlugin(Application application) throws ScriptException {
    super(application);

    // Need to use the initial class loader to access extension libraries.
    engine = new ScriptEngineManager(null).getEngineByName("nashorn");
    dustJs = initializeScriptEngine();
  }

  public Promise<String> render(String template, JsonNode data) {
    return Promise.promise(() -> {
      try {
        if (!loadedTemplates.contains(template)) {
          String jsFileName = templatesDirectory + "/" + template + ".js";
          if (LOG.isDebugEnabled()) {
            LOG.debug("Loading template " + jsFileName);
          }

          String compiledTemplate;
          Invocable invocable = (Invocable) engine;
          InputStream jsStream = WebJarAssetLocator.class.getClassLoader().getResourceAsStream(
              assetLocator.getFullPath(jsFileName));
          compiledTemplate = readAndClose(jsStream);
          invocable.invokeMethod(dustJs, "loadSource", compiledTemplate);
          loadedTemplates.add(template);
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("Rendering template " + template);
        }
        StringWriter writer = new StringWriter();
        String json = mapper.writeValueAsString(data);
        Bindings bindings = new SimpleBindings();
        bindings.put("name", template);
        bindings.put("json", json);
        bindings.put("writer", writer);
        engine.getContext().setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        engine.eval(RENDER_SCRIPT, engine.getContext());
        return writer.toString();
      } catch (ScriptException | NoSuchMethodException e) {
        throw new RuntimeException("Unable to execute javascript", e);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Unable to process json data " + data, e);
      }
    });
  }

  private Object initializeScriptEngine() {
    String dustJsPath = assetLocator.getFullPath(DUST_JS_NAME);
    InputStream dustJsStream =
        WebJarAssetLocator.class.getClassLoader().getResourceAsStream(dustJsPath);
    try {
      engine.eval(new InputStreamReader(dustJsStream));
      return engine.eval("dust");
    } catch (ScriptException e) {
      throw new RuntimeException("Unable to initialize script engine", e);
    } finally {
      try {
        dustJsStream.close();
      } catch (IOException e) {
        throw new RuntimeException("Unable to close stream", e);
      }
    }
  }

  private String readAndClose(InputStream stream) {
    try {
      return CharStreams.toString(new InputStreamReader(stream, Charset.forName("utf8")));
    } catch (IOException e) {
      throw new RuntimeException("Unable to read from stream", e);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        throw new RuntimeException("Unable to close stream", e);
      }
    }
  }
}
