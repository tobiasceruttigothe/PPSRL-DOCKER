import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.paper.controller.LogoController;
import org.paper.dtoCreate.LogoCreateDto;
import org.paper.dtoCreate.LogoUpdateDto;
import org.paper.dtoResponse.LogoResponseDto;
import org.paper.service.LogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogoController.class)
@ContextConfiguration(classes = org.paper.Main.class)
class LogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogoService logoService;

    private UUID usuarioId;
    private LogoResponseDto logoResponseDto;
    private LogoCreateDto logoCreateDto;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();

        logoResponseDto = LogoResponseDto.builder()
                .id(1)
                .nombre("Logo Test")
                .base64Logo("base64string")
                .build();

        logoCreateDto = new LogoCreateDto();
        logoCreateDto.setUsuarioId(usuarioId);
        logoCreateDto.setNombre("Nuevo Logo");
        logoCreateDto.setBase64Logo("base64string");
    }

    @Test
    void crearLogo_DeberiaRetornar201() throws Exception {
        // Arrange
        when(logoService.crearLogo(any(LogoCreateDto.class))).thenReturn(logoResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/logos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Logo Test"));

        verify(logoService).crearLogo(any(LogoCreateDto.class));
    }

    @Test
    void obtenerLogosPorUsuario_DeberiaRetornar200() throws Exception {
        // Arrange
        when(logoService.obtenerLogosPorUsuario(usuarioId))
                .thenReturn(Collections.singletonList(logoResponseDto));

        // Act & Assert
        mockMvc.perform(get("/api/logos/usuario/{usuarioId}", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Logo Test"));

        verify(logoService).obtenerLogosPorUsuario(usuarioId);
    }

    @Test
    void obtenerLogoPorId_DeberiaRetornar200() throws Exception {
        // Arrange
        when(logoService.obtenerLogoPorId(1)).thenReturn(logoResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/logos/{logoId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Logo Test"));

        verify(logoService).obtenerLogoPorId(1);
    }

    @Test
    void actualizarLogo_DeberiaRetornar200() throws Exception {
        // Arrange
        LogoUpdateDto updateDto = new LogoUpdateDto();
        updateDto.setNombre("Logo Actualizado");

        when(logoService.actualizarLogo(eq(1), any(LogoUpdateDto.class)))
                .thenReturn(logoResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/logos/{logoId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        verify(logoService).actualizarLogo(eq(1), any(LogoUpdateDto.class));
    }

    @Test
    void eliminarLogo_DeberiaRetornar200() throws Exception {
        // Arrange
        doNothing().when(logoService).eliminarLogo(1);

        // Act & Assert
        mockMvc.perform(delete("/api/logos/{logoId}", 1))
                .andExpect(status().isOk());

        verify(logoService).eliminarLogo(1);
    }

    @Test
    void contarLogosPorUsuario_DeberiaRetornar200() throws Exception {
        // Arrange
        when(logoService.contarLogosPorUsuario(usuarioId)).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/logos/usuario/{usuarioId}/count", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));

        verify(logoService).contarLogosPorUsuario(usuarioId);
    }
}