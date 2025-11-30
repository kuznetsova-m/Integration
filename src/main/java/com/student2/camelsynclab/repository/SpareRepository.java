package com.student2.camelsynclab.repository;

import com.student2.camelsynclab.entity.Spare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpareRepository extends JpaRepository<Spare, Long> {
    Optional<Spare> findBySpareCode(String spareCode);
}