package com.dxc.dxc_platform.modules.admin.repository;

import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailAndDeletedFalse(String email);

    Optional<User> findByIdAndDeletedFalse(Long id);

    Optional<User> findByEmailAndDeletedFalse(String email);

    @Query("""
        select u from User u
        where u.deleted = false
          and (:qLike = '%' or lower(u.email) like :qLike
               or lower(u.nom) like :qLike
               or lower(u.prenom) like :qLike)
          and (:enabled is null or u.enabled = :enabled)
          and (:roleLower = '' or exists (
                select 1 from u.roles r where lower(r.nom) = :roleLower
          ))
    """)
    Page<User> search(@Param("qLike") String qLike,
                      @Param("enabled") Boolean enabled,
                      @Param("roleLower") String roleLower,
                      Pageable pageable);
}