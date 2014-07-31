package me.tfeng.play.plugins;

import java.util.Collections;
import java.util.List;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.Scala;

import com.google.common.collect.Lists;

public class SpringPlugin extends AbstractPlugin<SpringPlugin> {

  private static final ALogger LOG = Logger.of(SpringPlugin.class);

  public static SpringPlugin getInstance() {
    return Play.application().plugin(SpringPlugin.class);
  }

  private ClassPathXmlApplicationContext applicationContext;

  public SpringPlugin(Application application) {
    super(application);
  }

  @Override
  public ConfigurableApplicationContext getApplicationContext() {
    return applicationContext;
  }

  @Override
  public void onStart() {
    List<String> configLocations =
        Lists.newArrayList(getConfiguration().getStringList("spring-plugin.spring-config-locations",
            Collections.singletonList("classpath*:spring/*.xml")));

    List<play.api.Plugin> plugins =
        Scala.asJava(getApplication().getWrappedApplication().plugins());
    for (play.api.Plugin plugin : plugins) {
      WithSpringConfig annotation = plugin.getClass().getAnnotation(WithSpringConfig.class);
      if (annotation != null) {
        Collections.addAll(configLocations, annotation.value());
      }
    }

    LOG.info("Starting spring application context with config locations " + configLocations);
    applicationContext = new ClassPathXmlApplicationContext(
        configLocations.toArray(new String[configLocations.size()]));

    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

    LOG.info("Stopping spring application context");
    applicationContext.close();
  }
}
