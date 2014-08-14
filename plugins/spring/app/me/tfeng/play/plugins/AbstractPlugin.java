package me.tfeng.play.plugins;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

import play.Application;
import play.Configuration;
import play.Plugin;

public class AbstractPlugin<T extends Plugin> extends Plugin {

  private Application application;

  public AbstractPlugin(Application application) {
    this.application = application;
  }

  public ConfigurableApplicationContext getApplicationContext() {
    return application.plugin(SpringPlugin.class).getApplicationContext();
  }

  @Override
  public void onStart() {
    AutowiredAnnotationBeanPostProcessor beanPostProcessor =
        new AutowiredAnnotationBeanPostProcessor();
    beanPostProcessor.setBeanFactory(getApplicationContext().getBeanFactory());
    beanPostProcessor.processInjection(this);
  }

  protected Application getApplication() {
    return application;
  }

  protected Configuration getConfiguration() {
    return application.configuration();
  }
}
