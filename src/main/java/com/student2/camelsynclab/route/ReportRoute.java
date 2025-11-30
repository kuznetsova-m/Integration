package com.student2.camelsynclab.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

/**
 * Маршрут формирования и отправки CSV-отчёта в Report API.
 * Выполняется каждые 5 минут.
 * Отчёт отправляется ТОЛЬКО при изменении данных (сравнение хэша содержимого).
 * CSV: без заголовка, разделитель ";", кодировка UTF-8.
 */
@Component
public class ReportRoute extends RouteBuilder {

    // Хранит хэш последнего отправленного отчёта
    private volatile String lastCsvHash = "";

    @Override
    public void configure() {

        CsvDataFormat csv = new CsvDataFormat();
        csv.setDelimiter(';');
        csv.setSkipHeaderRecord(true);

        from("timer:report?period=300000") //5 минут
                .routeId("db-to-csv-report-smart")
                .to("sql:SELECT spare_code, spare_name, spare_description, spare_type, spare_status, price, quantity, updated_at FROM spares ORDER BY LENGTH(spare_code), spare_code?dataSource=#dataSource")
                .marshal(csv)
                .convertBodyTo(String.class)

                // Считаем хэш текущего CSV
                .process(exchange -> {
                    String currentCsv = exchange.getIn().getBody(String.class);
                    String currentHash = Integer.toHexString(currentCsv.hashCode());

                    if (currentHash.equals(lastCsvHash)) {
                        log.info("Данные не изменились с прошлого раза — отчёт НЕ отправляется");
                        exchange.setProperty("skipReport", true);
                    } else {
                        log.info("Данные изменились — отправляем новый отчёт");
                        exchange.setProperty("skipReport", false);
                        lastCsvHash = currentHash; // обновляем хэш
                        exchange.getIn().setBody(currentCsv);
                    }
                })

                .choice()
                .when(simple("${exchangeProperty.skipReport} != true"))
                .to("file:reports?fileName=report-student2-${date:now:yyyy-MM-dd_HH-mm-ss}.csv")
                .setHeader("CamelHttpMethod", constant("POST"))
                .setHeader("Content-Type", constant("text/csv; charset=UTF-8"))
                .to("http://212.237.219.35:8080/students/2/report/csv?throwExceptionOnFailure=false")
                .log("ОТЧЁТ УСПЕШНО ОТПРАВЛЕН!")
                .otherwise()
                .log("Пропускаем отправку — данные не изменились")
                .end();
    }
}