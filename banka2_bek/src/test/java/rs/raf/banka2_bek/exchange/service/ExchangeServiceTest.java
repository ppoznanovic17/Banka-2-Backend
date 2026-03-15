

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.banka2_bek.exchange.ExchangeService;
import rs.raf.banka2_bek.exchange.dto.ExchangeRateDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeService exchangeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exchangeService, "apiKey", "test-key");
        ReflectionTestUtils.setField(exchangeService, "apiUrl", "https://data.fixer.io/api/latest");
    }

    @Test
    void shouldReturnAllExchangeRates() {
        Map<String, Object> rates = new HashMap<>();
        rates.put("RSD", 117.35);
        rates.put("EUR", 1.0);
        rates.put("USD", 1.15);
        rates.put("CHF", 0.91);
        rates.put("GBP", 0.87);
        rates.put("JPY", 183.02);
        rates.put("CAD", 1.58);
        rates.put("AUD", 1.65);

        Map<String, Object> body = new HashMap<>();
        body.put("rates", rates);

        ResponseEntity<Map> responseEntity = ResponseEntity.ok(body);

        String expectedUrl =
                "https://data.fixer.io/api/latest?access_key=test-key&symbols=RSD,EUR,CHF,USD,GBP,JPY,CAD,AUD";

        when(restTemplate.getForEntity(expectedUrl, Map.class)).thenReturn(responseEntity);

        List<ExchangeRateDto> result = exchangeService.getAllRates();

        assertNotNull(result);
        assertEquals(8, result.size());

        ExchangeRateDto rsd = result.stream()
                .filter(r -> r.getCurrency().equals("RSD"))
                .findFirst()
                .orElse(null);

        ExchangeRateDto eur = result.stream()
                .filter(r -> r.getCurrency().equals("EUR"))
                .findFirst()
                .orElse(null);

        ExchangeRateDto usd = result.stream()
                .filter(r -> r.getCurrency().equals("USD"))
                .findFirst()
                .orElse(null);

        assertNotNull(rsd);
        assertEquals(1.0, rsd.getRate());

        assertNotNull(eur);
        assertEquals(0.00852, eur.getRate(), 0.00001);

        assertNotNull(usd);
        assertEquals(0.0098, usd.getRate(), 0.0001);
    }

    @Test
    void shouldReturnEmptyListWhenBodyIsNull() {
        String expectedUrl =
                "https://data.fixer.io/api/latest?access_key=test-key&symbols=RSD,EUR,CHF,USD,GBP,JPY,CAD,AUD";

        when(restTemplate.getForEntity(expectedUrl, Map.class))
                .thenReturn(ResponseEntity.ok(null));

        List<ExchangeRateDto> result = exchangeService.getAllRates();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenRsdRateMissing() {
        Map<String, Object> rates = new HashMap<>();
        rates.put("EUR", 1.0);
        rates.put("USD", 1.15);

        Map<String, Object> body = new HashMap<>();
        body.put("rates", rates);

        String expectedUrl =
                "https://data.fixer.io/api/latest?access_key=test-key&symbols=RSD,EUR,CHF,USD,GBP,JPY,CAD,AUD";

        when(restTemplate.getForEntity(expectedUrl, Map.class))
                .thenReturn(ResponseEntity.ok(body));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> exchangeService.getAllRates());

        assertEquals("RSD rate not found.", ex.getMessage());
    }
    @Test
    void shouldReturnEmptyListWhenRatesMissing() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);

        String expectedUrl =
                "https://data.fixer.io/api/latest?access_key=test-key&symbols=RSD,EUR,CHF,USD,GBP,JPY,CAD,AUD";

        when(restTemplate.getForEntity(expectedUrl, Map.class))
                .thenReturn(ResponseEntity.ok(body));

        List<ExchangeRateDto> result = exchangeService.getAllRates();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}