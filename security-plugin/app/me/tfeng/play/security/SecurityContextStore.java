package me.tfeng.play.security;

import org.springframework.security.core.context.SecurityContext;

public interface SecurityContextStore {

  public SecurityContext load(String id) throws Throwable;

  public void remove(String id) throws Throwable;

  public void save(String id, SecurityContext securityContext, int expirationInSeconds)
      throws Throwable;
}
