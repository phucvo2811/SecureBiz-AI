package com.securebiz.report.api;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
public final class DashboardDtos{private DashboardDtos(){}
 public record MetricSummary(BigDecimal revenue,long orders,BigDecimal averageOrderValue,BigDecimal inventoryValue,BigDecimal purchaseValue){}
 public record TimePoint(LocalDate date,BigDecimal revenue,long orders){}
 public record NamedValue(String name,BigDecimal value){}
 public record EmployeeRevenue(String name,BigDecimal revenue,long orders){}
 public record PurchasePoint(LocalDate date,BigDecimal receivedValue,long receipts){}
 public record StockMovement(String type,BigDecimal quantity){}
 public record LowStock(String sku,String product,String warehouse,BigDecimal quantity,BigDecimal reorderLevel){}
 public record DashboardResponse(MetricSummary summary,List<TimePoint> revenueTrend,List<NamedValue> topProducts,List<NamedValue> revenueByWarehouse,List<EmployeeRevenue> revenueByEmployee,List<PurchasePoint> purchaseTrend,List<StockMovement> stockMovements,List<LowStock> lowStock){}
}
