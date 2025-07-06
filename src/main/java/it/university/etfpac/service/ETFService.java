package it.university.etfpac.service;

import it.university.etfpac.dto.response.ETFResponse;
import it.university.etfpac.entity.ETF;
import it.university.etfpac.exception.ResourceNotFoundException;
import it.university.etfpac.repository.ETFRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ETFService {

    private final ETFRepository etfRepository;

    public List<ETFResponse> getAllETFs() {
        log.info("Recupero tutti gli ETF");
        return etfRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ETFResponse getETFById(String id) {
        log.info("Recupero ETF con ID: {}", id);
        ETF etf = etfRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ETF non trovato con ID: " + id));
        return convertToResponse(etf);
    }

    public List<ETFResponse> getETFsByRisk(String riskLevel) {
        log.info("Recupero ETF per livello di rischio: {}", riskLevel);
        ETF.RiskLevel risk = ETF.RiskLevel.valueOf(riskLevel.toUpperCase());
        return etfRepository.findByRisk(risk).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ETFResponse> getTopPerformingETFs() {
        log.info("Recupero ETF con migliori performance");
        return etfRepository.findTopPerformingETFs().stream()
                .limit(10)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ETFResponse> getLowCostETFs(Double maxExpense) {
        log.info("Recupero ETF a basso costo con spese max: {}", maxExpense);
        return etfRepository.findLowCostETFs(maxExpense).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ETFResponse convertToResponse(ETF etf) {
        return ETFResponse.builder()
                .id(etf.getId())
                .name(etf.getName())
                .ticker(etf.getTicker())
                .expense(etf.getExpense())
                .risk(etf.getRisk().name().toLowerCase())
                .sector(etf.getSector())
                .aum(etf.getAum())
                .dividend(etf.getDividend())
                .beta(etf.getBeta())
                .sharpe(etf.getSharpe())
                .maxDrawdown(etf.getMaxDrawdown())
                .ytd(etf.getYtd())
                .oneYear(etf.getOneYear())
                .threeYear(etf.getThreeYear())
                .fiveYear(etf.getFiveYear())
                .build();
    }
}