package com.klef.sdp.orderservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.klef.sdp.orderservice.client.PaymentClient;
import com.klef.sdp.orderservice.client.RestaurantClient;
import com.klef.sdp.orderservice.dto.CreateOrderRequest;
import com.klef.sdp.orderservice.dto.MenuItemValidationResponse;
import com.klef.sdp.orderservice.dto.OrderItemRequest;
import com.klef.sdp.orderservice.dto.OrderResponse;
import com.klef.sdp.orderservice.dto.PaymentRequest;
import com.klef.sdp.orderservice.dto.PaymentResponse;
import com.klef.sdp.orderservice.entity.Order;
import com.klef.sdp.orderservice.entity.OrderStatus;
import com.klef.sdp.orderservice.entity.OrderStatusHistory;
import com.klef.sdp.orderservice.exception.DownstreamServiceException;
import com.klef.sdp.orderservice.exception.OrderRequestException;
import com.klef.sdp.orderservice.repository.OrderItemRepository;
import com.klef.sdp.orderservice.repository.OrderRepository;
import com.klef.sdp.orderservice.repository.OrderStatusHistoryRepository;

import feign.FeignException;
import feign.Request;
import feign.Response;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private RestaurantClient restaurantClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void assignOrderIdWhenSaving() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(31L);
            return order;
        });
    }

    @Test
    void rejectsMenuItemFromDifferentRestaurant() {
        when(restaurantClient.validateMenuItem(9L))
                .thenReturn(new MenuItemValidationResponse(9L, 22L, "Soup", 7.5, true));

        assertThrows(OrderRequestException.class,
                () -> orderService.createOrder(orderRequest()));
    }

    @Test
    void mapsMissingMenuItemToInvalidOrderRequest() {
        when(restaurantClient.validateMenuItem(9L))
                .thenThrow(feignFailure(404));

        OrderRequestException exception = assertThrows(OrderRequestException.class,
                () -> orderService.createOrder(orderRequest()));

        assertEquals("Menu item not found: 9", exception.getMessage());
    }

    @Test
    void mapsPaymentServiceFailureToDownstreamFailure() {
        when(restaurantClient.validateMenuItem(9L))
                .thenReturn(new MenuItemValidationResponse(9L, 11L, "Soup", 7.5, true));
        when(paymentClient.createPayment(anyString(), any(PaymentRequest.class)))
                .thenThrow(feignFailure(503));

        assertThrows(DownstreamServiceException.class,
                () -> orderService.createOrder(orderRequest()));
    }

    @Test
    void successfulPaymentPreservesPlacedStatusAndHistory() {
        when(restaurantClient.validateMenuItem(9L))
                .thenReturn(new MenuItemValidationResponse(9L, 11L, "Soup", 7.5, true));
        when(paymentClient.createPayment(anyString(), any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(2L, 31L, new BigDecimal("7.50"),
                        "CARD", "SUCCESS", LocalDateTime.now(), LocalDateTime.now()));

        OrderResponse response = orderService.createOrder(orderRequest());

        assertEquals(OrderStatus.PLACED, response.getStatus());
        assertEquals(0, new BigDecimal("7.5").compareTo(response.getTotalAmount()));
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        org.mockito.ArgumentCaptor<String> key = forClass(String.class);
        verify(paymentClient).createPayment(key.capture(), any(PaymentRequest.class));
        assertEquals("order-31-payment-attempt-1", key.getValue());
    }

    private CreateOrderRequest orderRequest() {
        return new CreateOrderRequest(101L, 11L,
                List.of(new OrderItemRequest(9L, 1, new BigDecimal("7.50"))), "CARD");
    }

    private FeignException feignFailure(int status) {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://restaurant-service/menu-item/9/validate",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("RestaurantClient#validateMenuItem", response);
    }
}
