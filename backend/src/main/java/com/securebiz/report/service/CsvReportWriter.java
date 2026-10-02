package com.securebiz.report.service;

import com.securebiz.report.api.DashboardDtos.DashboardResponse;

import java.nio.charset.StandardCharsets;

final class CsvReportWriter {
    private CsvReportWriter() {}

    static byte[] dashboard(DashboardResponse report) {
        StringBuilder csv=new StringBuilder("\ufeffsection,dimension,value,orders,detail\r\n");
        row(csv,"summary","revenue",report.summary().revenue(),report.summary().orders(),"");
        row(csv,"summary","average_order_value",report.summary().averageOrderValue(),"","");
        row(csv,"summary","inventory_value",report.summary().inventoryValue(),"","");
        row(csv,"summary","purchase_value",report.summary().purchaseValue(),"","");
        report.revenueTrend().forEach(point->row(csv,"revenue_trend",point.date(),point.revenue(),point.orders(),""));
        report.topProducts().forEach(value->row(csv,"top_product",value.name(),value.value(),"",""));
        report.revenueByWarehouse().forEach(value->row(csv,"warehouse_revenue",value.name(),value.value(),"",""));
        report.revenueByEmployee().forEach(value->row(csv,"employee_revenue",value.name(),value.revenue(),value.orders(),""));
        report.purchaseTrend().forEach(value->row(csv,"purchase_receipt",value.date(),value.receivedValue(),value.receipts(),""));
        report.stockMovements().forEach(value->row(csv,"stock_movement",value.type(),value.quantity(),"",""));
        report.lowStock().forEach(item->row(csv,"low_stock",item.sku(),item.quantity(),"",item.product()+" @ "+item.warehouse()+"; reorder="+item.reorderLevel()));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder csv,Object... values) {
        for(int i=0;i<values.length;i++){if(i>0)csv.append(',');csv.append(cell(String.valueOf(values[i])));}csv.append("\r\n");
    }

    static String cell(String value) {
        String safe=value;
        if(!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0))>=0)safe="'"+safe;
        return "\""+safe.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\"";
    }
}
