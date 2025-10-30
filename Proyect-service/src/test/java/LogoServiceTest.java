
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paper.dtoCreate.LogoCreateDto;
import org.paper.dtoCreate.LogoUpdateDto;
import org.paper.dtoResponse.LogoResponseDto;
import org.paper.entity.Logo;
import org.paper.entity.Usuario;
import org.paper.exception.EntityNotFoundException;
import org.paper.exception.UnauthorizedAccessException;
import org.paper.repository.LogoRepository;
import org.paper.repository.UsuarioRepository;
import org.paper.service.LogoService;
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
class LogoServiceTest {

    @Mock
    private LogoRepository logoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Base64ValidatorUtil base64Validator;

    @InjectMocks
    private LogoService logoService;

    private UUID usuarioId;
    private Usuario usuario;
    private Logo logo;
    private LogoCreateDto logoCreateDto;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(usuarioId);

        logo = new Logo();
        logo.setId(1);
        logo.setUsuario(usuario);
        logo.setNombre("Logo Test");
        logo.setBase64Logo("base64encodedstring");
        logo.setFechaCreacion(LocalDateTime.now());
        logo.setTamanoBytes(1024L);

        logoCreateDto = new LogoCreateDto();
        logoCreateDto.setUsuarioId(usuarioId);
        logoCreateDto.setNombre("Nuevo Logo");
        logoCreateDto.setBase64Logo("base64encodedstring");
    }

    @Test
    void crearLogo_DeberiaCrearCorrectamente() {
        // Arrange
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        doNothing().when(base64Validator).validateBase64ForLogo(anyString(), anyString());
        when(base64Validator.calculateBase64Size(anyString())).thenReturn(1024L);
        when(logoRepository.save(any(Logo.class))).thenReturn(logo);

        // Act
        LogoResponseDto result = logoService.crearLogo(logoCreateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Logo Test", result.getNombre());
        verify(usuarioRepository).findById(usuarioId);
        verify(logoRepository).save(any(Logo.class));
        verify(base64Validator).validateBase64ForLogo(anyString(), anyString());
    }

    @Test
    void crearLogo_DeberiaLanzarExcepcion_CuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> logoService.crearLogo(logoCreateDto));
        verify(logoRepository, never()).save(any());
    }

    @Test
    void obtenerLogosPorUsuario_DeberiaRetornarLista() {
        // Arrange
        when(usuarioRepository.existsById(usuarioId)).thenReturn(true);
        when(logoRepository.findByUsuarioId(usuarioId)).thenReturn(Arrays.asList(logo));

        // Act
        List<LogoResponseDto> result = logoService.obtenerLogosPorUsuario(usuarioId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Logo Test", result.get(0).getNombre());
    }

    @Test
    void obtenerLogoPorId_DeberiaRetornarLogo() {
        // Arrange
        when(logoRepository.findById(1)).thenReturn(Optional.of(logo));

        // Act
        LogoResponseDto result = logoService.obtenerLogoPorId(1);

        // Assert
        assertNotNull(result);
        assertEquals("Logo Test", result.getNombre());
    }

    @Test
    void obtenerLogoPorId_DeberiaLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        when(logoRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> logoService.obtenerLogoPorId(999));
    }

    @Test
    void actualizarLogo_DeberiaActualizarCorrectamente() {
        // Arrange
        LogoUpdateDto updateDto = new LogoUpdateDto();
        updateDto.setNombre("Logo Actualizado");
        updateDto.setBase64Logo("newbase64");

        when(logoRepository.findById(1)).thenReturn(Optional.of(logo));
        doNothing().when(base64Validator).validateBase64ForLogo(anyString(), anyString());
        when(base64Validator.calculateBase64Size(anyString())).thenReturn(2048L);
        when(logoRepository.save(any(Logo.class))).thenReturn(logo);

        // Act
        LogoResponseDto result = logoService.actualizarLogo(1, updateDto);

        // Assert
        assertNotNull(result);
        verify(logoRepository).save(any(Logo.class));
    }

    @Test
    void eliminarLogo_DeberiaEliminarCorrectamente() {
        // Arrange
        when(logoRepository.existsById(1)).thenReturn(true);
        doNothing().when(logoRepository).deleteById(1);

        // Act
        logoService.eliminarLogo(1);

        // Assert
        verify(logoRepository).deleteById(1);
    }

    @Test
    void eliminarLogoPorUsuario_DeberiaEliminarCorrectamente() {
        // Arrange
        when(logoRepository.findById(1)).thenReturn(Optional.of(logo));
        doNothing().when(logoRepository).delete(logo);

        // Act
        logoService.eliminarLogoPorUsuario(usuarioId, 1);

        // Assert
        verify(logoRepository).delete(logo);
    }

    @Test
    void eliminarLogoPorUsuario_DeberiaLanzarExcepcion_CuandoNoPertenece() {
        // Arrange
        UUID otroUsuarioId = UUID.randomUUID();
        when(logoRepository.findById(1)).thenReturn(Optional.of(logo));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class,
                () -> logoService.eliminarLogoPorUsuario(otroUsuarioId, 1));
        verify(logoRepository, never()).delete(any());
    }

    @Test
    void contarLogosPorUsuario_DeberiaRetornarCantidad() {
        // Arrange
        when(logoRepository.countByUsuarioId(usuarioId)).thenReturn(5L);

        // Act
        long result = logoService.contarLogosPorUsuario(usuarioId);

        // Assert
        assertEquals(5L, result);
    }
}