package com.student2.camelsynclab.route;

import com.student2.camelsynclab.dto.SpareDto;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CmsSyncRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(CmsSyncRoute.class);

    @Override
    public void configure() {

        from("timer:cms-sync?period=300000")
                .routeId("cms-to-db-sync")
                .setProperty("page", constant(0))
                .setProperty("totalProcessed", constant(0))
                .setProperty("continueLoop", constant(true))

                .loopDoWhile(simple("${exchangeProperty.continueLoop} == true"))
                .setHeader("CamelHttpMethod", constant("GET"))
                .toD("http://212.237.219.35:8080/students/2/cms/spares?page=${exchangeProperty.page}&size=10")

                .choice()
                .when(simple("${header.CamelHttpResponseCode} != 200"))
                .log("Ошибка HTTP ${header.CamelHttpResponseCode} при запросе страницы ${exchangeProperty.page}")
                .setProperty("continueLoop", constant(false))
                .otherwise()
                .unmarshal().json(JsonLibrary.Jackson, SpareDto[].class)

                .process(exchange -> {
                    SpareDto[] pageData = exchange.getIn().getBody(SpareDto[].class);
                    if (pageData == null || pageData.length == 0) {
                        exchange.setProperty("continueLoop", false);
                        //log.info("Cтраница {} пустая", exchange.getProperty("page", Integer.class) + 1);
                    } else {
                        exchange.setProperty("page", exchange.getProperty("page", Integer.class) + 1);
                    }
                })

                .filter(simple("${body.length} > 0"))
                .process(exchange -> {
                    SpareDto[] pageData = exchange.getIn().getBody(SpareDto[].class);
                    int pageSize = pageData.length;

                    Integer currentTotal = exchange.getProperty("totalProcessed", 0, Integer.class);
                    int newTotal = currentTotal + pageSize;
                    exchange.setProperty("totalProcessed", newTotal);

                    //log.info("Обработана страница {}: {} записей (всего: {})",
                    //        exchange.getProperty("page"), pageSize, newTotal);
                })
                .split(body())
                .to("bean:spareSyncService?method=upsertSpare")
                .end()
                .end()
                .endChoice()
                .end()
                .end()

                .log("Синхронизация завершена. Всего обработано записей: ${exchangeProperty.totalProcessed}")
                .to("direct:generateReport")
                .log("Запущена генерация отчёта");
    }
}