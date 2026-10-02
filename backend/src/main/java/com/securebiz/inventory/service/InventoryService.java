package com.securebiz.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.inventory.api.InventoryDtos.*;
import com.securebiz.inventory.repository.InventoryRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {
    private final InventoryRepository repository;private final ActivityLogRepository audit;private final ObjectMapper json;
    public InventoryService(InventoryRepository repository,ActivityLogRepository audit,ObjectMapper json){this.repository=repository;this.audit=audit;this.json=json;}
    @PreAuthorize("hasAuthority('inventory.view')") @Transactional(readOnly=true)
    public List<WarehouseResponse> warehouses(){return repository.warehouses();}
    @PreAuthorize("hasAuthority('warehouse.manage')") @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request){UUID id=UUID.randomUUID(),actor=CurrentActor.userId();try{var r=repository.createWarehouse(id,request);audit.appendMutation(actor,"WAREHOUSE_CREATE","inventory","warehouse",id,"null",write(r));return r;}catch(DataIntegrityViolationException e){throw new BusinessException(HttpStatus.CONFLICT,"WAREHOUSE_CONFLICT","Warehouse code is already used");}}
    @PreAuthorize("hasAuthority('inventory.view')") @Transactional(readOnly=true)
    public PageResponse<InventoryResponse> inventory(UUID warehouse,String search,int page,int size){String q=search==null?"":search.strip();return PageResponse.of(repository.inventory(warehouse,q,size,(long)page*size),page,size,repository.countInventory(warehouse,q));}
    @PreAuthorize("hasAuthority('inventory.view')") @Transactional(readOnly=true)
    public PageResponse<StockLedgerEntry> ledger(UUID warehouse,UUID variant,String type,int page,int size){String normalized=type==null?"":type.strip().toUpperCase();return PageResponse.of(repository.ledger(warehouse,variant,normalized,size,(long)page*size),page,size,repository.countLedger(warehouse,variant,normalized));}
    @PreAuthorize("hasAuthority('inventory.adjust')") @Transactional
    public StockTransactionResponse adjust(AdjustmentRequest request){
        if(request.quantityDelta().compareTo(BigDecimal.ZERO)==0)throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"ZERO_ADJUSTMENT","Adjustment quantity cannot be zero");
        try{repository.ensureBalance(request.warehouseId(),request.variantId());var balance=repository.lockBalance(request.warehouseId(),request.variantId()).orElseThrow();
            BigDecimal after=balance.quantity().add(request.quantityDelta());if(after.compareTo(BigDecimal.ZERO)<0)throw new BusinessException(HttpStatus.CONFLICT,"INSUFFICIENT_STOCK","Adjustment would make inventory negative");
            UUID actor=CurrentActor.userId(),tx=UUID.randomUUID();String reference="ADJ-"+tx.toString().substring(0,8).toUpperCase();
            repository.updateBalance(balance.id(),after);var result=repository.appendAdjustment(tx,reference,request.warehouseId(),actor,request.reason().strip(),request.variantId(),request.quantityDelta(),balance.quantity(),after);
            audit.appendMutation(actor,"INVENTORY_ADJUST","inventory","stock_transaction",tx,"null",write(result));return result;
        }catch(DataIntegrityViolationException e){throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_INVENTORY_REFERENCE","Warehouse or variant is invalid");}
    }
    private String write(Object o){try{return json.writeValueAsString(o);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
}
