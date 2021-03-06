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

package me.tfeng.play.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.springframework.beans.factory.annotation.Value;
import org.webjars.WebJarAssetLocator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import me.tfeng.play.common.Constants;
import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.F.Promise;
import scala.concurrent.ExecutionContextExecutorService;
import scala.concurrent.Future;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class DustPlugin extends AbstractPlugin {

  private static final String DUST_JS_NAME = "dust-full.min.js";

  private static final ALogger LOG = Logger.of(DustPlugin.class);

  private static final String RENDER_SCRIPT =
      "dust.render(name, JSON.parse(json), function(err, data) {"
          + "if (err) throw new Error(err); else writer.write(data, 0, data.length); })";

  public static DustPlugin getInstance() {
    return Play.application().plugin(DustPlugin.class);
  }

  private WebJarAssetLocator assetLocator;

  private ConcurrentLinkedQueue<ScriptEngine> engines;

  private ExecutionContextExecutorService executionContext;

  private ThreadPoolExecutor executor;

  @Value("${dust-plugin.js-engine-pool-size:4}")
  private int jsEnginePoolSize;

  @Value("${dust-plugin.js-engine-pool-timeout-ms:10000}")
  private long jsEnginePoolTimeoutMs;

  private final ObjectMapper mapper = new ObjectMapper();

  private BlockingQueue<Runnable> queue;

  @Value("${dust-plugin.templates-directory:templates}")
  private String templatesDirectory;

  public DustPlugin(Application application) throws ScriptException {
    super(application);
  }

  @Override
  public void onStart() {
    super.onStart();

    assetLocator =
        new WebJarAssetLocator(WebJarAssetLocator.getFullPathIndex(Pattern.compile(".*\\.js$"),
            getApplication().classloader()));

    engines = new ConcurrentLinkedQueue<ScriptEngine>();
    for (int i = 0; i < jsEnginePoolSize; i++) {
      ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
      initializeScriptEngine(engine);
      engines.offer(engine);
    }

    queue = new LinkedBlockingQueue<>();
    executor = new ThreadPoolExecutor(jsEnginePoolSize, jsEnginePoolSize, jsEnginePoolTimeoutMs,
        TimeUnit.MILLISECONDS, queue);
    executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        LOG.warn("JS engine rejected a request; executor " + executor);
      }
    });
    executionContext = ExecutionContexts.fromExecutorService(executor);
  }

  public Promise<String> render(String template, JsonNode data) {
    Future<String> future = Futures.future(() -> {
      ScriptEngine engine = engines.poll();

      try {
        boolean isRegistered =
            ((Boolean) evaluate(engine, "dust.cache[template] !== undefined",
                ImmutableMap.of("template", template))).booleanValue();

        if (!isRegistered) {
          String jsFileName = templatesDirectory + "/" + template + ".js";
          if (LOG.isDebugEnabled()) {
            LOG.debug("Loading template " + jsFileName);
          }

          InputStream jsStream = getApplication().classloader().getResourceAsStream(
              assetLocator.getFullPath(jsFileName));
          String compiledTemplate = readAndClose(jsStream);
          evaluate(engine, "dust.loadSource(source)", ImmutableMap.of("source", compiledTemplate));
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("Rendering template " + template);
        }

        String json = mapper.writeValueAsString(data);
        StringWriter writer = new StringWriter();
        evaluate(engine, RENDER_SCRIPT,
            ImmutableMap.of("name", template, "json", json, "writer", writer));
        return writer.toString();
      } finally {
        engines.add(engine);
      }
    }, executionContext);

    return Promise.wrap(future);
  }

  private Object evaluate(ScriptEngine engine, String script, Map<String, Object> data)
      throws ScriptException {
    Bindings bindings = new SimpleBindings(data);
    engine.getContext().setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
    return engine.eval(script);
  }

  private void initializeScriptEngine(ScriptEngine engine) {
    String dustJsPath = assetLocator.getFullPath(DUST_JS_NAME);
    String dustJs = readAndClose(getApplication().classloader().getResourceAsStream(dustJsPath));
    try {
      engine.eval(dustJs);
    } catch (ScriptException e) {
      throw new RuntimeException("Unable to initialize script engine", e);
    }
  }

  private String readAndClose(InputStream stream) {
    try {
      return CharStreams.toString(new InputStreamReader(stream, Constants.UTF8));
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
