 package com.feasto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klef.sdp.RestaurantServiceApplication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.springframework.http.MediaType;

@SpringBootTest(classes = RestaurantServiceApplication.class, properties = {
		"spring.datasource.url=jdbc:h2:mem:restaurant-test",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
class RestaurantServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void publicRestaurantBrowsingDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/restaurant/all"))
				.andExpect(status().isOk());
	}

	@Test
	void customerCannotCreateRestaurant() throws Exception {
		mockMvc.perform(post("/restaurant/create")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Cafe","location":"City","region":"North",
								"address":"123 Test Street","phone":"9876543210","rating":4.5}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void superAdminCanCreateRestaurant() throws Exception {
		mockMvc.perform(post("/restaurant/create")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Cafe","location":"City","region":"North",
								"address":"123 Test Street","phone":"9876543210","rating":4.5}
								"""))
				.andExpect(status().isCreated());
	}

	@Test
	void onlySuperAdminCanChangeRestaurantActivation() throws Exception {
		mockMvc.perform(patch("/restaurant/1/activation")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void activationValidatesBodyAndReturnsNotFound() throws Exception {
		var administrator = jwt().authorities(
				new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));

		mockMvc.perform(patch("/restaurant/999999/activation")
						.with(administrator)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/restaurant/999999/activation")
						.with(administrator)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":null}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/restaurant/999999/activation")
						.with(administrator)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void activationAndDeleteDeactivateWithoutDeletingRestaurantOrMenu() throws Exception {
		long restaurantId = createRestaurant();
		mockMvc.perform(post("/restaurant/{restaurantId}/menu", restaurantId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Dish","description":"Test item",
								"category":"Main","price":12.5,"available":true}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(patch("/restaurant/{restaurantId}/activation", restaurantId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(patch("/restaurant/{restaurantId}/activation", restaurantId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));

		mockMvc.perform(delete("/restaurant/{restaurantId}", restaurantId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/restaurant/{restaurantId}", restaurantId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(get("/restaurant/{restaurantId}/menu", restaurantId))
				.andExpect(status().isOk())
				.andExpect(content().json("[{\"name\":\"Test Dish\"}]"));
	}

	private long createRestaurant() throws Exception {
		MvcResult result = mockMvc.perform(post("/restaurant/create")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Activation Test Cafe","location":"Test City","region":"North",
								"address":"123 Test Street","phone":"6888888889","rating":4.5}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode restaurant = objectMapper.readTree(
				result.getResponse().getContentAsString());
		return restaurant.get("restaurantId").asLong();
	}

	@Test
	void unauthenticatedMenuValidationIsRejected() throws Exception {
		mockMvc.perform(get("/menu-item/1/validate"))
				.andExpect(status().isUnauthorized());
	}
}
