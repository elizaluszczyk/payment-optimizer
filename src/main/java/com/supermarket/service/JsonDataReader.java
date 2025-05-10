package com.supermarket.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.model.Order;
import com.supermarket.model.PaymentMethod;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDataReader {

  private static final Logger logger = LoggerFactory.getLogger(JsonDataReader.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  public List<Order> readOrders(Path filePath) throws IOException {
    logger.info("Reading orders from: {}", filePath);
    try {
      List<Order> orders = objectMapper.readValue(filePath.toFile(), new TypeReference<>() {});
      logger.info("Successfully read {} orders.", orders.size());
      return orders;
    } catch (IOException e) {
      logger.error("Failed to read or parse orders from {}: {}", filePath, e.getMessage());
      throw e;
    }
  }

  public List<PaymentMethod> readPaymentMethods(Path filePath) throws IOException {
    logger.info("Reading payment methods from: {}", filePath);
    try {
      List<PaymentMethod> paymentMethods =
          objectMapper.readValue(filePath.toFile(), new TypeReference<>() {});
      logger.info("Successfully read {} payment methods.", paymentMethods.size());
      return paymentMethods;
    } catch (IOException e) {
      logger.error("Failed to read or parse payment methods from {}: {}", filePath, e.getMessage());
      throw e;
    }
  }
}
