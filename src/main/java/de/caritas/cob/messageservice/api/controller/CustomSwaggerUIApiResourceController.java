package de.caritas.cob.messageservice.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.swagger.web.ApiResourceController;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

@Controller
@ApiIgnore
@RequestMapping(value = "${springfox.docuPath}" + "/swagger-resources")
public class CustomSwaggerUIApiResourceController extends ApiResourceController {

  public static final String SWAGGER_UI_BASE_URL = "/messages/docs";

  public CustomSwaggerUIApiResourceController(SwaggerResourcesProvider swaggerResources) {
    super(swaggerResources, SWAGGER_UI_BASE_URL);
  }

}
