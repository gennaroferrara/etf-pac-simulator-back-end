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

    // NUOVI ENDPOINT AGGIUNTI

    @Operation(summary = "Lista template portfolio", description = "Restituisce i template di portfolio predefiniti")
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getPortfolioTemplates(
            @RequestParam(required = false) String riskProfile) {
        log.info("GET /api/v1/portfolios/templates - Recupero template portfolio");

        List<PortfolioResponse> response = portfolioService.getPortfolioTemplates(riskProfile);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovati %d template", response.size())));
    }

    @Operation(summary = "Simula performance portfolio", description = "Simula la performance futura del portfolio")
    @PostMapping("/{id}/simulate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulatePerformance(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestParam(defaultValue = "60") int months) {
        log.info("POST /api/v1/portfolios/{}/simulate - Simulazione performance per {} mesi", id, months);

        Map<String, Object> simulation = portfolioService.simulatePerformance(id, months);
        return ResponseEntity.ok(ApiResponse.success(simulation, "Simulazione completata"));
    }

    @Operation(summary = "Analizza diversificazione", description = "Analizza il livello di diversificazione del portfolio")
    @GetMapping("/{id}/diversification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeDiversification(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("GET /api/v1/portfolios/{}/diversification - Analisi diversificazione", id);

        Map<String, Object> analysis = portfolioService.analyzeDiversification(id);
        return ResponseEntity.ok(ApiResponse.success(analysis, "Analisi completata"));
    }

    @Operation(summary = "Calcola ribilanciamento", description = "Calcola le operazioni necessarie per ribilanciare il portfolio")
    @PostMapping("/{id}/rebalance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateRebalancing(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestBody Map<String, Double> currentValues) {
        log.info("POST /api/v1/portfolios/{}/rebalance - Calcolo ribilanciamento", id);

        Map<String, Object> rebalancing = portfolioService.calculateRebalancing(id, currentValues);
        return ResponseEntity.ok(ApiResponse.success(rebalancing, "Calcolo completato"));
    }

    @Operation(summary = "Esporta portfolio", description = "Esporta i dettagli del portfolio nel formato richiesto")
    @PostMapping("/{id}/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportPortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestParam(defaultValue = "PDF") String format) {
        log.info("POST /api/v1/portfolios/{}/export - Export portfolio in formato {}", id, format);

        Map<String, Object> exportData = portfolioService.exportPortfolio(id, format);
        return ResponseEntity.ok(ApiResponse.success(exportData, "Export completato"));
    }

    @Operation(summary = "Analizza rischio portfolio", description = "Analizza il profilo di rischio del portfolio")
    @GetMapping("/{id}/risk-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzePortfolioRisk(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("GET /api/v1/portfolios/{}/risk-analysis - Analisi rischio", id);

        Map<String, Object> riskAnalysis = portfolioService.analyzePortfolioRisk(id);
        return ResponseEntity.ok(ApiResponse.success(riskAnalysis, "Analisi rischio completata"));
    }

    @Operation(summary = "Confronta portfolio", description = "Confronta più portfolio tra loro")
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> comparePortfolios(
            @RequestBody List<Long> portfolioIds) {
        log.info("POST /api/v1/portfolios/compare - Confronto {} portfolio", portfolioIds.size());

        if (portfolioIds.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Almeno 2 portfolio richiesti per confronto"));
        }

        Map<String, Object> comparison = portfolioService.comparePortfolios(portfolioIds);
        return ResponseEntity.ok(ApiResponse.success(comparison, "Confronto completato"));
    }

    @Operation(summary = "Clona portfolio", description = "Crea una copia di un portfolio esistente")
    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<PortfolioResponse>> clonePortfolio(
            @Parameter(description = "ID del portfolio da clonare") @PathVariable Long id,
            @RequestParam(required = false) String newName) {
        log.info("POST /api/v1/portfolios/{}/clone - Clonazione portfolio", id);

        PortfolioResponse response = portfolioService.clonePortfolio(id, newName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Portfolio clonato con successo"));
    }

    @Operation(summary = "Attiva portfolio", description = "Attiva un portfolio in stato DRAFT")
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<PortfolioResponse>> activatePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("POST /api/v1/portfolios/{}/activate - Attivazione portfolio", id);

        PortfolioResponse response = portfolioService.activatePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio attivato"));
    }

    @Operation(summary = "Pausa portfolio", description = "Mette in pausa un portfolio attivo")
    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<PortfolioResponse>> pausePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("POST /api/v1/portfolios/{}/pause - Pausa portfolio", id);

        PortfolioResponse response = portfolioService.pausePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio messo in pausa"));
    }

    @Operation(summary = "Completa portfolio", description = "Marca un portfolio come completato")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<PortfolioResponse>> completePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("POST /api/v1/portfolios/{}/complete - Completamento portfolio", id);

        PortfolioResponse response = portfolioService.completePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio completato"));
    }

    @Operation(summary = "Salva come template", description = "Salva un portfolio come template riutilizzabile")
    @PostMapping("/{id}/save-as-template")
    public ResponseEntity<ApiResponse<PortfolioResponse>> saveAsTemplate(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestParam String templateName) {
        log.info("POST /api/v1/portfolios/{}/save-as-template - Salvataggio template", id);

        PortfolioResponse response = portfolioService.saveAsTemplate(id, templateName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Template salvato con successo"));
    }

    @Operation(summary = "Statistiche portfolio utente", description = "Restituisce statistiche aggregate sui portfolio di un utente")
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPortfolioStatistics(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId) {
        log.info("GET /api/v1/portfolios/user/{}/statistics - Statistiche portfolio utente", userId);

        Map<String, Object> statistics = portfolioService.getUserPortfolioStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistiche calcolate"));
    }

    @Operation(summary = "Portfolio per strategia", description = "Restituisce portfolio filtrati per strategia")
    @GetMapping("/by-strategy/{strategy}")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getPortfoliosByStrategy(
            @Parameter(description = "Strategia di investimento") @PathVariable String strategy) {
        log.info("GET /api/v1/portfolios/by-strategy/{} - Portfolio per strategia", strategy);

        List<PortfolioResponse> response = portfolioService.getPortfoliosByStrategy(strategy);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovati %d portfolio con strategia %s", response.size(), strategy)));
    }

    @Operation(summary = "Verifica compatibilità utente", description = "Verifica se un portfolio è compatibile con il profilo di rischio dell'utente")
    @GetMapping("/{portfolioId}/compatibility/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUserCompatibility(
            @Parameter(description = "ID del portfolio") @PathVariable Long portfolioId,
            @Parameter(description = "ID dell'utente") @PathVariable Long userId) {
        log.info("GET /api/v1/portfolios/{}/compatibility/{} - Verifica compatibilità", portfolioId, userId);

        Map<String, Object> compatibility = portfolioService.checkUserCompatibility(portfolioId, userId);
        return ResponseEntity.ok(ApiResponse.success(compatibility, "Verifica completata"));
    }
}