package com.securebiz.inventory.api;

import com.securebiz.inventory.api.StockOperationDtos.*;
import com.securebiz.inventory.service.StockOperationService;
import com.securebiz.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@Validated @RestController @RequestMapping("/api/v1")
public class StockOperationController {
    private final StockOperationService service;
    public StockOperationController(StockOperationService service){this.service=service;}
    @GetMapping("/stock-transfers") PageResponse<TransferResponse> transfers(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.transfers(page,size);}
    @PostMapping("/stock-transfers") ResponseEntity<TransferResponse> create(@Valid@RequestBody TransferRequest request){var result=service.createTransfer(request);return ResponseEntity.created(URI.create("/api/v1/stock-transfers/"+result.id())).body(result);}
    @PostMapping("/stock-transfers/{id}/submit") TransferResponse submit(@PathVariable UUID id){return service.submitTransfer(id);}
    @PostMapping("/stock-transfers/{id}/approve") TransferResponse approve(@PathVariable UUID id){return service.approveTransfer(id);}
    @PostMapping("/stock-transfers/{id}/dispatch") TransferResponse dispatch(@PathVariable UUID id){return service.dispatch(id);}
    @PostMapping("/stock-transfers/{id}/receive") TransferResponse receive(@PathVariable UUID id){return service.receive(id);}
    @GetMapping("/stocktakes") PageResponse<StocktakeResponse> stocktakes(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.stocktakes(page,size);}
    @PostMapping("/stocktakes") ResponseEntity<StocktakeResponse> stocktake(@Valid@RequestBody StocktakeRequest request){var result=service.createStocktake(request);return ResponseEntity.created(URI.create("/api/v1/stocktakes/"+result.id())).body(result);}
    @PutMapping("/stocktakes/{id}/counts") StocktakeResponse count(@PathVariable UUID id,@Valid@RequestBody CountRequest request){return service.count(id,request);}
    @PostMapping("/stocktakes/{id}/submit") StocktakeResponse submitStocktake(@PathVariable UUID id){return service.submitStocktake(id);}
    @PostMapping("/stocktakes/{id}/approve") StocktakeResponse approveStocktake(@PathVariable UUID id){return service.approveStocktake(id);}
}
