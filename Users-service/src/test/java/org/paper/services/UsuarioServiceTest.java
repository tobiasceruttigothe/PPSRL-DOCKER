package org.paper.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paper.clients.KeycloakClient;
import org.paper.dto.UsuarioCreateDTO;
import org.paper.entity.Usuario;
import org.paper.entity.UsuarioStatus;
import org.paper.exception.KeycloakException;
import org.paper.exception.UsuarioYaExisteException;
import org.paper.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        lenient().when(keycloakAdminService.getAdminToken()).thenReturn("fake-admin-token");
    }

    @Test
    void testCrearUsuario_ok() {
        // Arrange
        UsuarioCreateDTO dto = UsuarioCreateDTO.builder()
                .username("testUser")
                .email("test@example.com")
                .razonSocial("MiEmpresa")
                .password("Password123")
                .rol("CLIENTE")
                .enabled(true)
                .emailVerified(false)
                .build();

        String newUserId = UUID.randomUUID().toString();

        when(keycloakClient.obtenerUserId(eq("testUser"), anyString()))
                .thenReturn(null) // Primera llamada: usuario no existe
                .thenReturn(newUserId); // Segunda llamada: usuario existe después de crearlo

        doNothing().when(keycloakClient).crearUsuario(any(UsuarioCreateDTO.class), anyString());
        doNothing().when(keycloakClient).asignarPasswordTemporal(eq(newUserId), eq(dto.getPassword()), anyString());
        doNothing().when(emailVerificationService).createAndSendVerification(anyString(), anyString(), anyString());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        // Mock para cambio de rol
        when(keycloakClient.listarRolesDeUsuario(anyString(), anyString())).thenReturn(java.util.Collections.emptyList());
        when(keycloakClient.obtenerRolPorNombre(eq("CLIENTE"), anyString()))
                .thenReturn(java.util.Map.of("name", "CLIENTE", "id", "role-id"));
        doNothing().when(keycloakClient).agregarRolesAUsuario(anyString(), any(), anyString());

        // Act
        ResponseEntity<String> response = usuarioService.crearUsuario(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Usuario creado correctamente"));
        verify(usuarioRepository).save(any(Usuario.class));
        verify(keycloakClient).asignarPasswordTemporal(eq(newUserId), eq(dto.getPassword()), anyString());
    }

    @Test
    void testCrearUsuario_fallaPorUsuarioYaExistente() {
        // Arrange
        UsuarioCreateDTO dto = UsuarioCreateDTO.builder()
                .username("testUser")
                .email("test@example.com")
                .razonSocial("MiEmpresa")
                .password("Password123")
                .rol("CLIENTE")
                .enabled(true)
                .emailVerified(false)
                .build();

        when(keycloakClient.obtenerUserId(eq("testUser"), anyString())).thenReturn("existing-id");

        // Act & Assert
        assertThrows(UsuarioYaExisteException.class, () -> usuarioService.crearUsuario(dto));
        verify(keycloakClient, never()).crearUsuario(any(), any());
    }

    @Test
    void testCrearUsuario_fallaKeycloakAlCrear() {
        // Arrange
        UsuarioCreateDTO dto = UsuarioCreateDTO.builder()
                .username("badUser")
                .email("bad@example.com")
                .razonSocial("MiEmpresa")
                .password("Password123")
                .rol("CLIENTE")
                .enabled(true)
                .emailVerified(false)
                .build();

        when(keycloakClient.obtenerUserId(eq("badUser"), anyString())).thenReturn(null);
        doThrow(new KeycloakException("crear usuario", 500, "Error"))
                .when(keycloakClient).crearUsuario(any(UsuarioCreateDTO.class), anyString());

        // Act & Assert
        assertThrows(KeycloakException.class, () -> usuarioService.crearUsuario(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testEliminarUsuario_ok() {
        // Arrange
        String username = "testUser";
        String userId = UUID.randomUUID().toString();

        Usuario usuario = new Usuario();
        usuario.setId(UUID.fromString(userId));
        usuario.setFechaRegistro(OffsetDateTime.now());
        usuario.setStatus(UsuarioStatus.ACTIVE); // o el enum que corresponda

        when(keycloakClient.obtenerUserId(eq(username), anyString())).thenReturn(userId);
        when(usuarioRepository.findById(UUID.fromString(userId)))
                .thenReturn(Optional.of(usuario));
        doNothing().when(keycloakClient).eliminarUsuario(eq(userId), anyString());

        // Act
        ResponseEntity<String> response = usuarioService.eliminarUsuario(username);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Usuario eliminado correctamente", response.getBody());
        verify(usuarioRepository).deleteById(UUID.fromString(userId));
        verify(keycloakClient).eliminarUsuario(eq(userId), anyString());
    }


    @Test
    void testCambiarRolUsuario_ok() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        String nuevoRol = "ADMIN";
        String token = "fake-token";

        when(keycloakClient.listarRolesDeUsuario(eq(userId), eq(token)))
                .thenReturn(java.util.List.of(java.util.Map.of("name", "CLIENTE")));
        when(keycloakClient.obtenerRolPorNombre(eq(nuevoRol), eq(token)))
                .thenReturn(java.util.Map.of("name", nuevoRol, "id", "admin-role-id"));
        doNothing().when(keycloakClient).eliminarRolesDeUsuario(anyString(), any(), anyString());
        doNothing().when(keycloakClient).agregarRolesAUsuario(anyString(), any(), anyString());

        // Act
        usuarioService.cambiarRolUsuario(userId, nuevoRol, token);

        // Assert
        verify(keycloakClient).eliminarRolesDeUsuario(eq(userId), any(), eq(token));
        verify(keycloakClient).agregarRolesAUsuario(eq(userId), any(), eq(token));
    }
}