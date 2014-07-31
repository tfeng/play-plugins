package me.tfeng.play.security;

import org.springframework.security.core.context.SecurityContext;

import play.cache.Cache;

public class CacheSecurityContextStore implements SecurityContextStore {

  @Override
  public SecurityContext load(String id) {
    return (SecurityContext) Cache.get(id);
  }

  @Override
  public void remove(String id) {
    Cache.remove(id);
  }

  @Override
  public void save(String id, SecurityContext securityContext, int expirationInSeconds) {
    Cache.set(id, securityContext, expirationInSeconds);
  }
}
