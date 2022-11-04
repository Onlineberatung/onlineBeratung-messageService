package de.caritas.cob.messageservice.config.apiclient;

import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceApiControllerFactory {

  private final RestTemplate restTemplate;

  @Value("${user.service.api.liveproxy.url}")
  private String userServiceBasePath;

  public UserControllerApi userControllerApi() {
    var apiClient = new ApiClient(restTemplate).setBasePath(userServiceBasePath);

    return new UserControllerApi(apiClient);
  }
}
