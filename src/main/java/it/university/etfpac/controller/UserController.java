package it.university.etfpac.controller;

import it.university.etfpac.dto.request.UserRequest;
import it.university.etfpac.dto.response.ApiResponse;
import it.university.etfpac.dto.response.UserResponse;
import it.university.etfpac.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "User Management", description = "API base per gestione utenti")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Crea utente demo", description = "Crea un utente per demo/test")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {
        log.info("POST /api/users - Creazione utente demo: {}", request.getEmail());

        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Utente creato"));
    }

    @Operation(summary = "Lista utenti", description = "Restituisce lista utenti per selezione")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("GET /api/users - Recupero utenti");

        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}