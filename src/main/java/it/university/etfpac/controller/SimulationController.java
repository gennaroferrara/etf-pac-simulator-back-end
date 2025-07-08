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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Simulation Management", description = "API per la gestione delle simulazioni ETF PAC")
public class SimulationController {

    private final SimulationService simulationService;

    @Operation(summary = "Esegui simulazione", description = "Esegue una nuova simulazione con i parametri forniti")
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runSimulation(
            @Valid @RequestBody SimulationRequest request) {
        log.info("POST /api/v1/simulations/run - Esecuzione simulazione");

        try {
            Map<String, Object> result = simulationService.runSimulationDirect(request);
            return ResponseEntity.ok(ApiResponse.success(result, "Simulazione completata"));
        } catch (Exception e) {
            log.error("Errore durante simulazione", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Errore durante la simulazione: " + e.getMessage()));
        }
    }

    @Operation(summary = "Salva simulazione", description = "Salva i risultati di una simulazione")
    @PostMapping
    public ResponseEntity<ApiResponse<SimulationResponse>> saveSimulation(
            @Valid @RequestBody SimulationRequest request) {
        log.info("POST /api/v1/simulations - Salvataggio simulazione");

        try {
            SimulationResponse response = simulationService.createSimulation(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Simulazione salvata con successo"));
        } catch (Exception e) {
            log.error("Errore durante salvataggio simulazione", e);
            throw e;
        }
    }

    @Operation(summary = "Recupera simulazione per ID", description = "Restituisce i dettagli di una simulazione salvata")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SimulationResponse>> getSimulationById(
            @Parameter(description = "ID della simulazione") @PathVariable Long id) {
        log.info("GET /api/v1/simulations/{} - Recupero simulazione", id);

        SimulationResponse response = simulationService.getSimulationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lista simulazioni", description = "Restituisce tutte le simulazioni salvate con paginazione")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SimulationResponse>>> getAllSimulations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/simulations - Recupero tutte le simulazioni (page: {}, size: {})", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<SimulationResponse> response = simulationService.getAllSimulationsPaged(pageable);
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovate %d simulazioni", response.getTotalElements())));
    }

    @Operation(summary = "Confronta simulazioni", description = "Confronta performance di più simulazioni")
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareSimulations(
            @RequestBody Map<String, Object> comparisonRequest) {
        log.info("POST /api/v1/simulations/compare - Confronto simulazioni");

        @SuppressWarnings("unchecked")
        List<Long> simulationIds = (List<Long>) comparisonRequest.get("simulation_ids");

        if (simulationIds == null || simulationIds.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Almeno 2 simulazioni richieste per confronto"));
        }

        if (simulationIds.size() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Massimo 10 simulazioni confrontabili"));
        }

        Map<String, Object> comparison = simulationService.compareSimulations(simulationIds);
        return ResponseEntity.ok(ApiResponse.success(comparison, "Confronto completato"));
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


}