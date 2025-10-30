import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.paper.controller.TipoBolsaController;
import org.paper.dtoCreate.TipoBolsaCreateDto;
import org.paper.dtoCreate.TipoBolsaUpdateDto;
import org.paper.dtoResponse.TipoBolsaResponseDto;
import org.paper.service.TipoBolsaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TipoBolsaController.class)
@ContextConfiguration(classes = org.paper.Main.class)
class TipoBolsaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TipoBolsaService tipoBolsaService;

    private TipoBolsaResponseDto tipoBolsaResponseDto;
    private TipoBolsaCreateDto tipoBolsaCreateDto;

    @BeforeEach
    void setUp() {
        tipoBolsaResponseDto = TipoBolsaResponseDto.builder()
                .id(1)
                .nombre("Bolsa con Asa")
                .build();

        tipoBolsaCreateDto = new TipoBolsaCreateDto();
        tipoBolsaCreateDto.setNombre("Bolsa Sin Asa");
    }

    @Test
    void obtenerTiposBolsas_DeberiaRetornar200ConLista() throws Exception {
        // Arrange
        when(tipoBolsaService.findAll())
                .thenReturn(Collections.singletonList(tipoBolsaResponseDto));

        // Act & Assert
        mockMvc.perform(get("/api/tipos-bolsa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nombre").value("Bolsa con Asa"))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 tipos de bolsa"));

        verify(tipoBolsaService).findAll();
    }

    @Test
    void obtenerTiposBolsas_DeberiaRetornar200ConListaVacia() throws Exception {
        // Arrange
        when(tipoBolsaService.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tipos-bolsa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("Se encontraron 0 tipos de bolsa"));

        verify(tipoBolsaService).findAll();
    }

    @Test
    void obtenerTipoBolsaPorId_DeberiaRetornar200() throws Exception {
        // Arrange
        when(tipoBolsaService.findById(1)).thenReturn(tipoBolsaResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/tipos-bolsa/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Bolsa con Asa"));

        verify(tipoBolsaService).findById(1);
    }

    @Test
    void crearTipoBolsa_DeberiaRetornar201() throws Exception {
        // Arrange
        when(tipoBolsaService.save(any(TipoBolsaCreateDto.class)))
                .thenReturn(tipoBolsaResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/tipos-bolsa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tipoBolsaCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Bolsa con Asa"))
                .andExpect(jsonPath("$.message").value("Tipo de bolsa creado exitosamente"));

        verify(tipoBolsaService).save(any(TipoBolsaCreateDto.class));
    }

    @Test
    void crearTipoBolsa_DeberiaRetornar400CuandoNombreVacio() throws Exception {
        // Arrange
        TipoBolsaCreateDto dtoInvalido = new TipoBolsaCreateDto();
        dtoInvalido.setNombre(""); // Nombre vacío

        // Act & Assert
        mockMvc.perform(post("/api/tipos-bolsa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());

        verify(tipoBolsaService, never()).save(any());
    }

    @Test
    void actualizarTipoBolsa_DeberiaRetornar200() throws Exception {
        // Arrange
        TipoBolsaUpdateDto updateDto = new TipoBolsaUpdateDto();
        updateDto.setNombre("Bolsa Americana");

        when(tipoBolsaService.update(eq(1), any(TipoBolsaUpdateDto.class)))
                .thenReturn(tipoBolsaResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/tipos-bolsa/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tipo de bolsa actualizado exitosamente"));

        verify(tipoBolsaService).update(eq(1), any(TipoBolsaUpdateDto.class));
    }

    @Test
    void eliminarTipoBolsa_DeberiaRetornar200() throws Exception {
        // Arrange
        doNothing().when(tipoBolsaService).deleteById(1);

        // Act & Assert
        mockMvc.perform(delete("/api/tipos-bolsa/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tipo de bolsa eliminado exitosamente"));

        verify(tipoBolsaService).deleteById(1);
    }

    @Test
    void buscarTiposBolsa_DeberiaRetornar200() throws Exception {
        // Arrange
        when(tipoBolsaService.searchByNombre("asa"))
                .thenReturn(Collections.singletonList(tipoBolsaResponseDto));

        // Act & Assert
        mockMvc.perform(get("/api/tipos-bolsa/search")
                        .param("nombre", "asa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Bolsa con Asa"))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 tipos de bolsa"));

        verify(tipoBolsaService).searchByNombre("asa");
    }

    @Test
    void buscarTiposBolsa_DeberiaRetornar200ConListaVacia() throws Exception {
        // Arrange
        when(tipoBolsaService.searchByNombre("noexiste"))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tipos-bolsa/search")
                        .param("nombre", "noexiste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("Se encontraron 0 tipos de bolsa"));

        verify(tipoBolsaService).searchByNombre("noexiste");
    }
}