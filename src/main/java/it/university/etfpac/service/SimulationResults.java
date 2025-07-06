package it.university.etfpac.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulationResults {
    private Double finalValue;
    private Double totalInvested;
    private Double cumulativeReturn;
    private Double volatility;
    private Double maxDrawdown;
    private Double sharpeRatio;
    private Double winRate;
}