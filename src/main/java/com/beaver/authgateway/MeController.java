package com.beaver.authgateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

@RestController
class MeController {

    @GetMapping("/")
    public String index(Principal principal) {
        return principal.getName();
    }
}