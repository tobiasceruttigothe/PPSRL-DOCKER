package org.paper.service;


import lombok.extern.slf4j.Slf4j;
import org.paper.repository.DisenoRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DisenoService {

    private final DisenoRepository disenoRepository;

    public DisenoService(DisenoRepository disenoRepository) {
        this.disenoRepository = disenoRepository;
    }

}
