package com.securebiz.sales.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.inventory.repository.InventoryRepository;
import com.securebiz.inventory.repository.StockOperationRepository;
import com.securebiz.sales.api.ReturnDtos.ReturnRequest;
import com.securebiz.sales.api.ReturnDtos.ReturnResponse;
import com.securebiz.sales.repository.ReturnRepository;
import com.securebiz.sales.repository.ReturnRepository.ReturnLineDraft;
import com.securebiz.notification.repository.NotificationRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ReturnService {
    private final ReturnRepository returns;
    private final InventoryRepository inventory;
    private final StockOperationRepository stock;
    private final ActivityLogRepository audit;
    private final ObjectMapper json;
    private final NotificationRepository notifications;

    public ReturnService(ReturnRepository returns, InventoryRepository inventory,
                         StockOperationRepository stock, ActivityLogRepository audit, ObjectMapper json, NotificationRepository notifications) {
        this.returns = returns;
        this.inventory = inventory;
        this.stock = stock;
        this.audit = audit;
        this.json = json;
        this.notifications = notifications;
    }

    @PreAuthorize("hasAnyAuthority('return.create','return.approve')")
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> list(int page,int size) { return PageResponse.of(returns.list(size,(long)page*size),page,size,returns.count()); }

    @PreAuthorize("hasAuthority('return.create')")
    @Transactional
    public ReturnResponse create(ReturnRequest request) {
        UUID actor = CurrentActor.userId();
        var order = returns.lockOrder(request.orderId()).orElseThrow(() -> notFound("ORDER_NOT_FOUND", "Order not found"));
        if (!List.of("COMPLETED", "PARTIALLY_RETURNED").contains(order.status()))
            throw invalid("ORDER_NOT_RETURNABLE", "Only completed orders can be returned");

        var unique = new HashSet<UUID>();
        for (var item : request.items())
            if (!unique.add(item.orderItemId()))
                throw invalid("DUPLICATE_RETURN_ITEM", "An order line may appear only once");

        var orderLines = returns.orderLines(request.orderId(), unique.stream().sorted().toList());
        if (orderLines.size() != unique.size())
            throw invalid("INVALID_ORDER_ITEM", "One or more lines do not belong to this order");

        var byId = orderLines.stream().collect(java.util.stream.Collectors.toMap(ReturnRepository.OrderLine::id, x -> x));
        List<ReturnLineDraft> lines = request.items().stream().map(item -> {
            var line = byId.get(item.orderItemId());
            BigDecimal remaining = line.purchased().subtract(line.returned());
            if (item.quantity().compareTo(remaining) > 0)
                throw invalid("RETURN_QUANTITY_EXCEEDED", "Return quantity exceeds remaining quantity for SKU " + line.sku());
            BigDecimal unitRefund = line.lineTotal().divide(line.purchased(), 4, RoundingMode.HALF_UP);
            BigDecimal lineRefund = unitRefund.multiply(item.quantity()).setScale(4, RoundingMode.HALF_UP);
            return new ReturnLineDraft(line.id(), item.quantity(), item.disposition(), unitRefund, lineRefund);
        }).toList();

        UUID id = UUID.randomUUID();
        var result = returns.create(id, "RET-" + shortId(id), request, actor, lines);
        audit.appendMutation(actor, "RETURN_CREATE", "sales", "return", id, "null", write(result));
        return result;
    }

    @PreAuthorize("hasAuthority('return.create')")
    @Transactional
    public ReturnResponse submit(UUID id) {
        UUID actor = CurrentActor.userId();
        var ret = returns.lockReturn(id).orElseThrow(() -> notFound("RETURN_NOT_FOUND", "Return not found"));
        if (!"DRAFT".equals(ret.status())) throw conflict("INVALID_RETURN_STATE", "Only draft returns can be submitted");
        if (returns.transition(id, "DRAFT", "PENDING_APPROVAL", actor) != 1)
            throw conflict("RETURN_STATE_CHANGED", "Return state changed concurrently");
        var result = returns.find(id).orElseThrow();
        notifications.createForPermission("return.approve","APPROVAL_REQUIRED","Phiếu trả hàng chờ duyệt",
                result.returnNumber()+" cần được kiểm tra và phê duyệt.","return",id,actor);
        audit.appendMutation(actor, "RETURN_SUBMIT", "sales", "return", id, "{\"status\":\"DRAFT\"}", write(result));
        return result;
    }

    @PreAuthorize("hasAuthority('return.approve')")
    @Transactional
    public ReturnResponse approve(UUID id) {
        UUID actor = CurrentActor.userId();
        var ret = returns.lockReturn(id).orElseThrow(() -> notFound("RETURN_NOT_FOUND", "Return not found"));
        if (!"PENDING_APPROVAL".equals(ret.status()))
            throw conflict("INVALID_RETURN_STATE", "Only submitted returns can be approved");
        if (actor.equals(ret.createdBy()))
            throw conflict("SEGREGATION_OF_DUTIES", "The return creator cannot approve its refund");

        var order = returns.lockOrder(ret.orderId()).orElseThrow();
        var payment = returns.lockPayment(order.id()).orElseThrow(() -> conflict("PAYMENT_NOT_FOUND", "No successful payment exists for this order"));
        var lines = returns.lockReturnLines(id);
        BigDecimal refund = lines.stream().map(ReturnRepository.ReturnLine::lineRefund).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = payment.amount().subtract(returns.successfulRefunds(payment.id()));
        if (refund.signum() <= 0 || refund.compareTo(remaining) > 0)
            throw conflict("REFUND_LIMIT_EXCEEDED", "Refund exceeds the remaining captured payment");

        var restock = lines.stream().filter(line -> "RESTOCK".equals(line.disposition())).toList();
        if (!restock.isEmpty()) {
            UUID tx = UUID.randomUUID();
            stock.createStockHeader(tx, "RTN-" + shortId(tx), order.warehouse(), "RETURN", actor, "return", id, "Approved customer return");
            for (var line : restock) {
                inventory.ensureBalance(order.warehouse(), line.variant());
                var balance = inventory.lockBalance(order.warehouse(), line.variant()).orElseThrow();
                BigDecimal after = balance.quantity().add(line.quantity());
                inventory.updateBalance(balance.id(), after);
                stock.createStockItem(tx, line.variant(), line.quantity(), balance.quantity(), after);
            }
        }

        returns.createRefund(UUID.randomUUID(), id, payment, refund.setScale(4, RoundingMode.HALF_UP), actor);
        if (returns.transition(id, "PENDING_APPROVAL", "COMPLETED", actor) != 1)
            throw conflict("RETURN_STATE_CHANGED", "Return state changed concurrently");
        returns.updateOrderReturnState(order.id(), returns.allOrderQuantityReturned(order.id()));
        var result = returns.find(id).orElseThrow();
        audit.appendMutation(actor, "RETURN_APPROVE", "sales", "return", id, "{\"status\":\"PENDING_APPROVAL\"}", write(result));
        return result;
    }

    private String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private String shortId(UUID id) { return id.toString().substring(0, 8).toUpperCase(); }
    private BusinessException invalid(String code, String message) { return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, code, message); }
    private BusinessException conflict(String code, String message) { return new BusinessException(HttpStatus.CONFLICT, code, message); }
    private BusinessException notFound(String code, String message) { return new BusinessException(HttpStatus.NOT_FOUND, code, message); }
}
