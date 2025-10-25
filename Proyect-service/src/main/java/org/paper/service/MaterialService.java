package org.paper.service;


import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.entity.Material;
import org.paper.repository.MaterialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MaterialService {

    private MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public Optional<Material> findById(Integer id) {
        return materialRepository.findById(id);
    }

    public List<Material> findAll() {
        return materialRepository.findAll();
    }

    public Material save(MaterialCreateDto materialCreateDto) {
        Material material = new Material();
        material.setNombre(materialCreateDto.getNombre());
        return materialRepository.save(material);
    }

    public void deleteById(Integer id) {
        materialRepository.deleteById(id);
    }


}
