package rs.raf.banka2_bek.exchange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rs.raf.banka2_bek.exchange.dto.ExchangeRateDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExchangeService {

    private final RestTemplate restTemplate;

    @Value("${exchange.api.key}")
    private String apiKey;

    @Value("${exchange.api.url}")
    private String apiUrl;

    public ExchangeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ExchangeRateDto> getAllRates() {

        String url = apiUrl + "?access_key=" + apiKey +
                "&symbols=RSD,EUR,CHF,USD,GBP,JPY,CAD,AUD";

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null || body.get("rates") == null) {
            return new ArrayList<>();
        }

        Map<String, Object> rates = (Map<String, Object>) body.get("rates");

        Double eurToRsd = getDouble(rates.get("RSD"));

        if (eurToRsd == null || eurToRsd == 0.0) {
            throw new RuntimeException("RSD rate not found.");
        }

        String[] currencies = {"RSD", "EUR", "CHF", "USD", "GBP", "JPY", "CAD", "AUD"};

        List<ExchangeRateDto> result = new ArrayList<>();

        for (String currency : currencies) {

            if ("RSD".equals(currency)) {
                result.add(new ExchangeRateDto("RSD", 1.0));
                continue;
            }

            if ("EUR".equals(currency)) {
                double rate = round(1.0 / eurToRsd, 6);
                result.add(new ExchangeRateDto("EUR", rate));
                continue;
            }

            Double eurToTarget = getDouble(rates.get(currency));

            if (eurToTarget != null) {
                double rsdToTarget = eurToTarget / eurToRsd;
                result.add(new ExchangeRateDto(currency, round(rsdToTarget, 6)));
            }
        }

        return result;
    }

    private Double getDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}