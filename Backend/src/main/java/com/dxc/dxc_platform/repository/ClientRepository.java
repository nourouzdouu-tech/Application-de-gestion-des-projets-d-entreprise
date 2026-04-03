package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByNomAndDeletedFalse(String nom);
    Optional<Client> findByIdAndDeletedFalse(Long id);

    @Query("SELECT c FROM Client c WHERE c.deleted = false AND LOWER(c.nom) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Client> search(@Param("q") String q, Pageable pageable);
}