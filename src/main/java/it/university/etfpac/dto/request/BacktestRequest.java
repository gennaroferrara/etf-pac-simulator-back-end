package it.university.etfpac.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Map;

@Data
public class BacktestRequest {

    @NotBlank(message = "Nome backtest richiesto")
    private String name;

    @NotBlank(message = "Strategia richiesta")
    private String strategy;

    @NotNull(message = "Data inizio richiesta")
    private LocalDate startDate;

    @NotNull(message = "Data fine richiesta")
    private LocalDate endDate;

    @NotNull(message = "Importo iniziale richiesto")
    @DecimalMin(value = "1000.0", message = "Importo iniziale minimo €1000")
    private Double initialAmount;

    @NotNull(message = "Importo mensile richiesto")
    @DecimalMin(value = "100.0", message = "Importo mensile minimo €100")
    private Double monthlyAmount;

    @NotNull(message = "Allocazione ETF richiesta")
    @Size(min = 1, message = "Almeno un ETF deve essere selezionato")
    private Map<String, Double> etfAllocation;

    @NotBlank(message = "Frequenza richiesta")
    private String frequency;

    @NotBlank(message = "Periodo richiesto (1Y, 3Y, 5Y, 10Y)")
    private String period;

    private String riskTolerance = "MODERATE";

    private String rebalanceFrequency = "QUARTERLY";

    private Boolean automaticRebalance = true;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "50.0")
    private Double stopLoss;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private Double takeProfitTarget;

    private Boolean includeTransactionCosts = false;

    private Double transactionCostPercentage = 0.1;

    private Boolean includeDividends = true;

    private String benchmarkIndex = "SP500";

    @NotNull(message = "ID utente richiesto")
    private Long userId;
}