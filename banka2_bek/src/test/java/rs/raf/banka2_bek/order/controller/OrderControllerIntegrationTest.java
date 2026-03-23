package rs.raf.banka2_bek.order.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import rs.raf.banka2_bek.auth.model.User;
import rs.raf.banka2_bek.auth.repository.PasswordResetTokenRepository;
import rs.raf.banka2_bek.auth.repository.UserRepository;
import rs.raf.banka2_bek.auth.service.JwtService;
import rs.raf.banka2_bek.order.model.Order;
import rs.raf.banka2_bek.order.model.OrderDirection;
import rs.raf.banka2_bek.order.model.OrderStatus;
import rs.raf.banka2_bek.order.model.OrderType;
import rs.raf.banka2_bek.order.repository.OrderRepository;
import rs.raf.banka2_bek.stock.model.Listing;
import rs.raf.banka2_bek.stock.model.ListingType;
import rs.raf.banka2_bek.stock.repository.ListingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return rt;
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        listingRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String adminToken() {
        User user = new User();
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setEmail("admin-orders@test.com");
        user.setPassword("x");
        user.setActive(true);
        user.setRole("ADMIN");
        userRepository.save(user);
        return jwtService.generateAccessToken(user);
    }

    private String clientToken() {
        User user = new User();
        user.setFirstName("Client");
        user.setLastName("User");
        user.setEmail("client-orders@test.com");
        user.setPassword("x");
        user.setActive(true);
        user.setRole("CLIENT");
        userRepository.save(user);
        return jwtService.generateAccessToken(user);
    }

    private Listing saveListing() {
        Listing listing = new Listing();
        listing.setTicker("INT" + System.nanoTime());
        listing.setName("Integration Test Listing");
        listing.setListingType(ListingType.STOCK);
        listing.setPrice(new BigDecimal("100"));
        listing.setAsk(new BigDecimal("101"));
        listing.setBid(new BigDecimal("99"));
        return listingRepository.save(listing);
    }

    private Order buildOrder(Listing listing, OrderStatus status, LocalDateTime createdAt) {
        Order o = new Order();
        o.setUserId(1L);
        o.setUserRole("CLIENT");
        o.setListing(listing);
        o.setOrderType(OrderType.MARKET);
        o.setQuantity(2);
        o.setContractSize(1);
        o.setPricePerUnit(new BigDecimal("100"));
        o.setDirection(OrderDirection.BUY);
        o.setStatus(status);
        o.setDone(false);
        o.setAfterHours(false);
        o.setAllOrNone(false);
        o.setMargin(false);
        o.setRemainingPortions(2);
        o.setCreatedAt(createdAt);
        o.setLastModification(createdAt);
        return o;
    }

    @Test
    void getOrdersAsAdminReturnsPagedSortedAndFiltered() throws Exception {
        String token = adminToken();
        Listing listing = saveListing();
        LocalDateTime older = LocalDateTime.now().minusDays(1);
        orderRepository.save(buildOrder(listing, OrderStatus.PENDING, older));
        orderRepository.save(buildOrder(listing, OrderStatus.APPROVED, LocalDateTime.now()));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> all = restTemplate.exchange(
                url("/orders?page=0&size=10&status=ALL"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(all.getBody());
        assertThat(root.get("content")).hasSize(2);
        assertThat(root.get("content").get(0).get("status").asText()).isEqualTo("APPROVED");
        assertThat(root.get("content").get(1).get("status").asText()).isEqualTo("PENDING");

        ResponseEntity<String> pendingOnly = restTemplate.exchange(
                url("/orders?page=0&size=10&status=PENDING"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        JsonNode pendingRoot = objectMapper.readTree(pendingOnly.getBody());
        assertThat(pendingRoot.get("content")).hasSize(1);
        assertThat(pendingRoot.get("content").get(0).get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void getOrdersInvalidStatusReturns400() {
        String token = adminToken();
        saveListing();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> res = restTemplate.exchange(
                url("/orders?page=0&size=10&status=INVALID"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).contains("Invalid order status filter");
    }

    @Test
    void getOrdersAsNonAdminReturns403() {
        String token = clientToken();
        Listing listing = saveListing();
        orderRepository.save(buildOrder(listing, OrderStatus.APPROVED, LocalDateTime.now()));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> res = restTemplate.exchange(
                url("/orders?page=0&size=10"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
