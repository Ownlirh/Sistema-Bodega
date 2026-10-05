(() => {
    const cuerpo = document.getElementById('lineas');
    const agregar = document.getElementById('agregar');
    const actualizar = () => {
        let total = 0;
        [...cuerpo.rows].forEach((fila, indice) => {
            fila.querySelectorAll('[name]').forEach(campo => {
                campo.name = campo.name.replace(/lineas\[\d+\]/, `lineas[${indice}]`);
                campo.id = campo.name.replace(/[.\[\]]/g, '_');
            });
            const cantidad = Number(fila.querySelector('[name$=".cantidad"]').value) || 0;
            const costo = Number(fila.querySelector('[name$=".costo"]').value) || 0;
            const subtotal = cantidad * Math.round(costo * 100);
            fila.querySelector('.subtotal').textContent = (subtotal / 100).toFixed(2);
            total += subtotal;
            fila.querySelector('.quitar').disabled = cuerpo.rows.length === 1;
        });
        document.getElementById('total').textContent = (total / 100).toFixed(2);
        agregar.disabled = cuerpo.rows.length >= 100;
    };
    agregar.addEventListener('click', () => {
        if (cuerpo.rows.length >= 100) return;
        const fila = cuerpo.rows[0].cloneNode(true);
        fila.querySelectorAll('input, select').forEach(campo => campo.value = '');
        cuerpo.appendChild(fila);
        actualizar();
        fila.querySelector('select').focus();
    });
    cuerpo.addEventListener('click', evento => {
        if (evento.target.classList.contains('quitar') && cuerpo.rows.length > 1) {
            evento.target.closest('tr').remove();
            actualizar();
        }
    });
    cuerpo.addEventListener('input', actualizar);
    document.getElementById('form-compra').addEventListener('submit', () => {
        document.getElementById('guardar').disabled = true;
    });
    actualizar();
})();
