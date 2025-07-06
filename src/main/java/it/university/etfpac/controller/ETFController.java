package it.university.etfpac.controller;

import it.university.etfpac.dto.response.ApiResponse;
import it.university.etfpac.dto.response.ETFResponse;
import it.university.etfpac.service.ETFService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/etfs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "ETF Management", description = "API per la gestione degli ETF")
public class ETFController {

    private final ETFService etfService;

    @Operation(summary = "Lista tutti gli ETF", description = "Restituisce l'elenco completo degli ETF disponibili")
    @GetMapping
    @Cacheable("etfs")
    public ResponseEntity<ApiResponse<List<ETFResponse>>> getAllETFs() {
        log.info("GET /api/v1/etfs - Recupero tutti gli ETF");

        List<ETFResponse> response = etfService.getAllETFs();
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovati %V5__Add_portfolio_id_to_simulations.sql ETF", response.size())));
    }

    @Operation(summary = "Recupera ETF per ID", description = "Restituisce i dettagli di un ETF specifico")
    @GetMapping("/{id}")
    @Cacheable(value = "etf", key = "#id")
    public ResponseEntity<ApiResponse<ETFResponse>> getETFById(
            @Parameter(description = "ID dell'ETF") @PathVariable String id) {
        log.info("GET /api/v1/etfs/{} - Recupero ETF", id);

        ETFResponse response = etfService.getETFById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Filtra ETF per livello di rischio", description = "Restituisce ETF filtrati per livello di rischio")
    @GetMapping("/filter/risk/{riskLevel}")
    public ResponseEntity<ApiResponse<List<ETFResponse>>> getETFsByRisk(
            @Parameter(description = "Livello di rischio (LOW, MEDIUM, HIGH, VERY_HIGH)")
            @PathVariable String riskLevel) {
        log.info("GET /api/v1/etfs/filter/risk/{} - Recupero ETF per livello di rischio", riskLevel);

        List<ETFResponse> response = etfService.getETFsByRisk(riskLevel);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovati %V5__Add_portfolio_id_to_simulations.sql ETF con rischio %s", response.size(), riskLevel)));
    }

    @Operation(summary = "ETF con migliori performance", description = "Restituisce i top ETF per performance")
    @GetMapping("/top-performing")
    @Cacheable("top-etfs")
    public ResponseEntity<ApiResponse<List<ETFResponse>>> getTopPerformingETFs(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/v1/etfs/top-performing - Recupero top {} ETF per performance", limit);

        List<ETFResponse> response = etfService.getTopPerformingETFs();
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Top %V5__Add_portfolio_id_to_simulations.sql ETF per performance", response.size())));
    }

    @Operation(summary = "ETF a basso costo", description = "Restituisce ETF con spese di gestione contenute")
    @GetMapping("/low-cost")
    public ResponseEntity<ApiResponse<List<ETFResponse>>> getLowCostETFs(
            @Parameter(description = "Spesa massima (%)")
            @RequestParam(defaultValue = "0.5") Double maxExpense) {
        log.info("GET /api/v1/etfs/low-cost - Recupero ETF con spese max: {}%", maxExpense);

        List<ETFResponse> response = etfService.getLowCostETFs(maxExpense);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovati %V5__Add_portfolio_id_to_simulations.sql ETF con spese ≤ %.2f%%", response.size(), maxExpense)));
    }
}