import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.dtoCreate.MaterialUpdateDto;
import org.paper.dtoResponse.MaterialResponseDto;
import org.paper.entity.Material;
import org.paper.exception.DuplicateEntityException;
import org.paper.exception.EntityNotFoundException;
import org.paper.repository.MaterialRepository;
import org.paper.service.MaterialService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialService materialService;

    private Material material;
    private MaterialCreateDto materialCreateDto;

    @BeforeEach
    void setUp() {
        material = new Material();
        material.setId(1);
        material.setNombre("Papel Kraft");

        materialCreateDto = new MaterialCreateDto();
        materialCreateDto.setNombre("Papel Blanco");
    }

    @Test
    void findAll_DeberiaRetornarListaDeMateriales() {
        // Arrange
        when(materialRepository.findAll()).thenReturn(Arrays.asList(material));

        // Act
        List<MaterialResponseDto> result = materialService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Papel Kraft", result.get(0).getNombre());
    }

    @Test
    void findById_DeberiaRetornarMaterial() {
        // Arrange
        when(materialRepository.findById(1)).thenReturn(Optional.of(material));

        // Act
        MaterialResponseDto result = materialService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals("Papel Kraft", result.getNombre());
    }

    @Test
    void findById_DeberiaLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        when(materialRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> materialService.findById(999));
    }

    @Test
    void save_DeberiaCrearMaterial() {
        // Arrange
        when(materialRepository.existsByNombreIgnoreCase(anyString())).thenReturn(false);
        when(materialRepository.save(any(Material.class))).thenReturn(material);

        // Act
        MaterialResponseDto result = materialService.save(materialCreateDto);

        // Assert
        assertNotNull(result);
        verify(materialRepository).save(any(Material.class));
    }

    @Test
    void save_DeberiaLanzarExcepcion_CuandoNombreDuplicado() {
        // Arrange
        when(materialRepository.existsByNombreIgnoreCase("Papel Blanco")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEntityException.class, () -> materialService.save(materialCreateDto));
        verify(materialRepository, never()).save(any());
    }

    @Test
    void update_DeberiaActualizarMaterial() {
        // Arrange
        MaterialUpdateDto updateDto = new MaterialUpdateDto();
        updateDto.setNombre("Papel Reciclado");

        when(materialRepository.findById(1)).thenReturn(Optional.of(material));
        when(materialRepository.existsByNombreIgnoreCaseAndIdNot(anyString(), anyInt())).thenReturn(false);
        when(materialRepository.save(any(Material.class))).thenReturn(material);

        // Act
        MaterialResponseDto result = materialService.update(1, updateDto);

        // Assert
        assertNotNull(result);
        verify(materialRepository).save(any(Material.class));
    }

    @Test
    void deleteById_DeberiaEliminarMaterial() {
        // Arrange
        when(materialRepository.existsById(1)).thenReturn(true);
        doNothing().when(materialRepository).deleteById(1);

        // Act
        materialService.deleteById(1);

        // Assert
        verify(materialRepository).deleteById(1);
    }

    @Test
    void searchByNombre_DeberiaRetornarResultados() {
        // Arrange
        when(materialRepository.findByNombreContainingIgnoreCase("Papel"))
                .thenReturn(Arrays.asList(material));

        // Act
        List<MaterialResponseDto> result = materialService.searchByNombre("Papel");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}