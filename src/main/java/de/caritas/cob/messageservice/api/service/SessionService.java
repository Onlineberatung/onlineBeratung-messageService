package de.caritas.cob.messageservice.api.service;

import com.google.common.collect.Lists;
import de.caritas.cob.messageservice.api.tenant.TenantContext;
import de.caritas.cob.messageservice.config.apiclient.ApiControllerFactory;
import de.caritas.cob.messageservice.userservice.generated.web.model.GroupSessionListResponseDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/** Service class to provide handle session methods of the UserService. */
@Service
@RequiredArgsConstructor
public class SessionService {

  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;
  private final @NonNull TenantHeaderSupplier tenantHeaderSupplier;
  private final @NonNull ApiControllerFactory clientFactory;


  public GroupSessionListResponseDTO findSessionBelongingToRcGroupId(String rcToken, String rcGroupId) {
    var userControllerApi = clientFactory.userControllerApi();
    addDefaultHeaders(userControllerApi.getApiClient());

    return userControllerApi.getSessionsForGroupOrFeedbackGroupIds(rcToken, Lists.newArrayList(rcGroupId));
  }

  private void addDefaultHeaders(
      de.caritas.cob.messageservice.userservice.generated.ApiClient apiClient) {
    HttpHeaders headers = this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();
    tenantHeaderSupplier.addTenantHeader(headers, TenantContext.getCurrentTenantOption());
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }
}
