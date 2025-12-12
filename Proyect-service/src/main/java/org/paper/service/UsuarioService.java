package org.paper.service;


import lombok.extern.slf4j.Slf4j;
import org.paper.entity.Usuario;
import org.paper.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class UsuarioService {

    private UsuarioRepository usuarioRepository;
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario findbyUUID(UUID uuid){
        log.info("Buscando usuario con UUID: {}", uuid);
        return usuarioRepository.findById(uuid).orElse(null);
    }

}
