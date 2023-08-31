package de.caritas.cob.messageservice.config.apiclient;

import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceApiControllerFactory {

  @Value("${user.service.api.url}")
  private String userServiceApiUrl;

  @Autowired private RestTemplate restTemplate;

  public UserControllerApi createControllerApi() {
    var apiClient = new ApiClient(restTemplate)
            .setBasePath(this.userServiceApiUrl);
    return new UserControllerApi(apiClient);
  }
}
