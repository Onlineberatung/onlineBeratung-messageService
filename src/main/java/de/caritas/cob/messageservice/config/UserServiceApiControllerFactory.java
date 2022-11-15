package de.caritas.cob.messageservice.config;

import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceApiControllerFactory {

  @Value("${user.service.api.url}")
  private String messageServiceApiUrl;

  @Value("${user.service.api.liveproxy.url}")
  private String userServiceApiUrl;

  @Autowired
  private RestTemplate restTemplate;

  private final @NonNull Environment environment;

  public UserControllerApi createControllerApi() {
    var apiClient = new ApiClient(restTemplate).setBasePath(this.messageServiceApiUrl);
    var userControllerApi = new UserControllerApi(apiClient);
    if (!Set.of(environment.getActiveProfiles()).contains("testing")) {
      userControllerApi.getApiClient().setBasePath(userServiceApiUrl);
    }
    return userControllerApi;
  }

}
