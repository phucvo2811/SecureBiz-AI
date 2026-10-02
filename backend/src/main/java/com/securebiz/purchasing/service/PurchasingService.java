package com.securebiz.purchasing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.inventory.repository.InventoryRepository;
import com.securebiz.purchasing.api.PurchasingDtos.*;
import com.securebiz.purchasing.repository.PurchasingRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PurchasingService {
    private final PurchasingRepository repository;private final InventoryRepository inventory;private final ActivityLogRepository audit;private final ObjectMapper json;
    public PurchasingService(PurchasingRepository repository,InventoryRepository inventory,ActivityLogRepository audit,ObjectMapper json){this.repository=repository;this.inventory=inventory;this.audit=audit;this.json=json;}
    @PreAuthorize("hasAuthority('supplier.view')") @Transactional(readOnly=true)
    public PageResponse<SupplierResponse> suppliers(String q,int page,int size,String sort){String search=q==null?"":q.strip();return PageResponse.of(repository.suppliers(search,size,(long)page*size,sort),page,size,repository.countSuppliers(search));}
    @PreAuthorize("hasAuthority('supplier.view')") @Transactional(readOnly=true)
    public SupplierHistory supplierHistory(UUID id){var supplier=repository.supplier(id).orElseThrow(()->notFound("SUPPLIER_NOT_FOUND"));return new SupplierHistory(supplier,repository.countPurchasesBySupplier(id),repository.supplierReceivedValue(id),repository.purchasesBySupplier(id));}
    @PreAuthorize("hasAuthority('supplier.manage')") @Transactional
    public SupplierResponse createSupplier(SupplierRequest r){UUID id=UUID.randomUUID(),actor=CurrentActor.userId();try{var x=repository.createSupplier(id,r);audit.appendMutation(actor,"SUPPLIER_CREATE","purchasing","supplier",id,"null",write(x));return x;}catch(DataIntegrityViolationException e){throw conflict("SUPPLIER_CONFLICT","Supplier code is already used");}}
    @PreAuthorize("hasAuthority('purchase.view')") @Transactional(readOnly=true)
    public PageResponse<PurchaseOrderResponse> purchases(String status,String q,int page,int size,String sort){String normalized=status==null?"":status.strip().toUpperCase(),search=q==null?"":q.strip();return PageResponse.of(repository.purchases(normalized,search,size,(long)page*size,sort),page,size,repository.countPurchases(normalized,search));}
    @PreAuthorize("hasAuthority('purchase.view')") @Transactional(readOnly=true)
    public PurchaseOrderResponse purchase(UUID id){return repository.purchase(id).orElseThrow(()->notFound("PURCHASE_NOT_FOUND"));}
    @PreAuthorize("hasAuthority('purchase.create')") @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest r){
        Set<UUID> variants=new HashSet<>();for(var i:r.items())if(!variants.add(i.variantId()))throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"DUPLICATE_PURCHASE_VARIANT","A variant may appear only once");
        BigDecimal total=r.items().stream().map(i->i.quantity().multiply(i.unitCost())).reduce(BigDecimal.ZERO,BigDecimal::add);
        UUID id=UUID.randomUUID(),actor=CurrentActor.userId();String number="PO-"+id.toString().substring(0,8).toUpperCase();
        try{var x=repository.createPurchase(id,number,r,actor,total);audit.appendMutation(actor,"PURCHASE_CREATE","purchasing","purchase_order",id,"null",write(x));return x;}catch(DataIntegrityViolationException e){throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_PURCHASE_REFERENCE","Supplier, warehouse, or variant is invalid");}
    }
    @PreAuthorize("hasAuthority('purchase.create')") @Transactional public PurchaseOrderResponse submit(UUID id){transition(id,"DRAFT","PENDING_APPROVAL",false);return purchase(id);}
    @PreAuthorize("hasAuthority('purchase.approve')") @Transactional public PurchaseOrderResponse approve(UUID id){transition(id,"PENDING_APPROVAL","APPROVED",true);return purchase(id);}
    private void transition(UUID id,String from,String to,boolean approval){UUID actor=CurrentActor.userId();var po=repository.lockPurchase(id).orElseThrow(()->notFound("PURCHASE_NOT_FOUND"));if(!po.status().equals(from))throw conflict("PURCHASE_STATE_CONFLICT","Purchase order is not in "+from);if(approval&&po.createdBy().equals(actor))throw conflict("SEGREGATION_OF_DUTIES","Purchase creator cannot approve the same order");if(repository.transition(id,from,to,actor)==0)throw conflict("PURCHASE_STATE_CONFLICT","Purchase state changed concurrently");audit.appendMutation(actor,approval?"PURCHASE_APPROVE":"PURCHASE_SUBMIT","purchasing","purchase_order",id,"{\"status\":\""+from+"\"}","{\"status\":\""+to+"\"}");}
    @PreAuthorize("hasAuthority('purchase.receive')") @Transactional
    public ReceiptResponse receive(UUID poId,ReceiveRequest request){
        var po=repository.lockPurchase(poId).orElseThrow(()->notFound("PURCHASE_NOT_FOUND"));
        if(!Set.of("APPROVED","RECEIVING").contains(po.status()))throw conflict("PURCHASE_NOT_RECEIVABLE","Purchase order is not approved for receiving");
        var existing=repository.receiptByKey(poId,request.idempotencyKey());if(existing.isPresent())return existing.get();
        Map<UUID,ReceiptItemRequest> requested=new HashMap<>();for(var item:request.items())if(requested.put(item.purchaseOrderItemId(),item)!=null)throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"DUPLICATE_RECEIPT_LINE","Receipt line is duplicated");
        var lines=repository.lockLines(poId,new ArrayList<>(requested.keySet()));if(lines.size()!=requested.size())throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_RECEIPT_LINE","Receipt contains an invalid purchase line");
        for(var line:lines){BigDecimal qty=requested.get(line.id()).quantity(),remaining=line.ordered().subtract(line.received());if(qty.compareTo(remaining)>0)throw conflict("OVER_RECEIPT","Receipt quantity exceeds remaining ordered quantity");}
        UUID actor=CurrentActor.userId(),tx=UUID.randomUUID(),receipt=UUID.randomUUID();String txRef="RCV-"+tx.toString().substring(0,8).toUpperCase();repository.createStockHeader(tx,txRef,po.warehouseId(),actor,poId);
        for(var line:lines){BigDecimal qty=requested.get(line.id()).quantity();inventory.ensureBalance(po.warehouseId(),line.variantId());var balance=inventory.lockBalance(po.warehouseId(),line.variantId()).orElseThrow();BigDecimal after=balance.quantity().add(qty);inventory.updateBalance(balance.id(),after);repository.createStockItem(tx,line.variantId(),qty,balance.quantity(),after,line.unitCost());}
        String receiptNo="GR-"+receipt.toString().substring(0,8).toUpperCase();var result=repository.createReceipt(receipt,receiptNo,poId,tx,actor,request.idempotencyKey(),request.note(),request.items());repository.setReceivingState(poId,repository.allReceived(poId));audit.appendMutation(actor,"PURCHASE_RECEIVE","purchasing","goods_receipt",receipt,"null",write(result));return result;
    }
    private String write(Object o){try{return json.writeValueAsString(o);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private BusinessException conflict(String c,String m){return new BusinessException(HttpStatus.CONFLICT,c,m);}private BusinessException notFound(String c){return new BusinessException(HttpStatus.NOT_FOUND,c,"Resource not found");}
}
