import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.paper.controller.PlantillaController;
import org.paper.dtoCreate.PlantillaCreateDto;
import org.paper.dtoCreate.PlantillaUpdateDto;
import org.paper.dtoResponse.MaterialResponseDto;
import org.paper.dtoResponse.PlantillaResponseDto;
import org.paper.dtoResponse.PlantillaSimpleDto;
import org.paper.dtoResponse.TipoBolsaResponseDto;
import org.paper.service.PlantillaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlantillaController.class)
@ContextConfiguration(classes = org.paper.Main.class)
class PlantillaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlantillaService plantillaService;

    private PlantillaSimpleDto plantillaSimpleDto;
    private PlantillaResponseDto plantillaResponseDto;
    private PlantillaCreateDto plantillaCreateDto;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();

        plantillaSimpleDto = PlantillaSimpleDto.builder()
                .id(1)
                .nombre("Plantilla Test")
                .materialNombre("Papel Kraft")
                .tipoBolsaNombre("Bolsa con Asa")
                .ancho(20.0f)
                .alto(30.0f)
                .profundidad(10.0f)
                .build();

        MaterialResponseDto material = MaterialResponseDto.builder()
                .id(1)
                .nombre("Papel Kraft")
                .build();

        TipoBolsaResponseDto tipoBolsa = TipoBolsaResponseDto.builder()
                .id(1)
                .nombre("Bolsa con Asa")
                .build();

        plantillaResponseDto = PlantillaResponseDto.builder()
                .id(1)
                .nombre("Plantilla Test")
                .base64Plantilla("base64string")
                .material(material)
                .tipoBolsa(tipoBolsa)
                .ancho(20.0f)
                .alto(30.0f)
                .profundidad(10.0f)
                .build();

        plantillaCreateDto = new PlantillaCreateDto();
        plantillaCreateDto.setNombre("Nueva Plantilla");
        plantillaCreateDto.setMaterialId(1);
        plantillaCreateDto.setTipoBolsaId(1);
        plantillaCreateDto.setBase64Plantilla("base64string");
        plantillaCreateDto.setAncho(20.0f);
        plantillaCreateDto.setAlto(30.0f);
        plantillaCreateDto.setProfundidad(10.0f);
    }

    @Test
    void obtenerPlantillas_DeberiaRetornar200ConListaSinBase64() throws Exception {
        // Arrange
        when(plantillaService.findAll())
                .thenReturn(Collections.singletonList(plantillaSimpleDto));

        // Act & Assert
        mockMvc.perform(get("/api/plantillas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nombre").value("Plantilla Test"))
                .andExpect(jsonPath("$.data[0].materialNombre").value("Papel Kraft"))
                .andExpect(jsonPath("$.data[0].base64Plantilla").doesNotExist()) // NO debe incluir base64
                .andExpect(jsonPath("$.message").value("Se encontraron 1 plantillas"));

        verify(plantillaService).findAll();
    }

    @Test
    void obtenerPlantillas_DeberiaRetornar200ConListaVacia() throws Exception {
        // Arrange
        when(plantillaService.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/plantillas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("Se encontraron 0 plantillas"));

        verify(plantillaService).findAll();
    }

    @Test
    void obtenerPlantillaPorId_DeberiaRetornar200ConBase64() throws Exception {
        // Arrange
        when(plantillaService.findById(1)).thenReturn(plantillaResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/plantillas/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Plantilla Test"))
                .andExpect(jsonPath("$.data.base64Plantilla").value("base64string")) // SÍ incluye base64
                .andExpect(jsonPath("$.data.material.nombre").value("Papel Kraft"))
                .andExpect(jsonPath("$.data.tipoBolsa.nombre").value("Bolsa con Asa"));

        verify(plantillaService).findById(1);
    }

    @Test
    void crearPlantilla_DeberiaRetornar201() throws Exception {
        // Arrange
        when(plantillaService.save(any(PlantillaCreateDto.class)))
                .thenReturn(plantillaResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/plantillas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plantillaCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Plantilla Test"))
                .andExpect(jsonPath("$.message").value("Plantilla creada exitosamente"));

        verify(plantillaService).save(any(PlantillaCreateDto.class));
    }

    @Test
    void crearPlantilla_DeberiaRetornar400CuandoDimensionesInvalidas() throws Exception {
        // Arrange
        PlantillaCreateDto dtoInvalido = new PlantillaCreateDto();
        dtoInvalido.setNombre("Plantilla Inválida");
        dtoInvalido.setMaterialId(1);
        dtoInvalido.setTipoBolsaId(1);
        dtoInvalido.setBase64Plantilla("base64string");
        dtoInvalido.setAncho(-5.0f); // ❌ Inválido
        dtoInvalido.setAlto(30.0f);
        dtoInvalido.setProfundidad(10.0f);

        // Act & Assert
        mockMvc.perform(post("/api/plantillas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());

        verify(plantillaService, never()).save(any());
    }

    @Test
    void actualizarPlantilla_DeberiaRetornar200() throws Exception {
        // Arrange
        PlantillaUpdateDto updateDto = new PlantillaUpdateDto();
        updateDto.setNombre("Plantilla Actualizada");
        updateDto.setMaterialId(1);
        updateDto.setTipoBolsaId(1);
        updateDto.setAncho(25.0f);
        updateDto.setAlto(35.0f);
        updateDto.setProfundidad(15.0f);

        when(plantillaService.update(eq(1), any(PlantillaUpdateDto.class)))
                .thenReturn(plantillaResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/plantillas/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Plantilla actualizada exitosamente"));

        verify(plantillaService).update(eq(1), any(PlantillaUpdateDto.class));
    }

    @Test
    void eliminarPlantilla_DeberiaRetornar200() throws Exception {
        // Arrange
        doNothing().when(plantillaService).deleteById(1);

        // Act & Assert
        mockMvc.perform(delete("/api/plantillas/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Plantilla eliminada exitosamente"));

        verify(plantillaService).deleteById(1);
    }

    @Test
    void buscarPorMaterial_DeberiaRetornar200() throws Exception {
        // Arrange
        when(plantillaService.findByMaterial(1))
                .thenReturn(Collections.singletonList(plantillaSimpleDto));

        // Act & Assert
        mockMvc.perform(get("/api/plantillas/material/{materialId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].materialNombre").value("Papel Kraft"))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 plantillas"));

        verify(plantillaService).findByMaterial(1);
    }

    @Test
    void buscarPorTipoBolsa_DeberiaRetornar200() throws Exception {
        // Arrange
        when(plantillaService.findByTipoBolsa(1))
                .thenReturn(Collections.singletonList(plantillaSimpleDto));

        // Act & Assert
        mockMvc.perform(get("/api/plantillas/tipo-bolsa/{tipoBolsaId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tipoBolsaNombre").value("Bolsa con Asa"))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 plantillas"));

        verify(plantillaService).findByTipoBolsa(1);
    }

    @Test
    void buscarPlantillas_DeberiaRetornar200() throws Exception {
        // Arrange
        when(plantillaService.searchByNombre("Test"))
                .thenReturn(Collections.singletonList(plantillaSimpleDto));

        // Act & Assert
        mockMvc.perform(get("/api/plantillas/search")
                        .param("nombre", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Plantilla Test"))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 plantillas"));

        verify(plantillaService).searchByNombre("Test");
    }

    @Test
    void habilitarPlantillaParaUsuario_DeberiaRetornar200() throws Exception {
        // Arrange
        doNothing().when(plantillaService).habilitarPlantillaParaUsuario(1, usuarioId);

        // Act & Assert
        mockMvc.perform(post("/api/plantillas/{plantillaId}/habilitar-usuario/{usuarioId}",
                        1, usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Plantilla habilitada exitosamente para el usuario"));

        verify(plantillaService).habilitarPlantillaParaUsuario(1, usuarioId);
    }

    @Test
    void deshabilitarPlantillaParaUsuario_DeberiaRetornar200() throws Exception {
        // Arrange
        doNothing().when(plantillaService).deshabilitarPlantillaParaUsuario(1, usuarioId);

        // Act & Assert
        mockMvc.perform(delete("/api/plantillas/{plantillaId}/deshabilitar-usuario/{usuarioId}",
                        1, usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Plantilla deshabilitada exitosamente para el usuario"));

        verify(plantillaService).deshabilitarPlantillaParaUsuario(1, usuarioId);
    }

    @Test
    void obtenerPlantillasHabilitadasDeUsuario_DeberiaRetornar200() throws Exception {
        // Arrange
        when(plantillaService.findPlantillasHabilitadasParaUsuario(usuarioId))
                .thenReturn(Collections.singletonList(plantillaSimpleDto));

        // Act & Assert
        mockMvc.perform(get("/api/plantillas/usuario/{usuarioId}/habilitadas", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nombre").value("Plantilla Test"))
                .andExpect(jsonPath("$.message").value("El usuario tiene 1 plantillas habilitadas"));

        verify(plantillaService).findPlantillasHabilitadasParaUsuario(usuarioId);
    }

    @Test
    void obtenerPlantillasHabilitadasDeUsuario_DeberiaRetornar200ConListaVacia() throws Exception {
        // Arrange
        when(plantillaService.findPlantillasHabilitadasParaUsuario(usuarioId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/plantillas/usuario/{usuarioId}/habilitadas", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("El usuario tiene 0 plantillas habilitadas"));

        verify(plantillaService).findPlantillasHabilitadasParaUsuario(usuarioId);
    }
}