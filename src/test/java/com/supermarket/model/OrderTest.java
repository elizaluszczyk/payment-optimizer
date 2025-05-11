package com.supermarket.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

  @Test
  void testOrderCreation_ValidData() {
    List<String> promotions = List.of("PROMO1", "PROMO2");
    Order order = new Order("ORDER123", "150.75", promotions);

    assertEquals("ORDER123", order.id());
    assertEquals(new BigDecimal("150.75"), order.value());
    assertEquals(promotions, order.promotions());
  }

  @Test
  void testOrderCreation_NullPromotions() {
    Order order = new Order("ORDER456", "99.00", null);
    assertEquals("ORDER456", order.id());
    assertEquals(new BigDecimal("99.00"), order.value());
    assertNotNull(order.promotions(), "Promotions list should not be null");
    assertTrue(order.promotions().isEmpty(), "Promotions list should be empty when input is null");
  }

  @Test
  void testOrderCreation_EmptyPromotions() {
    Order order = new Order("ORDER789", "10.50", List.of());
    assertEquals("ORDER789", order.id());
    assertEquals(new BigDecimal("10.50"), order.value());
    assertTrue(order.promotions().isEmpty());
  }

  @Test
  void testOrderCreation_ValueWithNoDecimalPlaces() {
    Order order = new Order("ORDER101", "200", null);
    assertEquals(new BigDecimal("200"), order.value());
  }

  @Test
  void testOrderCreation_NullId_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new Order(null, "100.00", null);
        },
        "Order ID cannot be null");
  }

  @Test
  void testOrderCreation_ValueTooManyDecimalPlaces_ThrowsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new Order("ORDER222", "100.123", null);
            });
    assertEquals(
        "Order value cannot have more than two decimal places: 100.123", exception.getMessage());
  }

  @Test
  void testOrderCreation_NegativeValue_ThrowsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new Order("ORDER333", "-50.00", null);
            });
    assertEquals("Order value cannot be negative: -50.00", exception.getMessage());
  }

  @Test
  void testOrderCreation_NonNumericValue_ThrowsNumberFormatException() {
    assertThrows(
        NumberFormatException.class,
        () -> {
          new Order("ORDER444", "NOT_A_NUMBER", null);
        });
  }
}
