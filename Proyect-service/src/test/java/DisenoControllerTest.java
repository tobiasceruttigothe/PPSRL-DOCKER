import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.paper.controller.DisenoController;
import org.paper.dtoCreate.DisenoCreateDto;
import org.paper.dtoCreate.DisenoUpdateDto;
import org.paper.dtoResponse.DisenoResponseDto;
import org.paper.entity.DisenoStatus;
import org.paper.service.DisenoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DisenoController.class)
@ContextConfiguration(classes = org.paper.Main.class)
class DisenoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DisenoService disenoService;

    private UUID usuarioId;
    private DisenoResponseDto disenoResponseDto;
    private DisenoCreateDto disenoCreateDto;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();

        disenoResponseDto = DisenoResponseDto.builder()
                .id(1)
                .nombre("Diseño Test")
                .descripcion("Descripción Test")
                .status("PROGRESO")
                .base64Diseno("base64string")
                .plantillaId(1)
                .plantillaNombre("Plantilla Test")
                .fechaCreacion(LocalDateTime.now())
                .build();

        disenoCreateDto = new DisenoCreateDto();
        disenoCreateDto.setUsuarioId(usuarioId);
        disenoCreateDto.setPlantillaId(1);
        disenoCreateDto.setNombre("Nuevo Diseño");
        disenoCreateDto.setDescripcion("Nueva Descripción");
        disenoCreateDto.setBase64Diseno("base64string");
    }

    @Test
    void crearDiseno_DeberiaRetornar201() throws Exception {
        // Arrange
        when(disenoService.save(any(DisenoCreateDto.class))).thenReturn(disenoResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/disenos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disenoCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Diseño Test"));

        verify(disenoService).save(any(DisenoCreateDto.class));
    }

    @Test
    void obtenerDisenoPorId_DeberiaRetornar200() throws Exception {
        // Arrange
        when(disenoService.findById(1)).thenReturn(disenoResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/disenos/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Diseño Test"))
                .andExpect(jsonPath("$.data.status").value("PROGRESO"));

        verify(disenoService).findById(1);
    }

    @Test
    void actualizarDiseno_DeberiaRetornar200() throws Exception {
        // Arrange
        DisenoUpdateDto updateDto = new DisenoUpdateDto();
        updateDto.setNombre("Diseño Actualizado");
        updateDto.setDescripcion("Descripción Actualizada");

        when(disenoService.update(eq(1), any(DisenoUpdateDto.class)))
                .thenReturn(disenoResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/disenos/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        verify(disenoService).update(eq(1), any(DisenoUpdateDto.class));
    }

    @Test
    void marcarComoTerminado_DeberiaRetornar200() throws Exception {
        // Arrange
        when(disenoService.marcarComoTerminado(1)).thenReturn(disenoResponseDto);

        // Act & Assert
        mockMvc.perform(patch("/api/disenos/{id}/terminar", 1))
                .andExpect(status().isOk());

        verify(disenoService).marcarComoTerminado(1);
    }

    @Test
    void marcarComoEnProgreso_DeberiaRetornar200() throws Exception {
        // Arrange
        when(disenoService.marcarComoEnProgreso(1)).thenReturn(disenoResponseDto);

        // Act & Assert
        mockMvc.perform(patch("/api/disenos/{id}/reabrir", 1))
                .andExpect(status().isOk());

        verify(disenoService).marcarComoEnProgreso(1);
    }

    @Test
    void eliminarDiseno_DeberiaRetornar200() throws Exception {
        // Arrange
        doNothing().when(disenoService).deleteById(1);

        // Act & Assert
        mockMvc.perform(delete("/api/disenos/{id}", 1))
                .andExpect(status().isOk());

        verify(disenoService).deleteById(1);
    }

    @Test
    void obtenerDisenosPorUsuario_DeberiaRetornar200() throws Exception {
        // Arrange
        when(disenoService.findByUsuario(usuarioId))
                .thenReturn(Collections.singletonList(disenoResponseDto));

        // Act & Assert
        mockMvc.perform(get("/api/disenos/usuario/{usuarioId}", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Diseño Test"));

        verify(disenoService).findByUsuario(usuarioId);
    }

    @Test
    void obtenerDisenosPorUsuarioYEstado_DeberiaRetornar200() throws Exception {
        // Arrange
        when(disenoService.findByUsuarioAndStatus(usuarioId, DisenoStatus.PROGRESO))
                .thenReturn(Collections.singletonList(disenoResponseDto));

        // Act & Assert
        mockMvc.perform(get("/api/disenos/usuario/{usuarioId}/status/{status}",
                        usuarioId, "PROGRESO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PROGRESO"));

        verify(disenoService).findByUsuarioAndStatus(usuarioId, DisenoStatus.PROGRESO);
    }

    @Test
    void contarDisenosPorUsuario_DeberiaRetornar200() throws Exception {
        // Arrange
        when(disenoService.contarDisenosPorUsuario(usuarioId)).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(get("/api/disenos/usuario/{usuarioId}/count", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));

        verify(disenoService).contarDisenosPorUsuario(usuarioId);
    }

    @Test
    void contarDisenosPorUsuarioYEstado_DeberiaRetornar200() throws Exception {
        // Arrange
        when(disenoService.contarDisenosPorUsuarioYEstado(usuarioId, DisenoStatus.PROGRESO))
                .thenReturn(2L);

        // Act & Assert
        mockMvc.perform(get("/api/disenos/usuario/{usuarioId}/count/{status}",
                        usuarioId, "PROGRESO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        verify(disenoService).contarDisenosPorUsuarioYEstado(usuarioId, DisenoStatus.PROGRESO);
    }
}