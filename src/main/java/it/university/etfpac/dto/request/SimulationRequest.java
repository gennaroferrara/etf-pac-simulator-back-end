package it.university.etfpac.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.util.Map;

@Data
public class SimulationRequest {

    @NotBlank(message = "Nome simulazione richiesto")
    @Size(max = 255, message = "Nome non può superare 255 caratteri")
    private String name;

    @NotNull(message = "Importo iniziale richiesto")
    @DecimalMin(value = "100.0", message = "Importo iniziale minimo €100")
    @Positive(message = "Importo iniziale deve essere positivo")
    private Double initialAmount;

    @NotNull(message = "Importo mensile richiesto")
    @DecimalMin(value = "50.0", message = "Importo mensile minimo €50")
    @PositiveOrZero(message = "Importo mensile non può essere negativo")
    private Double monthlyAmount;

    @NotNull(message = "Periodo investimento richiesto")
    @Min(value = 6, message = "Periodo minimo 6 mesi")
    @Max(value = 600, message = "Periodo massimo 600 mesi")
    private Integer investmentPeriod;

    @NotBlank(message = "Frequenza richiesta")
    @Pattern(regexp = "MONTHLY|QUARTERLY|SEMIANNUAL|ANNUAL",
            message = "Frequenza non valida")
    private String frequency;

    @NotBlank(message = "Strategia richiesta")
    @Pattern(regexp = "DCA|VALUE_AVERAGING|MOMENTUM|CONTRARIAN|SMART_BETA|TACTICAL",
            message = "Strategia non valida")
    private String strategy;

    @NotNull(message = "Allocazione ETF richiesta")
    @Size(min = 1, message = "Almeno un ETF deve essere selezionato")
    private Map<String, Double> etfAllocation;

    @NotBlank(message = "Tolleranza al rischio richiesta")
    @Pattern(regexp = "CONSERVATIVE|MODERATE|AGGRESSIVE",
            message = "Tolleranza rischio non valida")
    private String riskTolerance;

    @NotBlank(message = "Frequenza ribilanciamento richiesta")
    @Pattern(regexp = "MONTHLY|QUARTERLY|SEMIANNUAL|ANNUAL",
            message = "Frequenza ribilanciamento non valida")
    private String rebalanceFrequency;

    private Boolean automaticRebalance = true;

    @DecimalMin(value = "0.0", message = "Stop loss non può essere negativo")
    @DecimalMax(value = "50.0", message = "Stop loss non può superare 50%")
    private Double stopLoss;

    @DecimalMin(value = "0.0", message = "Take profit non può essere negativo")
    @DecimalMax(value = "100.0", message = "Take profit non può superare 100%")
    private Double takeProfitTarget;

    @NotNull(message = "ID utente richiesto")
    @Positive(message = "ID utente deve essere positivo")
    private Long userId;

    @AssertTrue(message = "La somma delle allocazioni ETF deve essere 100%")
    public boolean isAllocationValid() {
        if (etfAllocation == null || etfAllocation.isEmpty()) {
            return true;
        }
        double sum = etfAllocation.values().stream()
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        return Math.abs(sum - 100.0) < 0.01;
    }

    @AssertTrue(message = "Tutte le allocazioni ETF devono essere positive")
    public boolean areAllAllocationsPositive() {
        if (etfAllocation == null) {
            return true;
        }
        return etfAllocation.values().stream()
                .filter(value -> value != null)
                .allMatch(value -> value >= 0);
    }
}