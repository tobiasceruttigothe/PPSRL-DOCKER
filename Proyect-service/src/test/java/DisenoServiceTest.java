import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paper.dtoCreate.DisenoCreateDto;
import org.paper.dtoCreate.DisenoUpdateDto;
import org.paper.dtoResponse.DisenoResponseDto;
import org.paper.entity.Diseno;
import org.paper.entity.DisenoStatus;
import org.paper.entity.Plantilla;
import org.paper.entity.Usuario;
import org.paper.exception.EntityNotFoundException;
import org.paper.exception.InvalidStateException;
import org.paper.exception.UnauthorizedAccessException;
import org.paper.repository.DisenoRepository;
import org.paper.repository.PlantillaRepository;
import org.paper.repository.UsuarioRepository;
import org.paper.service.DisenoService;
import org.paper.util.Base64ValidatorUtil;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisenoServiceTest {

    @Mock
    private DisenoRepository disenoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlantillaRepository plantillaRepository;

    @Mock
    private Base64ValidatorUtil base64Validator;

    @InjectMocks
    private DisenoService disenoService;

    private UUID usuarioId;
    private Usuario usuario;
    private Plantilla plantilla;
    private Diseno diseno;
    private DisenoCreateDto disenoCreateDto;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(usuarioId);

        plantilla = new Plantilla();
        plantilla.setId(1);
        plantilla.setNombre("Plantilla Test");

        diseno = new Diseno();
        diseno.setId(1);
        diseno.setUsuario(usuario);
        diseno.setPlantilla(plantilla);
        diseno.setNombre("Diseño Test");
        diseno.setDescripcion("Descripción Test");
        diseno.setBase64Diseno("base64string");
        diseno.setStatus(DisenoStatus.PROGRESO);
        diseno.setFechaCreacion(LocalDateTime.now());

        disenoCreateDto = new DisenoCreateDto();
        disenoCreateDto.setUsuarioId(usuarioId);
        disenoCreateDto.setPlantillaId(1);
        disenoCreateDto.setNombre("Nuevo Diseño");
        disenoCreateDto.setDescripcion("Nueva Descripción");
        disenoCreateDto.setBase64Diseno("base64string");
    }

    @Test
    void save_DeberiaCrearDiseno() {
        // Arrange
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findById(1)).thenReturn(Optional.of(plantilla));
        doNothing().when(base64Validator).validateBase64ForPlantillaOrDiseno(anyString(), anyString());
        when(disenoRepository.save(any(Diseno.class))).thenReturn(diseno);

        // Act
        DisenoResponseDto result = disenoService.save(disenoCreateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Diseño Test", result.getNombre());
        verify(disenoRepository).save(any(Diseno.class));
    }

    @Test
    void save_DeberiaLanzarExcepcion_CuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> disenoService.save(disenoCreateDto));
        verify(disenoRepository, never()).save(any());
    }

    @Test
    void findById_DeberiaRetornarDiseno() {
        // Arrange
        when(disenoRepository.findById(1)).thenReturn(Optional.of(diseno));

        // Act
        DisenoResponseDto result = disenoService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals("Diseño Test", result.getNombre());
        assertEquals("PROGRESO", result.getStatus());
    }

    @Test
    void update_DeberiaActualizarDiseno() {
        // Arrange
        DisenoUpdateDto updateDto = new DisenoUpdateDto();
        updateDto.setNombre("Diseño Actualizado");
        updateDto.setDescripcion("Descripción Actualizada");

        when(disenoRepository.findById(1)).thenReturn(Optional.of(diseno));
        when(disenoRepository.save(any(Diseno.class))).thenReturn(diseno);

        // Act
        DisenoResponseDto result = disenoService.update(1, updateDto);

        // Assert
        assertNotNull(result);
        verify(disenoRepository).save(any(Diseno.class));
    }

    @Test
    void update_DeberiaLanzarExcepcion_CuandoDisenoTerminado() {
        // Arrange
        diseno.setStatus(DisenoStatus.TERMINADO);
        DisenoUpdateDto updateDto = new DisenoUpdateDto();
        updateDto.setNombre("Diseño Actualizado");

        when(disenoRepository.findById(1)).thenReturn(Optional.of(diseno));

        // Act & Assert
        assertThrows(InvalidStateException.class, () -> disenoService.update(1, updateDto));
        verify(disenoRepository, never()).save(any());
    }



    @Test
    void deleteById_DeberiaEliminarDiseno() {
        // Arrange
        when(disenoRepository.existsById(1)).thenReturn(true);
        doNothing().when(disenoRepository).deleteById(1);

        // Act
        disenoService.deleteById(1);

        // Assert
        verify(disenoRepository).deleteById(1);
    }

    @Test
    void deleteByUsuario_DeberiaEliminarCorrectamente() {
        // Arrange
        when(disenoRepository.findById(1)).thenReturn(Optional.of(diseno));
        doNothing().when(disenoRepository).delete(diseno);

        // Act
        disenoService.deleteByUsuario(usuarioId, 1);

        // Assert
        verify(disenoRepository).delete(diseno);
    }

    @Test
    void deleteByUsuario_DeberiaLanzarExcepcion_CuandoNoPertenece() {
        // Arrange
        UUID otroUsuarioId = UUID.randomUUID();
        when(disenoRepository.findById(1)).thenReturn(Optional.of(diseno));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class,
                () -> disenoService.deleteByUsuario(otroUsuarioId, 1));
        verify(disenoRepository, never()).delete(any());
    }

    @Test
    void findByUsuario_DeberiaRetornarListaDeDisenos() {
        // Arrange
        when(usuarioRepository.existsById(usuarioId)).thenReturn(true);
        when(disenoRepository.findByUsuarioId(usuarioId)).thenReturn(Arrays.asList(diseno));

        // Act
        List<DisenoResponseDto> result = disenoService.findByUsuario(usuarioId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Diseño Test", result.get(0).getNombre());
    }

    @Test
    void findByUsuarioAndStatus_DeberiaRetornarDisenosFiltrados() {
        // Arrange
        when(usuarioRepository.existsById(usuarioId)).thenReturn(true);
        when(disenoRepository.findByUsuarioIdAndStatus(usuarioId, DisenoStatus.PROGRESO))
                .thenReturn(Arrays.asList(diseno));

        // Act
        List<DisenoResponseDto> result = disenoService.findByUsuarioAndStatus(usuarioId, DisenoStatus.PROGRESO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PROGRESO", result.get(0).getStatus());
    }

    @Test
    void contarDisenosPorUsuario_DeberiaRetornarCantidad() {
        // Arrange
        when(disenoRepository.countByUsuarioId(usuarioId)).thenReturn(3L);

        // Act
        long result = disenoService.contarDisenosPorUsuario(usuarioId);

        // Assert
        assertEquals(3L, result);
    }

    @Test
    void contarDisenosPorUsuarioYEstado_DeberiaRetornarCantidad() {
        // Arrange
        when(disenoRepository.countByUsuarioIdAndStatus(usuarioId, DisenoStatus.PROGRESO)).thenReturn(2L);

        // Act
        long result = disenoService.contarDisenosPorUsuarioYEstado(usuarioId, DisenoStatus.PROGRESO);

        // Assert
        assertEquals(2L, result);
    }
}