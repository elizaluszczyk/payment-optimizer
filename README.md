# Payment Optimizer

Optimizes payments for supermarket orders to maximize discounts using cards and loyalty points.
Prerequisites
- JDK 21+
- Maven 3.6.0+

## Build

```shell
mvn clean package
```
*(JAR in target/payment-optimizer-1.0-SNAPSHOT.jar)*

## Run

```shell
java -jar target/payment-optimizer-1.0-SNAPSHOT.jar <orders.json_path> <paymentmethods.json_path>
```
*(You can find example files in data)*

### Output Example:

```
PUNKTY 100.00
BosBankrut 190.00
mZysk 165.00
```

## Code Quality
- Check style: `mvn verify`
- Fix style: `mvn spotless:apply`
