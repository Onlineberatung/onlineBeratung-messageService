package de.caritas.cob.messageservice.api.service;

import de.caritas.cob.messageservice.config.CacheManagerConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import de.caritas.cob.messageservice.tenantservice.generated.web.TenantControllerApi;
import de.caritas.cob.messageservice.tenantservice.generated.web.model.RestrictedTenantDTO;

@Service
@RequiredArgsConstructor
public class TenantService {

  private final @NonNull TenantControllerApi tenantControllerApi;

  @Cacheable(cacheNames = CacheManagerConfig.TENANT_CACHE, key = "#subdomain")
  public RestrictedTenantDTO getRestrictedTenantDataBySubdomain(String subdomain) {
    return tenantControllerApi.getRestrictedTenantDataBySubdomainWithHttpInfo(subdomain).getBody();
  }

}
