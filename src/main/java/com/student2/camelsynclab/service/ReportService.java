package com.student2.camelsynclab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private String lastCsvHash = "";

    public boolean shouldSendReport(String currentCsv) {
        String currentHash = Integer.toHexString(currentCsv.hashCode());

        if (currentHash.equals(lastCsvHash)) {
            log.info("Отчёт пропущен: данные не изменились (хэш: {})", currentHash);
            return false;
        } else {
            log.info("Отчёт отправляется: данные изменились (новый хэш: {})", currentHash);
            lastCsvHash = currentHash;
            return true;
        }
    }

    public String getLastCsvHash() {
        return lastCsvHash;
    }
}