package com.supermarket.service;

import static org.junit.jupiter.api.Assertions.*;

import com.supermarket.model.Order;
import com.supermarket.model.PaymentMethod;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonDataReaderTest {

  private JsonDataReader dataReader;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    dataReader = new JsonDataReader();
  }

  @Test
  void testReadOrders_ValidFile() throws IOException {
    String jsonContent =
        """
        [
            {"id": "ORDER1", "value": "100.00", "promotions": ["PROMO_A"]},
            {"id": "ORDER2", "value": "200.50"}
        ]
        """;
    Path ordersFile = tempDir.resolve("orders.json");
    Files.writeString(ordersFile, jsonContent);

    List<Order> orders = dataReader.readOrders(ordersFile);

    assertNotNull(orders);
    assertEquals(2, orders.size());

    Order order1 = orders.get(0);
    assertEquals("ORDER1", order1.id());
    assertEquals(new BigDecimal("100.00"), order1.value());
    assertEquals(List.of("PROMO_A"), order1.promotions());

    Order order2 = orders.get(1);
    assertEquals("ORDER2", order2.id());
    assertEquals(new BigDecimal("200.50"), order2.value());
    assertTrue(order2.promotions().isEmpty(), "Promotions should be empty if not specified");
  }

  @Test
  void testReadOrders_EmptyList() throws IOException {
    String jsonContent = "[]";
    Path ordersFile = tempDir.resolve("empty_orders.json");
    Files.writeString(ordersFile, jsonContent);

    List<Order> orders = dataReader.readOrders(ordersFile);
    assertNotNull(orders);
    assertTrue(orders.isEmpty());
  }

  @Test
  void testReadOrders_FileNotFound_ThrowsIOException() {
    Path nonExistentFile = tempDir.resolve("non_existent.json");
    assertThrows(
        IOException.class,
        () -> {
          dataReader.readOrders(nonExistentFile);
        });
  }

  @Test
  void testReadPaymentMethods_EmptyList() throws IOException {
    String jsonContent = "[]";
    Path pmFile = tempDir.resolve("empty_pm.json");
    Files.writeString(pmFile, jsonContent);

    List<PaymentMethod> paymentMethods = dataReader.readPaymentMethods(pmFile);
    assertNotNull(paymentMethods);
    assertTrue(paymentMethods.isEmpty());
  }

  @Test
  void testReadPaymentMethods_FileNotFound_ThrowsIOException() {
    Path nonExistentFile = tempDir.resolve("non_existent_pm.json");
    assertThrows(
        IOException.class,
        () -> {
          dataReader.readPaymentMethods(nonExistentFile);
        });
  }

  @Test
  void testReadPaymentMethods_InvalidDiscountFormat_ThrowsJsonProcessingExceptionViaIOException()
      throws IOException {
    String jsonContent =
        """
        [
            {"id": "CARD_Z", "discount": "NOT_A_NUMBER", "limit": "100.00"}
        ]
        """;
    Path pmFile = tempDir.resolve("invalid_discount_pm.json");
    Files.writeString(pmFile, jsonContent);

    assertThrows(
        IOException.class,
        () -> {
          dataReader.readPaymentMethods(pmFile);
        });
  }
}
