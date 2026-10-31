package com.klef.sdp.orderservice;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.klef.sdp.orderservice.dto.CreateOrderRequest;
import com.klef.sdp.orderservice.dto.OrderResponse;
import com.klef.sdp.orderservice.entity.OrderStatus;
import com.klef.sdp.orderservice.exception.DownstreamServiceException;
import com.klef.sdp.orderservice.exception.OrderRequestException;
import com.klef.sdp.orderservice.service.OrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:order-test",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"feasto.internal-service-token=test-internal-service-token-0123456789"
})
@AutoConfigureMockMvc
class OrderServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@Test
	void contextLoads() {
	}

	@Test
	void protectedOrdersRejectMissingAndInvalidTokens() throws Exception {
		mockMvc.perform(get("/orders/"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/orders/").header("Authorization", "Bearer invalid.token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void customerCannotViewAnotherCustomersOrders() throws Exception {
		mockMvc.perform(get("/orders/user/202")
						.header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void orderOwnerCannotBeSuppliedByRequest() throws Exception {
		when(orderService.createOrder(any(CreateOrderRequest.class)))
				.thenAnswer(invocation -> OrderResponse.builder()
						.userId(((CreateOrderRequest) invocation.getArgument(0)).getUserId())
						.build());

		mockMvc.perform(post("/orders")
						.header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":202,"restaurantId":1,
								"items":[{"itemId":4,"quantity":1,"price":5.00}],
								"paymentMethod":"CARD"}
								"""))
				.andExpect(status().isCreated());

		org.mockito.ArgumentCaptor<CreateOrderRequest> request =
				org.mockito.ArgumentCaptor.forClass(CreateOrderRequest.class);
		verify(orderService).createOrder(request.capture());
		org.junit.jupiter.api.Assertions.assertEquals(101L, request.getValue().getUserId());
	}

	@Test
	void orderRequestRejectsNonPositiveMenuItemIds() throws Exception {
		mockMvc.perform(post("/orders")
						.header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":101,"restaurantId":1,
								"items":[{"itemId":0,"quantity":1,"price":5.00}],
								"paymentMethod":"CARD"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void invalidMenuItemFailureReturnsBadRequest() throws Exception {
		when(orderService.createOrder(any(CreateOrderRequest.class)))
				.thenThrow(new OrderRequestException("Menu item does not belong to the selected restaurant"));

		mockMvc.perform(post("/orders")
						.header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":101,"restaurantId":1,
								"items":[{"itemId":4,"quantity":1,"price":5.00}],
								"paymentMethod":"CARD"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Menu item does not belong to the selected restaurant")));
	}

	@Test
	void downstreamFailureReturnsBadGateway() throws Exception {
		when(orderService.createOrder(any(CreateOrderRequest.class)))
				.thenThrow(new DownstreamServiceException(
						"Payment service could not process the order payment"));

		mockMvc.perform(post("/orders")
						.header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":101,"restaurantId":1,
								"items":[{"itemId":4,"quantity":1,"price":5.00}],
								"paymentMethod":"CARD"}
								"""))
				.andExpect(status().isBadGateway())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Payment service could not process the order payment")));
	}

	@Test
	void customerCannotCancelAnotherCustomersOrder() throws Exception {
		when(orderService.getOrderById(77L))
				.thenReturn(OrderResponse.builder().orderId(77L).userId(202L).build());

		mockMvc.perform(patch("/orders/77/cancel")
						.header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER")))
				.andExpect(status().isForbidden());

		verify(orderService, never()).cancelOrder(77L);
	}

	@Test
	void customerCannotUpdateOrderStatusButSuperAdminCan() throws Exception {
		mockMvc.perform(patch("/orders/77/status")
						.param("status", "CONFIRMED")
						.header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER")))
				.andExpect(status().isForbidden());

		when(orderService.updateOrderStatus(77L, OrderStatus.CONFIRMED))
				.thenReturn(OrderResponse.builder().orderId(77L)
						.status(OrderStatus.CONFIRMED).build());
		mockMvc.perform(patch("/orders/77/status")
						.param("status", "CONFIRMED")
						.header("Authorization", "Bearer " + signedToken(303, "ROLE_SUPER_ADMIN")))
				.andExpect(status().isOk());

		verify(orderService).updateOrderStatus(77L, OrderStatus.CONFIRMED);
	}

	private String signedToken(long userId, String role) throws Exception {
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		String header = encoder.encodeToString(
				"{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		long issuedAt = System.currentTimeMillis() / 1000;
		String claims = "{\"sub\":\"order-test\",\"iat\":" + issuedAt
				+ ",\"exp\":" + (issuedAt + 300)
				+ ",\"role\":\"" + role + "\",\"userId\":" + userId + "}";
		String payload = encoder.encodeToString(claims.getBytes(StandardCharsets.UTF_8));
		String signingInput = header + "." + payload;

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(Base64.getDecoder().decode(
				"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="), "HmacSHA256"));
		String signature = encoder.encodeToString(
				mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
		return signingInput + "." + signature;
	}
}
