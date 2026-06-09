package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.config.TornDepositProperties;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.DepositRepository;
import com.xanwar.rps.repository.PendingDepositRepository;
import com.xanwar.rps.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private DepositRepository depositRepository;
    @Mock private PendingDepositRepository pendingDepositRepository;
    @Mock private TornApiClient tornApiClient;
    @Mock private TaskScheduler taskScheduler;
    @Mock private JdbcTemplate jdbcTemplate;

    private TornDepositProperties depositProperties;
    private GameProperties gameProperties;
    private DepositService depositService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        depositProperties = new TornDepositProperties();
        depositProperties.setRequiredMessage("RPS");
        depositProperties.setMaxAgeHours(72);
        depositProperties.setRecipientName("Hannath");
        depositProperties.setRecipientId("3961385");
        depositProperties.setXanaxValue(820000);

        gameProperties = new GameProperties();
        gameProperties.setMoolaPerXanax(4);

        depositService = new DepositService(
                userService, userRepository, depositRepository, pendingDepositRepository,
                tornApiClient, depositProperties, gameProperties,
                taskScheduler, mapper, jdbcTemplate
        );
    }

    @Test
    void initiatesDepositSuccessfully() {
        User user = new User("67890", "TestPlayer");
        when(userService.requireUser("67890")).thenReturn(user);
        when(pendingDepositRepository.existsByTornId("67890")).thenReturn(false);
        
        // Mock taskScheduler to return a non-null future
        ScheduledFuture mockFuture = org.mockito.Mockito.mock(ScheduledFuture.class);
        when(taskScheduler.scheduleWithFixedDelay(org.mockito.ArgumentMatchers.any(Runnable.class), 
                org.mockito.ArgumentMatchers.any(Instant.class), 
                org.mockito.ArgumentMatchers.any(java.time.Duration.class))).thenReturn(mockFuture);

        Map<String, Object> result = depositService.initiateDeposit("67890", 5);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("message")).isEqualTo("Monitoring for Xanax...");
    }

    @Test
    void failsToInitiateWhenAlreadyPending() {
        when(pendingDepositRepository.existsByTornId("67890")).thenReturn(true);
        when(userService.requireUser("67890")).thenReturn(new User("67890", "TestPlayer"));

        Map<String, Object> result = depositService.initiateDeposit("67890", 5);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("A deposit is already pending for your account.");
    }

    @Test
    void checkStatusReturnsPending() {
        when(pendingDepositRepository.existsByTornId("67890")).thenReturn(true);
        Map<String, Object> result = depositService.checkStatus("67890");
        assertThat(result.get("status")).isEqualTo("pending");
    }

    @Test
    void claimDepositSucceedsWithoutPendingRecord() {
        User user = new User("67890", "TestPlayer");
        user.setSiteBalance(100L);
        when(userRepository.findByTornId("67890")).thenReturn(Optional.of(user));
        when(depositRepository.existsByEventId("auto-12345")).thenReturn(false);
        
        // Mock TornDepositProperties
        // (depositProperties was set up in setUp)

        depositService.claimDeposit("67890", "12345", 1, Instant.now());

        assertThat(user.getSiteBalance()).isEqualTo(100L + depositProperties.getXanaxValue());
    }

    @Test
    void verifyDepositReturnsSuccess() {
        User user = new User("67890", "TestPlayer");
        user.setSiteBalance(500L);
        when(userService.requireUser("67890")).thenReturn(user);

        // Mock tornApiClient to return empty events so pollAndVerify doesn't fail
        JsonNode emptyNode = mapper.createObjectNode();
        ((com.fasterxml.jackson.databind.node.ObjectNode) emptyNode).putObject("events");
        when(tornApiClient.fetchHouseActivity()).thenReturn(emptyNode);

        Map<String, Object> result = depositService.verifyDeposit("67890");
        
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("site_balance")).isEqualTo(500L);
        assertThat(result.get("message")).isEqualTo("Verification triggered. Check your balance.");
    }
}
