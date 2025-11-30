package com.student2.camelsynclab.route;

import com.student2.camelsynclab.dto.SpareDto;
import com.student2.camelsynclab.entity.Spare;
import com.student2.camelsynclab.repository.SpareRepository;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Маршрут синхронизации данных из внешней CMS в локальную БД.
 * Выполняется каждые 15 минут.
 * Поддерживает пагинацию (size=10), обновляет и добавляет записи по spare_code.
 */
@Component
public class CmsSyncRoute extends RouteBuilder {

    @Autowired
    private SpareRepository spareRepository;

    @Override
    public void configure() {

        from("timer:cms-sync?period=900000") //15 минут
                .routeId("cms-to-db-sync")
                .setProperty("page", constant(0))
                .setProperty("totalProcessed", constant(0))
                .setProperty("continueLoop", constant(true))

                .loopDoWhile(simple("${exchangeProperty.continueLoop}"))
                .log("Запрашиваем страницу: ${exchangeProperty.page}")
                .setHeader("CamelHttpMethod", constant("GET"))
                .toD("http://212.237.219.35:8080/students/2/cms/spares?page=${exchangeProperty.page}&size=10")

                .choice()
                .when(simple("${header.CamelHttpResponseCode} == 200"))
                .unmarshal().json(JsonLibrary.Jackson, SpareDto[].class)
                .process(exchange -> {
                    SpareDto[] spares = exchange.getIn().getBody(SpareDto[].class);
                    log.info("Получено записей с сервера: {}", spares.length);

                    if (spares.length == 0) {
                        exchange.setProperty("continueLoop", false);
                        log.info("Больше страниц нет");
                    } else {

                        final int[] processed = {0};
                        final int[] errors = {0};

                        for (SpareDto dto : spares) {
                            try {
                                Spare spare = spareRepository.findBySpareCode(dto.getSpareCode())
                                        .orElse(new Spare());

                                spare.setSpareCode(dto.getSpareCode());
                                spare.setSpareName(dto.getSpareName());
                                spare.setSpareDescription(dto.getSpareDescription());
                                spare.setSpareType(dto.getSpareType());
                                spare.setSpareStatus(dto.getSpareStatus());

                                try {
                                    Integer price = Integer.valueOf(dto.getPrice());
                                    spare.setPrice(price);
                                } catch (Exception e) {
                                    spare.setPrice(0);
                                    log.warn("Ошибка преобразования цены для {}: {}", dto.getSpareCode(), dto.getPrice());
                                }

                                spare.setQuantity(dto.getQuantity());
                                spare.setUpdatedAt(dto.getUpdatedAt());

                                Spare saved = spareRepository.save(spare);
                                processed[0]++;

                                log.debug("Сохранено: {} - {}", saved.getSpareCode(), saved.getSpareName());

                            } catch (Exception e) {
                                errors[0]++;
                                log.error("Ошибка при обработке записи {}: {}", dto.getSpareCode(), e.getMessage());
                            }
                        }

                        Integer currentTotal = exchange.getProperty("totalProcessed", Integer.class);
                        if (currentTotal == null) currentTotal = 0;
                        exchange.setProperty("totalProcessed", currentTotal + processed[0]);
                        exchange.setProperty("continueLoop", true);

                        log.info("Страница {}: обработано {} записей, ошибок: {}. Всего: {}",
                                exchange.getProperty("page"), processed[0], errors[0], currentTotal + processed[0]);

                        Integer currentPage = exchange.getProperty("page", Integer.class);
                        exchange.setProperty("page", currentPage + 1);
                    }
                })
                .endChoice()
                .otherwise()
                .log("Ошибка при получении страницы ${exchangeProperty.page}: HTTP ${header.CamelHttpResponseCode}")
                .setProperty("continueLoop", constant(false))
                .end()
                .log("Синхронизация завершена. Всего обработано: ${exchangeProperty.totalProcessed} записей");
    }
}