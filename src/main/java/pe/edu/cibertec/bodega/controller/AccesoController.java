package pe.edu.cibertec.bodega.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccesoController {
    @GetMapping("/login") public String login() { return "seguridad/login"; }
    @GetMapping("/403") public String prohibido() { return "seguridad/403"; }
}
