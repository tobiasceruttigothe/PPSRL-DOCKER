import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paper.dtoCreate.TipoBolsaCreateDto;
import org.paper.dtoCreate.TipoBolsaUpdateDto;
import org.paper.dtoResponse.TipoBolsaResponseDto;
import org.paper.entity.TipoBolsa;
import org.paper.exception.DuplicateEntityException;
import org.paper.exception.EntityNotFoundException;
import org.paper.repository.TipoBolsaRepository;
import org.paper.service.TipoBolsaService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoBolsaServiceTest {

    @Mock
    private TipoBolsaRepository tipoBolsaRepository;

    @InjectMocks
    private TipoBolsaService tipoBolsaService;

    private TipoBolsa tipoBolsa;
    private TipoBolsaCreateDto tipoBolsaCreateDto;

    @BeforeEach
    void setUp() {
        tipoBolsa = new TipoBolsa();
        tipoBolsa.setId(1);
        tipoBolsa.setNombre("Bolsa con Asa");

        tipoBolsaCreateDto = new TipoBolsaCreateDto();
        tipoBolsaCreateDto.setNombre("Bolsa Sin Asa");
    }

    @Test
    void findAll_DeberiaRetornarListaDeTiposBolsa() {
        // Arrange
        when(tipoBolsaRepository.findAll()).thenReturn(Arrays.asList(tipoBolsa));

        // Act
        List<TipoBolsaResponseDto> result = tipoBolsaService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bolsa con Asa", result.get(0).getNombre());
    }

    @Test
    void findById_DeberiaRetornarTipoBolsa() {
        // Arrange
        when(tipoBolsaRepository.findById(1)).thenReturn(Optional.of(tipoBolsa));

        // Act
        TipoBolsaResponseDto result = tipoBolsaService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals("Bolsa con Asa", result.getNombre());
    }

    @Test
    void save_DeberiaCrearTipoBolsa() {
        // Arrange
        when(tipoBolsaRepository.existsByNombreIgnoreCase(anyString())).thenReturn(false);
        when(tipoBolsaRepository.save(any(TipoBolsa.class))).thenReturn(tipoBolsa);

        // Act
        TipoBolsaResponseDto result = tipoBolsaService.save(tipoBolsaCreateDto);

        // Assert
        assertNotNull(result);
        verify(tipoBolsaRepository).save(any(TipoBolsa.class));
    }

    @Test
    void save_DeberiaLanzarExcepcion_CuandoNombreDuplicado() {
        // Arrange
        when(tipoBolsaRepository.existsByNombreIgnoreCase("Bolsa Sin Asa")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEntityException.class, () -> tipoBolsaService.save(tipoBolsaCreateDto));
        verify(tipoBolsaRepository, never()).save(any());
    }

    @Test
    void update_DeberiaActualizarTipoBolsa() {
        // Arrange
        TipoBolsaUpdateDto updateDto = new TipoBolsaUpdateDto();
        updateDto.setNombre("Bolsa Americana");

        when(tipoBolsaRepository.findById(1)).thenReturn(Optional.of(tipoBolsa));
        when(tipoBolsaRepository.existsByNombreIgnoreCaseAndIdNot(anyString(), anyInt())).thenReturn(false);
        when(tipoBolsaRepository.save(any(TipoBolsa.class))).thenReturn(tipoBolsa);

        // Act
        TipoBolsaResponseDto result = tipoBolsaService.update(1, updateDto);

        // Assert
        assertNotNull(result);
        verify(tipoBolsaRepository).save(any(TipoBolsa.class));
    }

    @Test
    void deleteById_DeberiaEliminarTipoBolsa() {
        // Arrange
        when(tipoBolsaRepository.existsById(1)).thenReturn(true);
        doNothing().when(tipoBolsaRepository).deleteById(1);

        // Act
        tipoBolsaService.deleteById(1);

        // Assert
        verify(tipoBolsaRepository).deleteById(1);
    }
}