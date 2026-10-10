package pe.edu.cibertec.bodega.service;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.dto.GastoForm;
import pe.edu.cibertec.bodega.model.Gasto;
import pe.edu.cibertec.bodega.repository.*;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class GastoService {
    private final GastoRepository gastos;
    private final CategoriaGastoRepository categorias;
    private final TiendaRepository tiendas;
    private final UsuarioRepository usuarios;
    private final Validator validator;

    @Transactional
    public Gasto registrar(GastoForm form, Integer idUsuario) {
        if (form == null || !validator.validate(form).isEmpty())
            throw new IllegalArgumentException("Complete los datos del gasto con un importe válido.");
        if (form.getFecha().isAfter(LocalDate.now(ZoneId.of("America/Lima"))))
            throw new IllegalArgumentException("La fecha del gasto no puede ser futura.");
        if (form.getFecha().getYear() < 1000 || form.getFecha().getYear() > 9999)
            throw new IllegalArgumentException("La fecha está fuera del rango permitido.");
        var tienda = tiendas.findById(form.getIdTienda()).filter(t -> Boolean.TRUE.equals(t.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Seleccione una tienda activa."));
        var categoria = categorias.findById(form.getIdCategoriaGasto())
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));
        var usuario = usuarios.findById(idUsuario).filter(u -> Boolean.TRUE.equals(u.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("El usuario no está disponible."));
        if (usuario.getTienda() != null && !usuario.getTienda().getIdTienda().equals(tienda.getIdTienda()))
            throw new IllegalArgumentException("El usuario no pertenece a esta tienda.");
        Gasto gasto = new Gasto();
        gasto.setFecha(form.getFecha());
        gasto.setTienda(tienda);
        gasto.setCategoriaGasto(categoria);
        gasto.setDescripcion(form.getDescripcion() == null ? null : form.getDescripcion().strip());
        gasto.setMonto(form.getMonto().setScale(2));
        gasto.setUsuario(usuario);
        return gastos.save(gasto);
    }
}
