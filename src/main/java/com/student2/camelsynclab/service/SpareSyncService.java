package com.student2.camelsynclab.service;

import com.student2.camelsynclab.dto.SpareDto;
import com.student2.camelsynclab.entity.Spare;
import com.student2.camelsynclab.repository.SpareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpareSyncService {

    private static final Logger log = LoggerFactory.getLogger(SpareSyncService.class);

    @Autowired
    private SpareRepository spareRepository;

    public void upsertSpare(SpareDto dto) {
        Spare spare = spareRepository.findBySpareCode(dto.getSpareCode())
                .orElseGet(() -> {
                    Spare newSpare = new Spare();
                    newSpare.setSpareCode(dto.getSpareCode());
                    return newSpare;
                });

        spare.setSpareName(dto.getSpareName());
        spare.setSpareDescription(dto.getSpareDescription());
        spare.setSpareType(dto.getSpareType());
        spare.setSpareStatus(dto.getSpareStatus());

        try {
            spare.setPrice(Integer.valueOf(dto.getPrice()));
        } catch (NumberFormatException e) {
            spare.setPrice(0);
            log.warn("Ошибка преобразования цены для spareCode={} : '{}'", dto.getSpareCode(), dto.getPrice());
        }

        spare.setQuantity(dto.getQuantity());
        spare.setUpdatedAt(dto.getUpdatedAt());

        spareRepository.save(spare);
    }
}