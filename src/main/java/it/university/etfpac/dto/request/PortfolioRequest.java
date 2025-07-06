package it.university.etfpac.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class PortfolioRequest {

    @NotBlank(message = "Nome portfolio richiesto")
    @Size(max = 100, message = "Nome portfolio non può superare 100 caratteri")
    private String name;

    @Size(max = 500, message = "Descrizione non può superare 500 caratteri")
    private String description;

    @NotNull(message = "ID utente richiesto")
    private Long userId;

    @NotNull(message = "Importo iniziale richiesto")
    @DecimalMin(value = "0.00", message = "Importo iniziale deve essere non negativo")
    private BigDecimal initialAmount;

    @NotNull(message = "Importo mensile richiesto")
    @DecimalMin(value = "0.00", message = "Importo mensile deve essere non negativo")
    private BigDecimal monthlyAmount;

    @NotNull(message = "Periodo di investimento richiesto")
    @Min(value = 1, message = "Periodo minimo 1 mese")
    @Max(value = 600, message = "Periodo massimo 600 mesi")
    private Integer investmentPeriodMonths;

    @NotBlank(message = "Frequenza richiesta")
    private String frequency; // WEEKLY, MONTHLY, QUARTERLY

    @NotBlank(message = "Strategia richiesta")
    private String strategy; // DCA, VALUE_AVERAGING, MOMENTUM, etc.

    @NotBlank(message = "Frequenza ribilanciamento richiesta")
    private String rebalanceFrequency; // NEVER, MONTHLY, QUARTERLY, etc.

    private Boolean automaticRebalance = true;

    @DecimalMin(value = "0.00", message = "Stop loss deve essere non negativo")
    @DecimalMax(value = "50.00", message = "Stop loss non può superare 50%")
    private BigDecimal stopLossPercentage = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Take profit deve essere non negativo")
    @DecimalMax(value = "200.00", message = "Take profit non può superare 200%")
    private BigDecimal takeProfitPercentage = BigDecimal.ZERO;

    @NotNull(message = "Allocazione ETF richiesta")
    @Size(min = 1, message = "Almeno un ETF deve essere selezionato")
    private Map<String, BigDecimal> etfAllocations;

    private Boolean isTemplate = false;

    private String status = "DRAFT"; // DRAFT, ACTIVE, PAUSED, COMPLETED, ARCHIVED
}