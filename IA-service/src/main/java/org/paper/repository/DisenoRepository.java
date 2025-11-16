package org.paper.repository;

import org.paper.entity.Diseno;
import org.paper.entity.DisenoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisenoRepository extends JpaRepository<Diseno, Integer> {


}