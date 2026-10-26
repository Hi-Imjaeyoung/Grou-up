package growup.spring.springserver.login.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    @GetMapping("/api/members/test1")
    public String test() {
        return "test success";
    }
}
