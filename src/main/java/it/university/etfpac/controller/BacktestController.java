package it.university.etfpac.controller;

import it.university.etfpac.dto.request.BacktestRequest;
import it.university.etfpac.dto.response.ApiResponse;
import it.university.etfpac.service.BacktestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/backtest")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Backtesting", description = "API per backtesting storico delle strategie")
public class BacktestController {

    private final BacktestService backtestService;

    @Operation(summary = "Avvia backtest", description = "Esegue backtest su dati storici")
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runBacktest(
            @Valid @RequestBody BacktestRequest request) {
        log.info("POST /api/v1/backtest/run - Avvio backtest strategia {} periodo {}",
                request.getStrategy(), request.getPeriod());

        Map<String, Object> response = backtestService.runBacktest(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Backtest completato"));
    }

    @Operation(summary = "Confronta strategie", description = "Confronta performance di diverse strategie")
    @PostMapping("/compare-strategies")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareStrategies(
            @RequestBody Map<String, Object> comparisonRequest) {
        log.info("POST /api/v1/backtest/compare-strategies - Confronto strategie");

        Map<String, Object> response = backtestService.compareStrategies(comparisonRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Confronto completato"));
    }

    @Operation(summary = "Risultati backtest", description = "Recupera risultati di un backtest precedente")
    @GetMapping("/{backtestId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBacktestResults(
            @Parameter(description = "ID del backtest") @PathVariable Long backtestId) {
        log.info("GET /api/v1/backtest/{} - Recupero risultati backtest", backtestId);

        Map<String, Object> response = backtestService.getBacktestResults(backtestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}