package it.university.etfpac.controller;

import it.university.etfpac.dto.request.SimulationRequest;
import it.university.etfpac.dto.response.ApiResponse;
import it.university.etfpac.dto.response.SimulationResponse;
import it.university.etfpac.service.SimulationService;
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
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Simulation Management", description = "API per la gestione delle simulazioni ETF PAC")
public class SimulationController {

    private final SimulationService simulationService;

    @Operation(summary = "Crea e avvia nuova simulazione", description = "Crea una nuova simulazione e la avvia in modalità asincrona")
    @PostMapping
    public ResponseEntity<ApiResponse<SimulationResponse>> createSimulation(
            @Valid @RequestBody SimulationRequest request) {
        log.info("POST /api/v1/simulations - Creazione nuova simulazione: {}", request.getName());

        try {
            SimulationResponse response = simulationService.createSimulation(request);

            // Avvia simulazione in modo asincrono
            simulationService.runSimulation(response.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Simulazione creata e avviata con successo"));
        } catch (Exception e) {
            log.error("Errore durante creazione simulazione", e);
            throw e;
        }
    }

    @Operation(summary = "Avvia simulazione avanzata", description = "Esegue simulazione con parametri avanzati")
    @PostMapping("/run-advanced")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runAdvancedSimulation(
            @Valid @RequestBody SimulationRequest request) {
        log.info("POST /api/v1/simulations/run-advanced - Simulazione avanzata: {}", request.getName());

        try {
            // Crea simulazione
            SimulationResponse simulation = simulationService.createSimulation(request);

            // Esegui simulazione asincrona e ottieni CompletableFuture
            CompletableFuture<Map<String, Object>> futureResult = simulationService.runAdvancedSimulation(simulation.getId());

            // Per ora restituiamo info sulla simulazione avviata
            Map<String, Object> response = Map.of(
                    "simulation_id", simulation.getId(),
                    "status", "RUNNING",
                    "message", "Simulazione avanzata avviata con successo",
                    "estimated_completion_time", "2-3 minuti"
            );

            return ResponseEntity.ok(ApiResponse.success(response, "Simulazione avanzata avviata"));
        } catch (Exception e) {
            log.error("Errore durante simulazione avanzata", e);
            throw e;
        }
    }

    @Operation(summary = "Recupera simulazione per ID", description = "Restituisce i dettagli completi di una simulazione")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SimulationResponse>> getSimulationById(
            @Parameter(description = "ID della simulazione") @PathVariable Long id) {
        log.info("GET /api/v1/simulations/{} - Recupero simulazione", id);

        SimulationResponse response = simulationService.getSimulationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lista simulazioni utente", description = "Restituisce tutte le simulazioni di un utente")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<SimulationResponse>>> getUserSimulations(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId) {
        log.info("GET /api/v1/simulations/user/{} - Recupero simulazioni utente", userId);

        List<SimulationResponse> response = simulationService.getUserSimulations(userId);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovate %d simulazioni", response.size())));
    }

    @Operation(summary = "Lista simulazioni paginate", description = "Restituisce simulazioni con paginazione")
    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<ApiResponse<Page<SimulationResponse>>> getUserSimulationsPaged(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("GET /api/v1/simulations/user/{}/paged - Recupero simulazioni paginate", userId);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<SimulationResponse> response = simulationService.getUserSimulationsPaged(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Pagina %d di %d, totale: %d simulazioni",
                        page + 1, response.getTotalPages(), response.getTotalElements())));
    }

    @Operation(summary = "Migliori simulazioni", description = "Restituisce le simulazioni con migliori performance")
    @GetMapping("/user/{userId}/best-performing")
    public ResponseEntity<ApiResponse<List<SimulationResponse>>> getBestPerformingSimulations(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("GET /api/v1/simulations/user/{}/best-performing - Top {} simulazioni", userId, limit);

        List<SimulationResponse> response = simulationService.getBestPerformingSimulations(userId);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Top %d simulazioni per performance", response.size())));
    }

    @Operation(summary = "Stato simulazione", description = "Verifica lo stato di esecuzione di una simulazione")
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSimulationStatus(
            @Parameter(description = "ID della simulazione") @PathVariable Long id) {
        log.info("GET /api/v1/simulations/{}/status - Verifica stato", id);

        Map<String, Object> status = simulationService.getSimulationStatus(id);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @Operation(summary = "Risultati simulazione", description = "Recupera i risultati dettagliati di una simulazione completata")
    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSimulationResults(
            @Parameter(description = "ID della simulazione") @PathVariable Long id) {
        log.info("GET /api/v1/simulations/{}/results - Recupero risultati", id);

        Map<String, Object> results = simulationService.getDetailedResults(id);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Aggiorna simulazione", description = "Modifica i parametri di una simulazione esistente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SimulationResponse>> updateSimulation(
            @Parameter(description = "ID della simulazione") @PathVariable Long id,
            @Valid @RequestBody SimulationRequest request) {
        log.info("PUT /api/v1/simulations/{} - Aggiornamento simulazione", id);

        SimulationResponse response = simulationService.updateSimulation(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Simulazione aggiornata con successo"));
    }

    @Operation(summary = "Elimina simulazione", description = "Rimuove una simulazione e tutti i dati correlati")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSimulation(
            @Parameter(description = "ID della simulazione") @PathVariable Long id) {
        log.info("DELETE /api/v1/simulations/{} - Eliminazione simulazione", id);

        simulationService.deleteSimulation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Simulazione eliminata con successo"));
    }

    @Operation(summary = "Clona simulazione", description = "Crea una copia di una simulazione esistente")
    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<SimulationResponse>> cloneSimulation(
            @Parameter(description = "ID della simulazione da clonare") @PathVariable Long id,
            @RequestParam(required = false) String newName) {
        log.info("POST /api/v1/simulations/{}/clone - Clonazione simulazione", id);

        SimulationResponse response = simulationService.cloneSimulation(id, newName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Simulazione clonata con successo"));
    }

    @Operation(summary = "Statistiche simulazioni utente", description = "Restituisce statistiche aggregate delle simulazioni")
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserSimulationStatistics(
            @Parameter(description = "ID dell'utente") @PathVariable Long userId) {
        log.info("GET /api/v1/simulations/user/{}/statistics - Statistiche utente", userId);

        Map<String, Object> statistics = simulationService.getUserStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @Operation(summary = "Export simulazione", description = "Esporta i risultati di una simulazione in formato JSON")
    @GetMapping("/{id}/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportSimulation(
            @Parameter(description = "ID della simulazione") @PathVariable Long id,
            @RequestParam(defaultValue = "json") String format) {
        log.info("GET /api/v1/simulations/{}/export - Export formato {}", id, format);

        Map<String, Object> exportData = simulationService.exportSimulation(id, format);
        return ResponseEntity.ok(ApiResponse.success(exportData,
                String.format("Simulazione esportata in formato %s", format)));
    }

    @Operation(summary = "Confronta simulazioni", description = "Confronta performance di più simulazioni")
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareSimulations(
            @RequestBody Map<String, Object> comparisonRequest) {
        log.info("POST /api/v1/simulations/compare - Confronto simulazioni");

        @SuppressWarnings("unchecked")
        List<Long> simulationIds = (List<Long>) comparisonRequest.get("simulation_ids");

        Map<String, Object> comparison = simulationService.compareSimulations(simulationIds);
        return ResponseEntity.ok(ApiResponse.success(comparison, "Confronto completato"));
    }

    @Operation(summary = "Salva configurazione come template", description = "Salva la configurazione di una simulazione come template riutilizzabile")
    @PostMapping("/{id}/save-as-template")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveAsTemplate(
            @Parameter(description = "ID della simulazione") @PathVariable Long id,
            @RequestParam String templateName) {
        log.info("POST /api/v1/simulations/{}/save-as-template - Template: {}", id, templateName);

        Map<String, Object> template = simulationService.saveAsTemplate(id, templateName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(template, "Template salvato con successo"));
    }
}