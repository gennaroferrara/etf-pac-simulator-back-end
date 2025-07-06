package it.university.etfpac.controller;

import it.university.etfpac.dto.request.UserRequest;
import it.university.etfpac.dto.response.ApiResponse;
import it.university.etfpac.dto.response.UserResponse;
import it.university.etfpac.service.UserService;
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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "User Management", description = "API per la gestione degli utenti")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Crea nuovo utente", description = "Registra un nuovo utente nel sistema")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {
        log.info("POST /api/v1/users - Creazione nuovo utente: {}", request.getEmail());

        try {
            UserResponse response = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Utente creato con successo"));
        } catch (Exception e) {
            log.error("Errore durante creazione utente", e);
            throw e;
        }
    }

    @Operation(summary = "Recupera utente per ID", description = "Restituisce i dettagli di un utente specifico")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "ID dell'utente") @PathVariable Long id) {
        log.info("GET /api/v1/users/{} - Recupero utente", id);

        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Recupera utente per email", description = "Trova un utente tramite indirizzo email")
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(
            @Parameter(description = "Email dell'utente") @PathVariable String email) {
        log.info("GET /api/v1/users/email/{} - Recupero utente per email", email);

        UserResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lista tutti gli utenti", description = "Restituisce l'elenco di tutti gli utenti registrati")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/users - Recupero tutti gli utenti (page: {}, size: {})", page, size);

        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Trovati %V5__Add_portfolio_id_to_simulations.sql utenti", response.size())));
    }

    @Operation(summary = "Aggiorna utente", description = "Modifica i dati di un utente esistente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "ID dell'utente") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        log.info("PUT /api/v1/users/{} - Aggiornamento utente", id);

        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Utente aggiornato con successo"));
    }

    @Operation(summary = "Elimina utente", description = "Rimuove un utente dal sistema")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "ID dell'utente") @PathVariable Long id) {
        log.info("DELETE /api/v1/users/{} - Eliminazione utente", id);

        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Utente eliminato con successo"));
    }
}