package it.university.etfpac.controller;

import it.university.etfpac.dto.request.PortfolioRequest;
import it.university.etfpac.dto.response.ApiResponse;
import it.university.etfpac.dto.response.PortfolioResponse;
import it.university.etfpac.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Portfolio Management", description = "API per la gestione dei portfolio ETF")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "Crea nuovo portfolio", description = "Crea un nuovo portfolio con allocazione ETF")
    @PostMapping
    public ResponseEntity<ApiResponse<PortfolioResponse>> createPortfolio(
            @Valid @RequestBody PortfolioRequest request) {
        log.info("POST /api/v1/portfolios - Creazione nuovo portfolio: {}", request.getName());

        try {
            PortfolioResponse response = portfolioService.createPortfolio(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Portfolio creato con successo"));
        } catch (Exception e) {
            log.error("Errore durante creazione portfolio", e);
            throw e;
        }
    }

    @Operation(summary = "Recupera portfolio per ID", description = "Restituisce i dettagli completi di un portfolio")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolioById(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("GET /api/v1/portfolios/{} - Recupero portfolio", id);

        PortfolioResponse response = portfolioService.getPortfolioById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lista portfolio utente", description = "Restituisce tutti i portfolio di un utente")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getUserPortfolios(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean includeTemplates) {
        log.info("GET /api/v1/portfolios/user/{} - Recupero portfolio utente", userId);

        List<PortfolioResponse> response = portfolioService.getUserPortfolios(userId, includeTemplates);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovati %d portfolio", response.size())));
    }

    @Operation(summary = "Aggiorna portfolio", description = "Modifica un portfolio esistente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PortfolioResponse>> updatePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @Valid @RequestBody PortfolioRequest request) {
        log.info("PUT /api/v1/portfolios/{} - Aggiornamento portfolio", id);

        PortfolioResponse response = portfolioService.updatePortfolio(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio aggiornato con successo"));
    }

    @Operation(summary = "Elimina portfolio", description = "Rimuove un portfolio e tutte le simulazioni correlate")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("DELETE /api/v1/portfolios/{} - Eliminazione portfolio", id);

        portfolioService.deletePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Portfolio eliminato con successo"));
    }

    @Operation(summary = "Valida allocazione ETF", description = "Verifica che l'allocazione ETF sia valida")
    @PostMapping("/validate-allocation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateAllocation(
            @RequestBody Map<String, Double> etfAllocation) {
        log.info("POST /api/v1/portfolios/validate-allocation - Validazione allocazione");

        Map<String, Object> validation = portfolioService.validateEtfAllocation(etfAllocation);
        return ResponseEntity.ok(ApiResponse.success(validation));
    }

    @Operation(summary = "Ottimizza portfolio", description = "Suggerisce un'allocazione ottimale basata sul profilo di rischio")
    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> optimizePortfolio(
            @RequestBody Map<String, Object> optimizationRequest) {
        log.info("POST /api/v1/portfolios/optimize - Ottimizzazione portfolio");

        Map<String, Object> optimization = portfolioService.optimizePortfolio(optimizationRequest);
        return ResponseEntity.ok(ApiResponse.success(optimization, "Ottimizzazione completata"));
    }
}