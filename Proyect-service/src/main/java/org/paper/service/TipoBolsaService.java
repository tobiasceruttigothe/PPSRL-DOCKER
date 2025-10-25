package org.paper.service;


import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.dtoCreate.TipoBolsaCreateDto;
import org.paper.entity.Material;
import org.paper.entity.TipoBolsa;
import org.paper.repository.MaterialRepository;
import org.paper.repository.TipoBolsaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TipoBolsaService {

    private TipoBolsaRepository tipoBolsaRepository;

    public TipoBolsaService(TipoBolsaRepository tipoBolsaRepository) {
        this.tipoBolsaRepository = tipoBolsaRepository;
    }

    public Optional<TipoBolsa> findById(Integer id) {
        return tipoBolsaRepository.findById(id);
    }

    public List<TipoBolsa> findAll() {
        return tipoBolsaRepository.findAll();
    }

    public TipoBolsa save(TipoBolsaCreateDto tipoBolsaCreateDto) {
        TipoBolsa tipoBolsa = new TipoBolsa();
        tipoBolsa.setNombre(tipoBolsaCreateDto.getNombre());
        return tipoBolsaRepository.save(tipoBolsa);
    }

    public void deleteById(Integer id) {
        tipoBolsaRepository.deleteById(id);
    }


}
