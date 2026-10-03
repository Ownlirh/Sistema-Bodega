package pe.edu.cibertec.bodega.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import pe.edu.cibertec.bodega.model.Cliente;
import pe.edu.cibertec.bodega.repository.ClienteRepository;

@Controller
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/clientes")
    public String listar(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll());
        return "clientes/lista";
    }

    @GetMapping("/clientes/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/formulario";
    }

    @PostMapping("/clientes/guardar")
    public String guardar(Cliente cliente) {
        clienteRepository.save(cliente);
        return "redirect:/clientes";
    }
}