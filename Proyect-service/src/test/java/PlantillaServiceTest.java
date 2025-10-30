import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paper.dtoCreate.PlantillaCreateDto;
import org.paper.dtoCreate.PlantillaUpdateDto;
import org.paper.dtoResponse.PlantillaResponseDto;
import org.paper.dtoResponse.PlantillaSimpleDto;
import org.paper.entity.Material;
import org.paper.entity.Plantilla;
import org.paper.entity.TipoBolsa;
import org.paper.entity.Usuario;
import org.paper.exception.EntityNotFoundException;
import org.paper.repository.MaterialRepository;
import org.paper.repository.PlantillaRepository;
import org.paper.repository.TipoBolsaRepository;
import org.paper.repository.UsuarioRepository;
import org.paper.service.PlantillaService;
import org.paper.util.Base64ValidatorUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantillaServiceTest {

    @Mock
    private PlantillaRepository plantillaRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private TipoBolsaRepository tipoBolsaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Base64ValidatorUtil base64Validator;

    @InjectMocks
    private PlantillaService plantillaService;

    private Plantilla plantilla;
    private Material material;
    private TipoBolsa tipoBolsa;
    private PlantillaCreateDto plantillaCreateDto;

    @BeforeEach
    void setUp() {
        material = new Material();
        material.setId(1);
        material.setNombre("Papel Kraft");

        tipoBolsa = new TipoBolsa();
        tipoBolsa.setId(1);
        tipoBolsa.setNombre("Bolsa con Asa");

        plantilla = new Plantilla();
        plantilla.setId(1);
        plantilla.setNombre("Plantilla Test");
        plantilla.setMaterial(material);
        plantilla.setTipoBolsa(tipoBolsa);
        plantilla.setBase64Plantilla("base64string");
        plantilla.setAncho(20.0f);
        plantilla.setAlto(30.0f);
        plantilla.setProfundidad(10.0f);

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
    void findAll_DeberiaRetornarListaDePlantillas() {
        // Arrange
        when(plantillaRepository.findAll()).thenReturn(Arrays.asList(plantilla));

        // Act
        List<PlantillaSimpleDto> result = plantillaService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Plantilla Test", result.get(0).getNombre());
    }

    @Test
    void findById_DeberiaRetornarPlantilla() {
        // Arrange
        when(plantillaRepository.findById(1)).thenReturn(Optional.of(plantilla));

        // Act
        PlantillaResponseDto result = plantillaService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals("Plantilla Test", result.getNombre());
        assertEquals("base64string", result.getBase64Plantilla());
    }

    @Test
    void save_DeberiaCrearPlantilla() {
        // Arrange
        when(materialRepository.findById(1)).thenReturn(Optional.of(material));
        when(tipoBolsaRepository.findById(1)).thenReturn(Optional.of(tipoBolsa));
        doNothing().when(base64Validator).validateBase64ForPlantillaOrDiseno(anyString(), anyString());
        when(plantillaRepository.save(any(Plantilla.class))).thenReturn(plantilla);

        // Act
        PlantillaResponseDto result = plantillaService.save(plantillaCreateDto);

        // Assert
        assertNotNull(result);
        verify(plantillaRepository).save(any(Plantilla.class));
        verify(base64Validator).validateBase64ForPlantillaOrDiseno(anyString(), anyString());
    }

    @Test
    void save_DeberiaLanzarExcepcion_CuandoMaterialNoExiste() {
        // Arrange
        when(materialRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> plantillaService.save(plantillaCreateDto));
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void update_DeberiaActualizarPlantilla() {
        // Arrange
        PlantillaUpdateDto updateDto = new PlantillaUpdateDto();
        updateDto.setNombre("Plantilla Actualizada");
        updateDto.setMaterialId(1);
        updateDto.setTipoBolsaId(1);
        updateDto.setAncho(25.0f);
        updateDto.setAlto(35.0f);
        updateDto.setProfundidad(15.0f);

        when(plantillaRepository.findById(1)).thenReturn(Optional.of(plantilla));
        when(materialRepository.findById(1)).thenReturn(Optional.of(material));
        when(tipoBolsaRepository.findById(1)).thenReturn(Optional.of(tipoBolsa));
        when(plantillaRepository.save(any(Plantilla.class))).thenReturn(plantilla);

        // Act
        PlantillaResponseDto result = plantillaService.update(1, updateDto);

        // Assert
        assertNotNull(result);
        verify(plantillaRepository).save(any(Plantilla.class));
    }

    @Test
    void deleteById_DeberiaEliminarPlantilla() {
        // Arrange
        when(plantillaRepository.existsById(1)).thenReturn(true);
        doNothing().when(plantillaRepository).deleteById(1);

        // Act
        plantillaService.deleteById(1);

        // Assert
        verify(plantillaRepository).deleteById(1);
    }

    @Test
    void habilitarPlantillaParaUsuario_DeberiaHabilitarCorrectamente() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(plantillaRepository.findById(1)).thenReturn(Optional.of(plantilla));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.save(any(Plantilla.class))).thenReturn(plantilla);

        // Act
        plantillaService.habilitarPlantillaParaUsuario(1, usuarioId);

        // Assert
        verify(plantillaRepository).save(any(Plantilla.class));
    }
}