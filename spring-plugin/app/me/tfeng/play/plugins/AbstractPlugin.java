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

  public <A> A getBean(Class<A> clazz) {
    return getBean(null, clazz);
  }

  public <A> A getBean(String beanNameProperty, Class<A> clazz) {
    String beanName =
        beanNameProperty == null ? null : getConfiguration().getString(beanNameProperty);
    if (beanName == null) {
      return getApplicationContext().getBean(clazz);
    } else {
      return getApplicationContext().getBean(beanName, clazz);
    }
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

  protected ConfigurableApplicationContext getApplicationContext() {
    return SpringPlugin.getInstance().getApplicationContext();
  }

  protected Configuration getConfiguration() {
    return application.configuration();
  }
}
