package it.university.etfpac.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UserRequest {

    @NotBlank(message = "Nome richiesto")
    private String name;

    @Email(message = "Email non valida")
    @NotBlank(message = "Email richiesta")
    private String email;

    @NotBlank(message = "Profilo di rischio richiesto")
    private String riskProfile;

    @NotBlank(message = "Esperienza richiesta")
    private String experience;

    @DecimalMin(value = "0.0", message = "Portfolio totale deve essere positivo")
    private Double totalPortfolio;
}