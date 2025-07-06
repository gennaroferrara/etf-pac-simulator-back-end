package it.university.etfpac.dto.response;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ETFResponse {
    private String id;
    private String name;
    private String ticker;
    private Double expense;
    private String risk;
    private String sector;
    private String aum;
    private String dividend;
    private Double beta;
    private Double sharpe;
    private Double maxDrawdown;
    private Double ytd;
    private Double oneYear;
    private Double threeYear;
    private Double fiveYear;
}