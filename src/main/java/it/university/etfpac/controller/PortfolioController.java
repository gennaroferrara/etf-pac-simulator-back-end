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

    @Operation(summary = "Lista portfolio paginate", description = "Restituisce portfolio con paginazione")
    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<ApiResponse<Page<PortfolioResponse>>> getUserPortfoliosPaged(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("GET /api/v1/portfolios/user/{}/paged - Recupero portfolio paginate", userId);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<PortfolioResponse> response = portfolioService.getUserPortfoliosPaged(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Pagina %d di %d, totale: %d portfolio",
                        page + 1, response.getTotalPages(), response.getTotalElements())));
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

    @Operation(summary = "Analisi rischio portfolio", description = "Calcola metriche di rischio per un portfolio")
    @PostMapping("/{id}/risk-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeRisk(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("POST /api/v1/portfolios/{}/risk-analysis - Analisi rischio", id);

        Map<String, Object> riskAnalysis = portfolioService.analyzePortfolioRisk(id);
        return ResponseEntity.ok(ApiResponse.success(riskAnalysis, "Analisi rischio completata"));
    }

    @Operation(summary = "Confronta portfolio", description = "Confronta performance teoriche di diversi portfolio")
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> comparePortfolios(
            @RequestBody Map<String, Object> comparisonRequest) {
        log.info("POST /api/v1/portfolios/compare - Confronto portfolio");

        @SuppressWarnings("unchecked")
        List<Long> portfolioIds = (List<Long>) comparisonRequest.get("portfolio_ids");

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

    @Operation(summary = "Attiva portfolio", description = "Attiva un portfolio per iniziare gli investimenti")
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<PortfolioResponse>> activatePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("POST /api/v1/portfolios/{}/activate - Attivazione portfolio", id);

        PortfolioResponse response = portfolioService.activatePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio attivato con successo"));
    }

    @Operation(summary = "Pausa portfolio", description = "Mette in pausa un portfolio attivo")
    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<PortfolioResponse>> pausePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("POST /api/v1/portfolios/{}/pause - Pausa portfolio", id);

        PortfolioResponse response = portfolioService.pausePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio messo in pausa"));
    }

    @Operation(summary = "Completa portfolio", description = "Completa un portfolio terminato")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<PortfolioResponse>> completePortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("POST /api/v1/portfolios/{}/complete - Completamento portfolio", id);

        PortfolioResponse response = portfolioService.completePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio completato"));
    }

    @Operation(summary = "Template portfolio", description = "Recupera template di portfolio predefiniti")
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getPortfolioTemplates(
            @RequestParam(required = false) String riskProfile) {
        log.info("GET /api/v1/portfolios/templates - Recupero template");

        List<PortfolioResponse> templates = portfolioService.getPortfolioTemplates(riskProfile);
        return ResponseEntity.ok(ApiResponse.success(templates,
                String.format("Trovati %d template", templates.size())));
    }

    @Operation(summary = "Salva come template", description = "Salva un portfolio come template riutilizzabile")
    @PostMapping("/{id}/save-as-template")
    public ResponseEntity<ApiResponse<PortfolioResponse>> saveAsTemplate(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestParam String templateName) {
        log.info("POST /api/v1/portfolios/{}/save-as-template - Template: {}", id, templateName);

        PortfolioResponse template = portfolioService.saveAsTemplate(id, templateName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(template, "Template salvato con successo"));
    }

    @Operation(summary = "Performance simulata", description = "Calcola performance teorica di un portfolio")
    @PostMapping("/{id}/simulate-performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulatePerformance(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestParam(defaultValue = "12") int months) {
        log.info("POST /api/v1/portfolios/{}/simulate-performance - {} mesi", id, months);

        Map<String, Object> performance = portfolioService.simulatePerformance(id, months);
        return ResponseEntity.ok(ApiResponse.success(performance, "Simulazione performance completata"));
    }

    @Operation(summary = "Diversificazione portfolio", description = "Analizza la diversificazione del portfolio")
    @GetMapping("/{id}/diversification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeDiversification(
            @Parameter(description = "ID del portfolio") @PathVariable Long id) {
        log.info("GET /api/v1/portfolios/{}/diversification - Analisi diversificazione", id);

        Map<String, Object> diversification = portfolioService.analyzeDiversification(id);
        return ResponseEntity.ok(ApiResponse.success(diversification, "Analisi diversificazione completata"));
    }

    @Operation(summary = "Ribilanciamento suggerito", description = "Calcola il ribilanciamento necessario")
    @PostMapping("/{id}/rebalance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateRebalancing(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestBody Map<String, Double> currentValues) {
        log.info("POST /api/v1/portfolios/{}/rebalance - Calcolo ribilanciamento", id);

        Map<String, Object> rebalancing = portfolioService.calculateRebalancing(id, currentValues);
        return ResponseEntity.ok(ApiResponse.success(rebalancing, "Ribilanciamento calcolato"));
    }

    @Operation(summary = "Export portfolio", description = "Esporta configurazione portfolio")
    @GetMapping("/{id}/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportPortfolio(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestParam(defaultValue = "json") String format) {
        log.info("GET /api/v1/portfolios/{}/export - Export formato {}", id, format);

        Map<String, Object> exportData = portfolioService.exportPortfolio(id, format);
        return ResponseEntity.ok(ApiResponse.success(exportData,
                String.format("Portfolio esportato in formato %s", format)));
    }

    @Operation(summary = "Statistiche portfolio utente", description = "Restituisce statistiche aggregate")
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPortfolioStatistics(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId) {
        log.info("GET /api/v1/portfolios/user/{}/statistics - Statistiche utente", userId);

        Map<String, Object> statistics = portfolioService.getUserPortfolioStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @Operation(summary = "Portfolio per strategia", description = "Filtra portfolio per strategia di investimento")
    @GetMapping("/strategy/{strategy}")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getPortfoliosByStrategy(
            @Parameter(description = "Strategia di investimento") @PathVariable String strategy) {
        log.info("GET /api/v1/portfolios/strategy/{} - Filtro per strategia", strategy);

        List<PortfolioResponse> portfolios = portfolioService.getPortfoliosByStrategy(strategy);
        return ResponseEntity.ok(ApiResponse.success(portfolios,
                String.format("Trovati %d portfolio con strategia %s", portfolios.size(), strategy)));
    }

    @Operation(summary = "Verifica compatibilità", description = "Verifica compatibilità portfolio con profilo utente")
    @PostMapping("/{id}/compatibility-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCompatibility(
            @Parameter(description = "ID del portfolio") @PathVariable Long id,
            @RequestParam Long userId) {
        log.info("POST /api/v1/portfolios/{}/compatibility-check - Utente {}", id, userId);

        Map<String, Object> compatibility = portfolioService.checkUserCompatibility(id, userId);
        return ResponseEntity.ok(ApiResponse.success(compatibility, "Verifica compatibilità completata"));
    }
}