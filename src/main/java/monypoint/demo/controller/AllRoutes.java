package monypoint.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;

// @RestController
@Controller
public class AllRoutes {

    @GetMapping("/")
    public String index(){
        return "index";
    }

    @GetMapping("/personal")
    public String personal(){
        return "personal";
    }
    
    
}
