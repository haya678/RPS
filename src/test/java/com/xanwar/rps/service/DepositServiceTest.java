package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.config.TornDepositProperties;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.DepositRepository;
import com.xanwar.rps.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private DepositRepository depositRepository;
    @Mock private TornApiClient tornApiClient;

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

        gameProperties = new GameProperties();
        gameProperties.setMoolaPerXanax(4);

        depositService = new DepositService(
                userService, userRepository, depositRepository,
                tornApiClient, depositProperties, gameProperties
        );
    }

    @Test
    void creditsWalletFromLogWithScalarSenderAndItemsArray() throws Exception {
        User user = new User("67890", "TestPlayer");
        user.setSiteBalance(0L);
        when(userService.requireUser("67890")).thenReturn(user);
        when(depositRepository.existsByEventId(anyString())).thenReturn(false);

        // Torn API log format: scalar sender integer, items as array
        String json = """
                {
                  "events": {},
                  "log": {
                    "999": {
                      "timestamp": %d,
                      "log": 6380,
                      "title": "Received items",
                      "data": {
                        "items": [{"name": "Xanax", "qty": 2}],
                        "sender": 67890,
                        "message": "RPS"
                      }
                    }
                  }
                }
                """.formatted(System.currentTimeMillis() / 1000);
        JsonNode apiResponse = mapper.readTree(json);
        when(tornApiClient.fetchHouseActivity()).thenReturn(apiResponse);

        Map<String, Object> result = depositService.verifyDeposit("67890");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("verified")).isEqualTo(true);
        assertThat((long) result.get("moola_credited")).isEqualTo(8L);
        assertThat(user.getSiteBalance()).isEqualTo(8L);
    }

    @Test
    void creditsWalletFromEventsWithHtmlLink() throws Exception {
        User user = new User("12345", "SomePlayer");
        user.setSiteBalance(100L);
        when(userService.requireUser("12345")).thenReturn(user);
        when(depositRepository.existsByEventId(anyString())).thenReturn(false);

        String json = """
                {
                  "events": {
                    "555": {
                      "timestamp": %d,
                      "event": "<a href='/profiles.php?XID=12345'>SomePlayer</a> sent you 3x Xanax with the message: RPS."
                    }
                  },
                  "log": {}
                }
                """.formatted(System.currentTimeMillis() / 1000);
        JsonNode apiResponse = mapper.readTree(json);
        when(tornApiClient.fetchHouseActivity()).thenReturn(apiResponse);

        Map<String, Object> result = depositService.verifyDeposit("12345");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("verified")).isEqualTo(true);
        assertThat((long) result.get("moola_credited")).isEqualTo(12L);
        assertThat(user.getSiteBalance()).isEqualTo(112L);
    }

    @Test
    void skipsDepositWhenSenderDoesNotMatchLoggedInUser() throws Exception {
        User user = new User("11111", "OtherPlayer");
        user.setSiteBalance(50L);
        when(userService.requireUser("11111")).thenReturn(user);

        // Sender is 99999, but logged-in user is 11111
        String json = """
                {
                  "events": {
                    "777": {
                      "timestamp": %d,
                      "event": "<a href='/profiles.php?XID=99999'>Attacker</a> sent you 1x Xanax with the message: RPS."
                    }
                  },
                  "log": {}
                }
                """.formatted(System.currentTimeMillis() / 1000);
        JsonNode apiResponse = mapper.readTree(json);
        when(tornApiClient.fetchHouseActivity()).thenReturn(apiResponse);

        Map<String, Object> result = depositService.verifyDeposit("11111");

        assertThat(result.get("verified")).isEqualTo(false);
        assertThat(user.getSiteBalance()).isEqualTo(50L);
    }

    @Test
    void skipsDuplicateDeposit() throws Exception {
        User user = new User("67890", "TestPlayer");
        user.setSiteBalance(0L);
        when(userService.requireUser("67890")).thenReturn(user);
        when(depositRepository.existsByEventId("events-555")).thenReturn(true);

        String json = """
                {
                  "events": {
                    "555": {
                      "timestamp": %d,
                      "event": "<a href='/profiles.php?XID=67890'>TestPlayer</a> sent you 1x Xanax with the message: RPS."
                    }
                  },
                  "log": {}
                }
                """.formatted(System.currentTimeMillis() / 1000);
        JsonNode apiResponse = mapper.readTree(json);
        when(tornApiClient.fetchHouseActivity()).thenReturn(apiResponse);

        Map<String, Object> result = depositService.verifyDeposit("67890");

        assertThat(result.get("verified")).isEqualTo(false);
        assertThat(user.getSiteBalance()).isEqualTo(0L);
    }
}
