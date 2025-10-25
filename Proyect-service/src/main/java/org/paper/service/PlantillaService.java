package org.paper.service;


import lombok.extern.slf4j.Slf4j;
import org.paper.repository.PlantillaRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PlantillaService {

    private PlantillaRepository plantillaRepository;

    public PlantillaService(PlantillaRepository plantillaRepository) {
        this.plantillaRepository = plantillaRepository;
    }
}
