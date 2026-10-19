package pe.edu.cibertec.bodega.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base de las pruebas E2E.
 *
 * A diferencia de los @WebMvcTest del proyecto, esto levanta la aplicacion
 * ENTERA: controladores, servicios, repositorios, Hibernate, la cadena de
 * filtros real de SecurityConfig y una base de datos de verdad. Nada esta
 * mockeado.
 *
 * Eso cubre justo el hueco que dejan los @WebMvcTest: ahi los repositorios son
 * @MockBean, asi que una consulta JPQL mal escrita pasa en verde. Aca cada
 * consulta se ejecuta contra H2.
 *
 * La base es en memoria y se vacia y vuelve a sembrar antes de cada prueba,
 * asi que ninguna depende del orden ni de lo que haya dejado la anterior.
 *
 * OJO con el alcance: el esquema lo genera Hibernate desde las entidades, no
 * los scripts de db/ (son de MySQL y H2 no los traga). Ver el comentario de
 * application-e2e.properties.
 */
@SpringBootTest
@ActiveProfiles("e2e")
public abstract class BaseE2E {

    @Autowired protected WebApplicationContext contexto;
    @Autowired protected FilterChainProxy cadenaDeFiltros;
    @Autowired protected DatosDePrueba datos;

    protected MockMvc mvc;

    @BeforeEach
    void prepararEntorno() {
        // Se arma a mano en vez de usar @AutoConfigureMockMvc para dejar
        // explicito que la cadena de filtros que corre es la real.
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(cadenaDeFiltros)
                .apply(springSecurity())
                .build();

        datos.limpiar();
        datos.sembrar();
    }
}
