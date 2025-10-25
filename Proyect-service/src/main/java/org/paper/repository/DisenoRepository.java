package org.paper.repository;

import org.paper.entity.Diseno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisenoRepository extends JpaRepository<Diseno, Integer> {

}
