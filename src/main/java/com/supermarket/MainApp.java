package com.supermarket;

import com.supermarket.model.Order;
import com.supermarket.model.PaymentMethod;
import com.supermarket.service.JsonDataReader;
import com.supermarket.service.PaymentOptimizer;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp {
  private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

  public static void main(String[] args) {
    if (args.length != 2) {
      logger.error("Usage: java -jar app.jar <orders_file_path> <payment_methods_file_path>");
      System.err.println("Usage: java -jar app.jar <orders_file_path> <payment_methods_file_path>");
      System.exit(1);
    }

    Path ordersFilePath;
    Path paymentMethodsFilePath;

    try {
      ordersFilePath = Paths.get(args[0]);
      paymentMethodsFilePath = Paths.get(args[1]);
    } catch (InvalidPathException e) {
      logger.error("Invalid file path provided: {}", e.getMessage());
      System.err.println("Error: Invalid file path provided - " + e.getMessage());
      System.exit(1);
      return;
    }

    JsonDataReader dataReader = new JsonDataReader();
    PaymentOptimizer optimizer = new PaymentOptimizer();

    try {
      List<Order> orders = dataReader.readOrders(ordersFilePath);
      List<PaymentMethod> paymentMethods = dataReader.readPaymentMethods(paymentMethodsFilePath);

      if (orders.isEmpty()) {
        logger.warn("No orders found in the input file. Exiting.");
        System.out.println("No orders to process.");
        return;
      }
      if (paymentMethods.isEmpty()) {
        logger.warn("No payment methods found. Cannot process payments.");
        System.err.println("Error: No payment methods provided.");
        System.exit(1);
        return;
      }

      logger.info("Starting payment optimization...");
      Map<String, BigDecimal> result = optimizer.optimizePayments(orders, paymentMethods);
      logger.info("Payment optimization complete.");

      logger.info("Outputting results:");
      result.forEach(
          (methodId, spentAmount) -> {
            if (spentAmount.compareTo(BigDecimal.ZERO) > 0) {
              System.out.printf("%s %.2f%n", methodId, spentAmount);
              logger.debug("Output: {} {}", methodId, String.format("%.2f", spentAmount));
            }
          });

    } catch (IOException e) {
      logger.error("Error reading input files: {}", e.getMessage(), e);
      System.err.println("Error reading input files: " + e.getMessage());
      System.exit(1);
    } catch (IllegalArgumentException | IllegalStateException e) {
      logger.error("Error during processing: {}", e.getMessage(), e);
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      logger.error("An unexpected error occurred: {}", e.getMessage(), e);
      System.err.println("An unexpected error occurred: " + e.getMessage());
      System.exit(1);
    }
  }
}
