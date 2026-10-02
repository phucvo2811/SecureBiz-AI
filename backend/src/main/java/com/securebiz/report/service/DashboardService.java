package com.securebiz.report.service;

import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.report.api.DashboardDtos.DashboardResponse;
import com.securebiz.report.repository.DashboardRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class DashboardService {
    private final DashboardRepository reports;
    private final ActivityLogRepository audit;

    public DashboardService(DashboardRepository reports, ActivityLogRepository audit) {
        this.reports = reports;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('report.view')")
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(LocalDate from, LocalDate to) {
        validateRange(from, to);
        Instant start=from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end=to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DashboardResponse(reports.summary(start,end),reports.trend(start,end),
                reports.topProducts(start,end),reports.byWarehouse(start,end),reports.byEmployee(start,end),
                reports.purchaseTrend(start,end),reports.stockMovements(start,end),reports.lowStock());
    }

    @PreAuthorize("hasAuthority('report.export')")
    @Transactional
    public CsvReport export(LocalDate from, LocalDate to) {
        DashboardResponse report=dashboard(from,to);
        byte[] content=CsvReportWriter.dashboard(report);
        audit.appendMutation(CurrentActor.userId(),"REPORT_EXPORT","report","dashboard",null,"null",
                "{\"from\":\""+from+"\",\"to\":\""+to+"\",\"format\":\"CSV\"}");
        return new CsvReport("securebiz-dashboard-"+from+"-"+to+".csv",content);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if(from==null||to==null||to.isBefore(from)||from.plusDays(366).isBefore(to))
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_REPORT_RANGE",
                    "Report range must be ordered and no longer than 366 days");
    }

    public record CsvReport(String filename,byte[] content) {}
}
