package growup.spring.springserver.login.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    @GetMapping("/admin/resources")
    public String getAdminResources() {
        return "ADMIN";
    }

    @GetMapping("/user/resources")
    public String getUserResources() {
        return "USER ";
    }

    @GetMapping("/public/resources")
    public String getPublicResources() {
        return "PUBLIC ";
    }
}