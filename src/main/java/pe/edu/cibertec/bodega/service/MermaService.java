package pe.edu.cibertec.bodega.service;
import jakarta.validation.Validator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.dto.MermaForm;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import java.time.*;

@Service @RequiredArgsConstructor
public class MermaService {
    private final MermaRepository mermas;
    private final LoteRepository lotes;
    private final StockRepository stocks;
    private final UsuarioRepository usuarios;
    private final KardexService kardex;
    private final Validator validator;
    private final EntityManager em;
    @Transactional
    public Merma registrar(MermaForm form, Integer idUsuario) {
        if (form == null || !validator.validate(form).isEmpty())
            throw new IllegalArgumentException("Seleccione lote, motivo, cantidad positiva y descripción.");
        var usuario = usuarios.findById(idUsuario).filter(u -> Boolean.TRUE.equals(u.getEstado()))
            .orElseThrow(() -> new IllegalArgumentException("El usuario no está disponible."));
        var referencia = lotes.findById(form.getIdLote())
            .orElseThrow(() -> new IllegalArgumentException("El lote no existe."));
        // Orden compartido con transferencias: stock antes de bloquear el lote.
        var stock = stocks.findParaActualizar(referencia.getProducto().getIdProducto(), referencia.getTienda().getIdTienda())
            .orElseThrow(() -> new IllegalArgumentException("No existe stock para este lote."));
        var lote = lotes.bloquear(form.getIdLote()).orElseThrow(() -> new IllegalArgumentException("El lote no existe."));
        // Recargar: la primera lectura pudo ocurrir antes de esperar el bloqueo del stock.
        em.refresh(lote, LockModeType.PESSIMISTIC_WRITE);
        if (usuario.getTienda() != null && !usuario.getTienda().getIdTienda().equals(lote.getTienda().getIdTienda()))
            throw new IllegalArgumentException("El usuario no pertenece a esta tienda.");
        if (!Boolean.TRUE.equals(lote.getTienda().getEstado()))
            throw new IllegalArgumentException("La tienda está inactiva.");
        if (form.getCantidad() > lote.getDisponible() || form.getCantidad() > stock.getCantidad())
            throw new IllegalArgumentException("La merma supera el saldo del lote o el stock de la tienda.");
        if (form.getMotivo() == Merma.Motivo.VENCIMIENTO && !lote.getVencimiento().isBefore(LocalDate.now(ZoneId.of("America/Lima"))))
            throw new IllegalArgumentException("El lote todavía no está vencido. Vence al finalizar la fecha indicada.");
        int antes = stock.getCantidad();
        stock.setCantidad(antes - form.getCantidad());
        lote.setDisponible(lote.getDisponible() - form.getCantidad());
        stocks.save(stock); lotes.save(lote);
        Merma merma = new Merma(); merma.setLote(lote); merma.setCantidad(form.getCantidad());
        merma.setMotivo(form.getMotivo()); merma.setDescripcion(form.getDescripcion().strip());
        merma.setFecha(LocalDateTime.now(ZoneId.of("America/Lima"))); merma.setUsuario(usuario);
        mermas.saveAndFlush(merma);
        kardex.registrar(lote.getProducto(), lote.getTienda(), "MERMA", antes, -form.getCantidad(),
            "Merma #" + merma.getIdMerma() + " - lote #" + lote.getIdLote(), usuario);
        return merma;
    }
}
