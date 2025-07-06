package it.university.etfpac.service;

import it.university.etfpac.dto.request.UserRequest;
import it.university.etfpac.dto.response.UserResponse;
import it.university.etfpac.entity.User;
import it.university.etfpac.exception.BadRequestException;
import it.university.etfpac.exception.ResourceNotFoundException;
import it.university.etfpac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserResponse createUser(UserRequest request) {
        log.info("Creazione nuovo utente: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email già esistente: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRiskProfile(User.RiskProfile.valueOf(request.getRiskProfile().toUpperCase()));
        user.setExperience(User.Experience.valueOf(request.getExperience().toUpperCase()));
        user.setTotalPortfolio(request.getTotalPortfolio());
        user.setActiveSimulations(0);

        User savedUser = userRepository.save(user);
        log.info("Utente creato con successo con ID: {}", savedUser.getId());

        return convertToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Recupero utente con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato con ID: " + id));
        return convertToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.info("Recupero utente con email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato con email: " + email));
        return convertToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Recupero tutti gli utenti");
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Aggiornamento utente con ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato con ID: " + id));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email già esistente: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRiskProfile(User.RiskProfile.valueOf(request.getRiskProfile().toUpperCase()));
        user.setExperience(User.Experience.valueOf(request.getExperience().toUpperCase()));
        user.setTotalPortfolio(request.getTotalPortfolio());

        User updatedUser = userRepository.save(user);
        log.info("Utente aggiornato con successo");

        return convertToResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        log.info("Eliminazione utente con ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utente non trovato con ID: " + id);
        }

        userRepository.deleteById(id);
        log.info("Utente eliminato con successo");
    }

    public void incrementActiveSimulations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato con ID: " + userId));
        user.setActiveSimulations(user.getActiveSimulations() + 1);
        userRepository.save(user);
    }

    public void decrementActiveSimulations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato con ID: " + userId));
        user.setActiveSimulations(Math.max(0, user.getActiveSimulations() - 1));
        userRepository.save(user);
    }

    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .riskProfile(user.getRiskProfile().name().toLowerCase())
                .experience(user.getExperience().name().toLowerCase())
                .totalPortfolio(user.getTotalPortfolio())
                .activeSimulations(user.getActiveSimulations())
                .createdAt(user.getCreatedAt())
                .build();
    }
}