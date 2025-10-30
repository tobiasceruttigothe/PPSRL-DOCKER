import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.paper.controller.MaterialController;
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.dtoCreate.MaterialUpdateDto;
import org.paper.dtoResponse.MaterialResponseDto;
import org.paper.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaterialController.class)
@ContextConfiguration(classes = org.paper.Main.class)
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaterialService materialService;

    private MaterialResponseDto materialResponseDto;
    private MaterialCreateDto materialCreateDto;

    @BeforeEach
    void setUp() {
        materialResponseDto = MaterialResponseDto.builder()
                .id(1)
                .nombre("Papel Kraft")
                .build();

        materialCreateDto = new MaterialCreateDto();
        materialCreateDto.setNombre("Papel Blanco");
    }

    @Test
    void obtenerMateriales_DeberiaRetornar200() throws Exception {
        // Arrange
        when(materialService.findAll()).thenReturn(Collections.singletonList(materialResponseDto));

        // Act & Assert
        mockMvc.perform(get("/api/materiales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Papel Kraft"));

        verify(materialService).findAll();
    }

    @Test
    void obtenerMaterialPorId_DeberiaRetornar200() throws Exception {
        // Arrange
        when(materialService.findById(1)).thenReturn(materialResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/materiales/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Papel Kraft"));

        verify(materialService).findById(1);
    }

    @Test
    void crearMaterial_DeberiaRetornar201() throws Exception {
        // Arrange
        when(materialService.save(any(MaterialCreateDto.class)))
                .thenReturn(materialResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/materiales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(materialCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Papel Kraft"));

        verify(materialService).save(any(MaterialCreateDto.class));
    }

    @Test
    void actualizarMaterial_DeberiaRetornar200() throws Exception {
        // Arrange
        MaterialUpdateDto updateDto = new MaterialUpdateDto();
        updateDto.setNombre("Papel Reciclado");

        when(materialService.update(eq(1), any(MaterialUpdateDto.class)))
                .thenReturn(materialResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/materiales/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        verify(materialService).update(eq(1), any(MaterialUpdateDto.class));
    }

    @Test
    void eliminarMaterial_DeberiaRetornar200() throws Exception {
        // Arrange
        doNothing().when(materialService).deleteById(1);

        // Act & Assert
        mockMvc.perform(delete("/api/materiales/{id}", 1))
                .andExpect(status().isOk());

        verify(materialService).deleteById(1);
    }

    @Test
    void buscarMateriales_DeberiaRetornar200() throws Exception {
        // Arrange
        when(materialService.searchByNombre("Papel"))
                .thenReturn(Collections.singletonList(materialResponseDto));

        // Act & Assert
        mockMvc.perform(get("/api/materiales/search")
                        .param("nombre", "Papel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Papel Kraft"));

        verify(materialService).searchByNombre("Papel");
    }
}