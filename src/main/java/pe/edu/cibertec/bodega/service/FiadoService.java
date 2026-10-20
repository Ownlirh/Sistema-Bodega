
package pe.edu.cibertec.bodega.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.cibertec.bodega.model.Cliente;
import pe.edu.cibertec.bodega.model.Fiado;
import pe.edu.cibertec.bodega.model.PagoFiado;
import pe.edu.cibertec.bodega.repository.ClienteRepository;
import pe.edu.cibertec.bodega.repository.FiadoRepository;
import pe.edu.cibertec.bodega.repository.PagoFiadoRepository;

@Service
public class FiadoService {

    private final ClienteRepository clienteRepository;
    private final FiadoRepository fiadoRepository;
    private final PagoFiadoRepository pagoFiadoRepository;

    public FiadoService(
            ClienteRepository clienteRepository,
            FiadoRepository fiadoRepository,
            PagoFiadoRepository pagoFiadoRepository) {
        this.clienteRepository = clienteRepository;
        this.fiadoRepository = fiadoRepository;
        this.pagoFiadoRepository = pagoFiadoRepository;
    }

    @Transactional
    public Fiado registrarFiado(
            Integer idCliente,
            String descripcion,
            BigDecimal montoInicial) {

        if (idCliente == null) {
            throw new IllegalArgumentException("Debes seleccionar un cliente.");
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar una descripcion.");
        }

        if (montoInicial == null
                || montoInicial.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto del fiado debe ser mayor que cero.");
        }

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() ->
                        new IllegalArgumentException("El cliente no existe."));

        Fiado fiado = new Fiado();
        fiado.setCliente(cliente);
        fiado.setDescripcion(descripcion.trim());
        fiado.setMontoInicial(montoInicial);
        fiado.setSaldoPendiente(montoInicial);
        fiado.setEstado("PENDIENTE");

        return fiadoRepository.save(fiado);
    }

    @Transactional
    public PagoFiado registrarPago(
            Integer idFiado,
            BigDecimal monto,
            String observacion) {

        if (idFiado == null) {
            throw new IllegalArgumentException("Debes seleccionar un fiado.");
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El pago debe ser mayor que cero.");
        }

        Fiado fiado = fiadoRepository.findById(idFiado)
                .orElseThrow(() ->
                        new IllegalArgumentException("El fiado no existe."));

        BigDecimal saldoActual = fiado.getSaldoPendiente();

        if (saldoActual == null
                || saldoActual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Este fiado ya no tiene deuda pendiente.");
        }

        if (monto.compareTo(saldoActual) > 0) {
            throw new IllegalArgumentException(
                    "El pago no puede superar el saldo pendiente.");
        }

        BigDecimal nuevoSaldo = saldoActual.subtract(monto);

        fiado.setSaldoPendiente(nuevoSaldo);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            fiado.setEstado("PAGADO");
        } else {
            fiado.setEstado("PENDIENTE");
        }

        PagoFiado pago = new PagoFiado();
        pago.setFiado(fiado);
        pago.setMonto(monto);

        if (observacion != null && !observacion.isBlank()) {
            pago.setObservacion(observacion.trim());
        }

        fiadoRepository.save(fiado);
        return pagoFiadoRepository.save(pago);
    }
}
