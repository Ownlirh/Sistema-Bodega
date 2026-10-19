package pe.edu.cibertec.bodega.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.cibertec.bodega.model.Cliente;
import pe.edu.cibertec.bodega.repository.ClienteRepository;

@Controller
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // Listar clientes
    @GetMapping("/clientes")
    public String listar(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll());
        return "clientes/lista";
    }

    // Mostrar formulario para registrar un cliente
    @GetMapping("/clientes/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/formulario";
    }

    // Mostrar formulario para editar un cliente
    @GetMapping("/clientes/editar/{idCliente}")
    public String editar(
            @PathVariable("idCliente") Integer idCliente,
            Model model,
            RedirectAttributes atributos) {

        Optional<Cliente> resultado =
                clienteRepository.findById(idCliente);

        if (resultado.isEmpty()) {
            atributos.addFlashAttribute(
                    "error", "No se encontró el cliente.");
            return "redirect:/clientes";
        }

        model.addAttribute("cliente", resultado.get());
        return "clientes/formulario";
    }

    // Guardar un cliente nuevo o actualizar uno existente
    @PostMapping("/clientes/guardar")
    public String guardar(
            @ModelAttribute("cliente") Cliente cliente,
            RedirectAttributes atributos) {

        // Registrar un cliente nuevo
        if (cliente.getIdCliente() == null) {
            cliente.setEstado(true);
            clienteRepository.save(cliente);

            atributos.addFlashAttribute(
                    "mensaje", "Cliente registrado correctamente.");

            return "redirect:/clientes";
        }

        // Buscar el cliente que se va a actualizar
        Optional<Cliente> resultado =
                clienteRepository.findById(cliente.getIdCliente());

        if (resultado.isEmpty()) {
            atributos.addFlashAttribute(
                    "error", "No se encontró el cliente.");
            return "redirect:/clientes";
        }

        // Actualizar los datos sin modificar su estado
        Cliente existente = resultado.get();

        existente.setNombres(cliente.getNombres());
        existente.setApellidos(cliente.getApellidos());
        existente.setDni(cliente.getDni());
        existente.setTelefono(cliente.getTelefono());
        existente.setDireccion(cliente.getDireccion());

        clienteRepository.save(existente);

        atributos.addFlashAttribute(
                "mensaje", "Cliente actualizado correctamente.");

        return "redirect:/clientes";
    }

    // Activar o desactivar un cliente sin eliminarlo
    @PostMapping("/clientes/{idCliente}/estado")
    public String cambiarEstado(
            @PathVariable("idCliente") Integer idCliente,
            @RequestParam("estado") boolean estado,
            RedirectAttributes atributos) {

        Optional<Cliente> resultado =
                clienteRepository.findById(idCliente);

        if (resultado.isEmpty()) {
            atributos.addFlashAttribute(
                    "error", "No se encontró el cliente.");
            return "redirect:/clientes";
        }

        Cliente existente = resultado.get();
        existente.setEstado(estado);

        clienteRepository.save(existente);

        atributos.addFlashAttribute(
                "mensaje",
                estado
                    ? "Cliente activado correctamente."
                    : "Cliente desactivado correctamente.");

        return "redirect:/clientes";
    }
}