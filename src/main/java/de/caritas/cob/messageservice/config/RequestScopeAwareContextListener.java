package de.caritas.cob.messageservice.config;

import javax.servlet.annotation.WebListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;

@Configuration
@WebListener
public class RequestScopeAwareContextListener extends RequestContextListener {

}
