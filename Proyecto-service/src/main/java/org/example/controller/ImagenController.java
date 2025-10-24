package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ImagenDto;
import org.example.entity.Imagen;
import org.example.service.ImagenService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imagenes")
public class ImagenController {

    private final ImagenService service;

    @GetMapping
    public List<ImagenDto> list(){
        return service.list().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ImagenDto get(@PathVariable Integer id){ return toDto(service.get(id)); }

    @PostMapping
    public ImagenDto create(@RequestBody ImagenDto dto){
        return toDto(service.create(toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id){ service.delete(id); }

    @GetMapping("/formato/{formato}")
    public List<ImagenDto> byFormato(@PathVariable String formato){
        return service.findByFormato(formato).stream().map(this::toDto).collect(Collectors.toList());
    }

    @DeleteMapping("/older-than/{isoDateTime}")
    public void deleteOlderThan(@PathVariable String isoDateTime){
        service.deleteOlderThan(LocalDateTime.parse(isoDateTime));
    }

    // ===== mapping =====
    private ImagenDto toDto(Imagen i){
        return new ImagenDto(
                i.getId(),
                i.getFormato(),
                i.getTamanoBytes(),
                i.getFechaCreacion(),
                i.getDatosImagen()!=null ? Base64.getEncoder().encodeToString(i.getDatosImagen()) : null
        );
    }
    private Imagen toEntity(ImagenDto d){
        Imagen i = new Imagen();
        i.setId(d.getId());
        i.setFormato(d.getFormato());
        i.setTamanoBytes(d.getTamanoBytes());
        i.setFechaCreacion(d.getFechaCreacion());
        if(d.getBase64Data()!=null){ i.setDatosImagen(Base64.getDecoder().decode(d.getBase64Data())); }
        return i;
    }
}
