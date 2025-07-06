package it.university.etfpac.dto.response;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String riskProfile;
    private String experience;
    private Double totalPortfolio;
    private Integer activeSimulations;
    private LocalDateTime createdAt;
}