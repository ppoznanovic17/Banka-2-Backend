package rs.raf.banka2_bek.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.banka2_bek.order.dto.CreateOrderDto;
import rs.raf.banka2_bek.order.dto.OrderDto;
import rs.raf.banka2_bek.order.service.OrderService;

/**
 * Controller za kreiranje i upravljanje orderima.
 *
 * TODO: Dodati u GlobalSecurityConfig:
 *   .requestMatchers(HttpMethod.POST, "/orders").hasAnyRole("ADMIN", "CLIENT", "EMPLOYEE")
 *   .requestMatchers(HttpMethod.GET, "/orders/my").hasAnyRole("ADMIN", "CLIENT", "EMPLOYEE")
 *   .requestMatchers("/orders/{id}/approve", "/orders/{id}/decline").hasAnyRole("ADMIN")
 */
@Tag(name = "Orders", description = "Exchange orders API (create, list, approve)")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /orders - Kreiranje novog ordera (BUY ili SELL)
     * Pristup: aktuari i klijenti sa permisijom za trgovinu.
     */
    @Operation(summary = "Create order", description = "Creates a BUY or SELL order. Validates funds, listing, and actuary limits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order created", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation or business rule violation"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderDto dto) {
        return ResponseEntity.ok(orderService.createOrder(dto));
    }

    /**
     * GET /orders - Pregled svih ordera (supervizor portal)
     * Filtriranje po statusu: ALL, PENDING, APPROVED, DECLINED, DONE
     */
    @Operation(summary = "List all orders (supervisor)", description = "Paginated list of all orders, optional status filter. Sort: createdAt DESC, then id DESC. Use ALL or omit status for every order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of OrderDto (content, totalElements, totalPages, …)"),
            @ApiResponse(responseCode = "400", description = "Invalid status filter")
    })
    @GetMapping
    public ResponseEntity<Page<OrderDto>> getAllOrders(
            @Parameter(description = "ALL, PENDING, APPROVED, DECLINED, DONE; omit or ALL for no filter")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(status, page, size));
    }

    /**
     * GET /orders/my - Moji orderi (za korisnika)
     */
    @Operation(summary = "My orders", description = "Paginated orders for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of OrderDto")
    })
    @GetMapping("/my")
    public ResponseEntity<Page<OrderDto>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getMyOrders(page, size));
    }

    /**
     * GET /orders/{id} - Detalji jednog ordera
     */
    @Operation(summary = "Get order by id", description = "Returns a single order by id (access rules apply).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * PATCH /orders/{id}/approve - Supervizor odobrava order
     */
    @Operation(summary = "Approve order", description = "Supervisor approves a PENDING order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order approved", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state")
    })
    @PatchMapping("/{id}/approve")
    public ResponseEntity<OrderDto> approveOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.approveOrder(id));
    }

    /**
     * PATCH /orders/{id}/decline - Supervizor odbija order
     */
    @Operation(summary = "Decline order", description = "Supervisor declines a PENDING order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order declined", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state")
    })
    @PatchMapping("/{id}/decline")
    public ResponseEntity<OrderDto> declineOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.declineOrder(id));
    }
}
