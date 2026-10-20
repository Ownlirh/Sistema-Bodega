package pe.edu.cibertec.bodega.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.cibertec.bodega.model.Cliente;
import pe.edu.cibertec.bodega.model.Fiado;
import pe.edu.cibertec.bodega.repository.ClienteRepository;
import pe.edu.cibertec.bodega.repository.FiadoRepository;
import pe.edu.cibertec.bodega.repository.PagoFiadoRepository;
import pe.edu.cibertec.bodega.service.FiadoService;

@Controller
public class FiadoController {

    private final FiadoService fiadoService;
    private final FiadoRepository fiadoRepository;
    private final ClienteRepository clienteRepository;
    private final PagoFiadoRepository pagoFiadoRepository;

    public FiadoController(
            FiadoService fiadoService,
            FiadoRepository fiadoRepository,
            ClienteRepository clienteRepository,
            PagoFiadoRepository pagoFiadoRepository) {

        this.fiadoService = fiadoService;
        this.fiadoRepository = fiadoRepository;
        this.clienteRepository = clienteRepository;
        this.pagoFiadoRepository = pagoFiadoRepository;
    }

    // 1. Listar todos los fiados.
    @GetMapping("/fiados")
    public String listarFiados(Model model) {

        model.addAttribute("fiados",
                fiadoRepository.findAllWithCliente());

        return "fiados/lista";
    }

    // 2. Mostrar el formulario para registrar un fiado.
    @GetMapping("/fiados/nuevo")
    public String nuevoFiado(Model model) {

        model.addAttribute("clientes", clienteRepository.findAll());

        return "fiados/formulario";
    }

    // 3. Registrar un nuevo fiado.
    @PostMapping("/fiados/guardar")
    public String guardarFiado(
            @RequestParam("idCliente") Integer idCliente,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("montoInicial") BigDecimal montoInicial,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            fiadoService.registrarFiado(
                    idCliente,
                    descripcion,
                    montoInicial);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Fiado registrado correctamente.");

            return "redirect:/fiados";

        } catch (IllegalArgumentException e) {

            model.addAttribute("clientes", clienteRepository.findAll());
            model.addAttribute("idClienteSeleccionado", idCliente);
            model.addAttribute("descripcion", descripcion);
            model.addAttribute("montoInicial", montoInicial);
            model.addAttribute("error", e.getMessage());

            return "fiados/formulario";
        }
    }

    // 4. Mostrar el detalle del fiado y su historial de pagos.
    @GetMapping("/fiados/{idFiado}/pagos")
    public String verPagos(
            @PathVariable("idFiado") Integer idFiado,
            Model model) {

        Fiado fiado = fiadoRepository
                .findByIdWithCliente(idFiado)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El fiado no existe."));

        model.addAttribute("fiado", fiado);

        model.addAttribute(
                "pagos",
                pagoFiadoRepository
                        .findByFiadoIdFiadoOrderByFechaDesc(idFiado));

        return "fiados/pagos";
    }

    // 5. Registrar un pago.
    @PostMapping("/fiados/{idFiado}/pagos/guardar")
    public String guardarPago(
            @PathVariable("idFiado") Integer idFiado,
            @RequestParam("monto") BigDecimal monto,
            @RequestParam(value = "observacion", required = false)
                    String observacion,
            RedirectAttributes redirectAttributes) {

        try {
            fiadoService.registrarPago(
                    idFiado,
                    monto,
                    observacion);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Pago registrado correctamente.");

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage());
        }

        return "redirect:/fiados/" + idFiado + "/pagos";
    }

    // 6. Consultar todas las deudas que continúan pendientes.
    @GetMapping("/fiados/deudores")
    public String listarDeudores(Model model) {

        model.addAttribute(
                "fiados",
                fiadoRepository.findAllDeudasPendientes());

        return "fiados/deudores";
    }

    // 7. Ver el estado de cuenta de un cliente.
    @GetMapping("/fiados/cliente/{idCliente}/estado-cuenta")
    public String estadoCuentaCliente(
            @PathVariable("idCliente") Integer idCliente,
            Model model) {

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El cliente no existe."));

        List<Fiado> fiados =
                fiadoRepository.findAllByClienteWithCliente(idCliente);

        BigDecimal totalPendiente = fiados.stream()
                .map(Fiado::getSaldoPendiente)
                .filter(saldo -> saldo != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cliente", cliente);
        model.addAttribute("fiados", fiados);
        model.addAttribute("totalPendiente", totalPendiente);

        return "fiados/estado-cuenta";
    }
}