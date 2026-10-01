package pe.edu.cibertec.bodega.service;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import pe.edu.cibertec.bodega.dto.LoteForm;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service @RequiredArgsConstructor
public class LoteService {
    private final LoteRepository lotes;
    private final StockRepository stocks;
    private final UsuarioRepository usuarios;
    private final Validator validator;

    @Transactional
    public Lote registrar(LoteForm form, Integer idUsuario) {
        if (form == null || !validator.validate(form).isEmpty())
            throw new IllegalArgumentException("Complete los datos del lote con una cantidad positiva.");
        if (form.getVencimiento().getYear() < 1000 || form.getVencimiento().getYear() > 9999)
            throw new IllegalArgumentException("La fecha de vencimiento está fuera del rango permitido.");
        var usuario = usuarios.findById(idUsuario).filter(u -> Boolean.TRUE.equals(u.getEstado()))
            .orElseThrow(() -> new IllegalArgumentException("El usuario no está disponible."));
        var stock = stocks.findParaActualizar(form.getIdProducto(), form.getIdTienda())
            .orElseThrow(() -> new IllegalArgumentException("No existe stock de ese producto en la tienda. Registre primero la compra."));
        if (!Boolean.TRUE.equals(stock.getTienda().getEstado()))
            throw new IllegalArgumentException("La tienda está inactiva.");
        if (usuario.getTienda() != null && !usuario.getTienda().getIdTienda().equals(form.getIdTienda()))
            throw new IllegalArgumentException("El usuario no pertenece a esta tienda.");
        long libre = (long) stock.getCantidad() - lotes.cantidadAsignada(form.getIdProducto(), form.getIdTienda());
        if (form.getCantidad() > libre)
            throw new IllegalArgumentException("Cantidad superior al stock sin lote. Disponible para asignar: " + Math.max(0, libre) + ".");
        Lote lote = crear(stock.getProducto(), stock.getTienda(), form.getCodigo().strip(),
                form.getVencimiento(), form.getCantidad(), usuario);
        return lotes.save(lote);
    }

    /** El llamador ya debe tener bloqueados ambos stocks dentro de su transacción. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void trasladar(Producto producto, Tienda origen, Tienda destino, int cantidad, Usuario usuario) {
        int restante = cantidad;
        for (var lote : lotes.bloquearDisponibles(producto.getIdProducto(), origen.getIdTienda())) {
            if (restante == 0) break;
            int mover = Math.min(restante, lote.getDisponible());
            lote.setDisponible(lote.getDisponible() - mover);
            lotes.save(lote);
            lotes.save(crear(producto, destino, lote.getCodigo(), lote.getVencimiento(), mover, usuario));
            restante -= mover;
        }
        // El resto, si existe, corresponde a stock todavía sin lote y sigue sin lote en destino.
    }

    private Lote crear(Producto producto, Tienda tienda, String codigo, java.time.LocalDate vence,
                       int cantidad, Usuario usuario) {
        Lote l = new Lote(); l.setProducto(producto); l.setTienda(tienda); l.setCodigo(codigo);
        l.setVencimiento(vence); l.setCantidadInicial(cantidad); l.setDisponible(cantidad);
        l.setFechaRegistro(LocalDateTime.now(ZoneId.of("America/Lima"))); l.setUsuario(usuario);
        return l;
    }
}
