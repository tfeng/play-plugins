package me.tfeng.play.plugins;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

import play.Application;
import play.GlobalSettings;

public class SpringGlobalSettings extends GlobalSettings {

  @Override
  public <A> A getControllerInstance(Class<A> clazz) {
    try {
      return SpringPlugin.getInstance().getApplicationContext().getBean(clazz);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public void onStart(Application application) {
    ConfigurableApplicationContext applicationContext =
        application.plugin(SpringPlugin.class).getApplicationContext();
    AutowiredAnnotationBeanPostProcessor beanPostProcessor =
        new AutowiredAnnotationBeanPostProcessor();
    beanPostProcessor.setBeanFactory(applicationContext.getBeanFactory());
    beanPostProcessor.processInjection(this);
  }
}
