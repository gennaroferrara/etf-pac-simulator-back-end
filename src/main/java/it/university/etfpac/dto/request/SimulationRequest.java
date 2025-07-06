package it.university.etfpac.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.util.Map;

@Data
public class SimulationRequest {

    @NotBlank(message = "Nome simulazione richiesto")
    private String name;

    @NotNull(message = "Importo iniziale richiesto")
    @DecimalMin(value = "100.0", message = "Importo iniziale minimo €100")
    private Double initialAmount;

    @NotNull(message = "Importo mensile richiesto")
    @DecimalMin(value = "50.0", message = "Importo mensile minimo €50")
    private Double monthlyAmount;

    @NotNull(message = "Periodo investimento richiesto")
    @Min(value = 6, message = "Periodo minimo 6 mesi")
    @Max(value = 600, message = "Periodo massimo 600 mesi")
    private Integer investmentPeriod;

    @NotBlank(message = "Frequenza richiesta")
    private String frequency;

    @NotBlank(message = "Strategia richiesta")
    private String strategy;

    @NotNull(message = "Allocazione ETF richiesta")
    @Size(min = 1, message = "Almeno un ETF deve essere selezionato")
    private Map<String, Double> etfAllocation;

    @NotBlank(message = "Tolleranza al rischio richiesta")
    private String riskTolerance;

    @NotBlank(message = "Frequenza ribilanciamento richiesta")
    private String rebalanceFrequency;

    private Boolean automaticRebalance = true;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "50.0")
    private Double stopLoss;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private Double takeProfitTarget;

    @NotNull(message = "ID utente richiesto")
    private Long userId;
}