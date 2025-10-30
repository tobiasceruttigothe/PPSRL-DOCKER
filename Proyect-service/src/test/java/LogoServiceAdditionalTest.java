import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paper.dtoCreate.LogoCreateDto;
import org.paper.entity.Logo;
import org.paper.entity.Usuario;
import org.paper.exception.FileProcessingException;
import org.paper.repository.LogoRepository;
import org.paper.repository.UsuarioRepository;
import org.paper.service.LogoService;
import org.paper.util.Base64ValidatorUtil;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoServiceAdditionalTest {

    @Mock
    private LogoRepository logoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Base64ValidatorUtil base64Validator;

    @InjectMocks
    private LogoService logoService;

    @Test
    void crearLogo_DeberiaLanzarExcepcion_CuandoBase64Invalido() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        LogoCreateDto dto = new LogoCreateDto();
        dto.setUsuarioId(usuarioId);
        dto.setNombre("Logo Test");
        dto.setBase64Logo("invalid_base64");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        doThrow(new FileProcessingException("Logo Test", "decodificar base64", "El formato base64 es inválido"))
                .when(base64Validator).validateBase64ForLogo(anyString(), anyString());

        // Act & Assert
        assertThrows(FileProcessingException.class, () -> logoService.crearLogo(dto));
        verify(logoRepository, never()).save(any(Logo.class));
    }

    @Test
    void crearLogo_DeberiaLanzarExcepcion_CuandoArchivoMuyGrande() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        LogoCreateDto dto = new LogoCreateDto();
        dto.setUsuarioId(usuarioId);
        dto.setNombre("Logo Test");
        dto.setBase64Logo("very_large_base64_string");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        doThrow(new FileProcessingException("Logo Test", "validar tamaño", "El archivo excede el tamaño máximo"))
                .when(base64Validator).validateBase64ForLogo(anyString(), anyString());

        // Act & Assert
        assertThrows(FileProcessingException.class, () -> logoService.crearLogo(dto));
        verify(logoRepository, never()).save(any(Logo.class));
    }
}
