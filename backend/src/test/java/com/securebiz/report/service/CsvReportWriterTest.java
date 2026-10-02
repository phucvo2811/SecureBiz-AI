package com.securebiz.report.service;

import com.securebiz.report.api.DashboardDtos.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReportWriterTest {
    @Test void exportIncludesAllSectionsAndNeutralizesSpreadsheetFormulaInjection() {
        var report=new DashboardResponse(new MetricSummary(new BigDecimal("100"),2,new BigDecimal("50"),new BigDecimal("80"),new BigDecimal("30")),
                List.of(new TimePoint(LocalDate.of(2026,9,28),new BigDecimal("100"),2)),
                List.of(new NamedValue("=HYPERLINK(\"bad\")",new BigDecimal("3"))),
                List.of(new NamedValue("Main",new BigDecimal("100"))),
                List.of(new EmployeeRevenue("Seller",new BigDecimal("100"),2)),
                List.of(new PurchasePoint(LocalDate.of(2026,9,28),new BigDecimal("40"),1)),
                List.of(new StockMovement("SALE",new BigDecimal("-2"))),
                List.of(new LowStock("SKU-1","Mouse","Main",BigDecimal.ONE,new BigDecimal("5"))));
        String csv=new String(CsvReportWriter.dashboard(report),StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\ufeffsection,dimension,value,orders,detail")
                .contains("revenue_trend","top_product","warehouse_revenue","employee_revenue","purchase_receipt","stock_movement","low_stock")
                .contains("\"'=HYPERLINK(\"\"bad\"\")\"");
    }
}
