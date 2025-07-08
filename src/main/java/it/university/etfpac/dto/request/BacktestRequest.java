package it.university.etfpac.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Map;

@Data
public class BacktestRequest {

    @NotBlank(message = "Nome backtest richiesto")
    @Size(max = 255, message = "Nome non può superare 255 caratteri")
    private String name;

    @NotBlank(message = "Strategia richiesta")
    @Pattern(regexp = "DCA|VALUE_AVERAGING|MOMENTUM|CONTRARIAN|SMART_BETA|TACTICAL",
            message = "Strategia non valida")
    private String strategy;

    @NotNull(message = "Data inizio richiesta")
    @PastOrPresent(message = "Data inizio non può essere futura")
    private LocalDate startDate;

    @NotNull(message = "Data fine richiesta")
    private LocalDate endDate;

    @NotNull(message = "Importo iniziale richiesto")
    @DecimalMin(value = "1000.0", message = "Importo iniziale minimo €1000")
    @Positive(message = "Importo iniziale deve essere positivo")
    private Double initialAmount;

    @NotNull(message = "Importo mensile richiesto")
    @DecimalMin(value = "100.0", message = "Importo mensile minimo €100")
    @Positive(message = "Importo mensile deve essere positivo")
    private Double monthlyAmount;

    @NotNull(message = "Allocazione ETF richiesta")
    @Size(min = 1, message = "Almeno un ETF deve essere selezionato")
    private Map<String, Double> etfAllocation;

    @NotBlank(message = "Frequenza richiesta")
    @Pattern(regexp = "WEEKLY|MONTHLY|QUARTERLY", message = "Frequenza non valida")
    private String frequency;

    @NotBlank(message = "Periodo richiesto")
    @Pattern(regexp = "1Y|3Y|5Y|10Y", message = "Periodo deve essere 1Y, 3Y, 5Y o 10Y")
    private String period;

    @Pattern(regexp = "CONSERVATIVE|MODERATE|AGGRESSIVE", message = "Tolleranza rischio non valida")
    private String riskTolerance = "MODERATE";

    @Pattern(regexp = "MONTHLY|QUARTERLY|SEMIANNUAL|ANNUAL", message = "Frequenza ribilanciamento non valida")
    private String rebalanceFrequency = "QUARTERLY";

    private Boolean automaticRebalance = true;

    @DecimalMin(value = "0.0", message = "Stop loss non può essere negativo")
    @DecimalMax(value = "50.0", message = "Stop loss non può superare 50%")
    private Double stopLoss;

    @DecimalMin(value = "0.0", message = "Take profit non può essere negativo")
    @DecimalMax(value = "100.0", message = "Take profit non può superare 100%")
    private Double takeProfitTarget;

    private Boolean includeTransactionCosts = false;

    @DecimalMin(value = "0.0", message = "Costi transazione non possono essere negativi")
    @DecimalMax(value = "5.0", message = "Costi transazione non possono superare 5%")
    private Double transactionCostPercentage = 0.1;

    private Boolean includeDividends = true;

    @Pattern(regexp = "SP500|FTSE_MIB|EURO_STOXX_50|MSCI_WORLD",
            message = "Indice benchmark non valido")
    private String benchmarkIndex = "SP500";

    @NotNull(message = "ID utente richiesto")
    @Positive(message = "ID utente deve essere positivo")
    private Long userId;

    @AssertTrue(message = "Data fine deve essere successiva a data inizio")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "La somma delle allocazioni ETF deve essere 100%")
    public boolean isAllocationValid() {
        if (etfAllocation == null || etfAllocation.isEmpty()) {
            return true; // Le altre validazioni gestiranno questo caso
        }
        double sum = etfAllocation.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        return Math.abs(sum - 100.0) < 0.01;
    }
}