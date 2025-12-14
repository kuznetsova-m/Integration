package com.student2.camelsynclab.route;
import com.student2.camelsynclab.service.ReportService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

@Component
public class ReportRoute extends RouteBuilder {

    @Override
    public void configure() {

        CsvDataFormat csv = new CsvDataFormat();
        csv.setDelimiter(';');
        csv.setSkipHeaderRecord(true);

        from("direct:generateReport")
                .routeId("db-to-csv-report-smart")

                .to("sql:SELECT spare_code, spare_name, spare_description, spare_type, spare_status, price, quantity, updated_at FROM spares ORDER BY LENGTH(spare_code), spare_code?dataSource=#dataSource")

                .marshal(csv)
                .convertBodyTo(String.class)

                .setProperty("shouldSend", constant(true))
                .process(exchange -> {
                    String currentCsv = exchange.getIn().getBody(String.class);
                    boolean shouldSend = exchange.getContext().getRegistry()
                            .lookupByNameAndType("reportService", ReportService.class)
                            .shouldSendReport(currentCsv);

                    exchange.setProperty("shouldSend", shouldSend);
                    if (shouldSend) {
                        exchange.getIn().setBody(currentCsv);
                    }
                })

                .choice()
                .when(exchangeProperty("shouldSend"))
                .to("file:reports?fileName=report-student2-${date:now:yyyy-MM-dd_HH-mm-ss}.csv")

                .setHeader("CamelHttpMethod", constant("POST"))
                .setHeader("Content-Type", constant("text/csv; charset=UTF-8"))
                .to("http://212.237.219.35:8080/students/2/report/csv?throwExceptionOnFailure=false")
                .log("Отчёт отправлен в Report API")
                .otherwise()
                .log("Отчёт пропущен: данные не изменились")
                .end();
    }
}