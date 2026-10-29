package src;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class BillingService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    public static class BillResult {

        private final int billId;
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal gstAmount;
        private final BigDecimal finalAmount;

        public BillResult(
                int billId,
                BigDecimal subtotal,
                BigDecimal discount,
                BigDecimal gstAmount,
                BigDecimal finalAmount
        ) {
            this.billId = billId;
            this.subtotal = subtotal;
            this.discount = discount;
            this.gstAmount = gstAmount;
            this.finalAmount = finalAmount;
        }

        public int getBillId() {
            return billId;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public BigDecimal getGstAmount() {
            return gstAmount;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }
    }

    private BigDecimal calculateSubtotal(List<Product> cart) {

        BigDecimal subtotal = ZERO;

        for (Product product : cart) {

            // The cart contains one entry per unit.
            subtotal = subtotal.add(product.getPrice());
        }

        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal) {

        BigDecimal discountRate;

        if (subtotal.compareTo(new BigDecimal("5000")) >= 0) {
            discountRate = new BigDecimal("10");
        } else if (subtotal.compareTo(new BigDecimal("2000")) >= 0) {
            discountRate = new BigDecimal("5");
        } else {
            discountRate = ZERO;
        }

        return subtotal.multiply(discountRate)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public BillResult completeSale(
            String customerName,
            String phoneNumber,
            String paymentMethod,
            List<Product> cart
    ) throws SQLException {

        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer name cannot be empty."
            );
        }

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException(
                    "The shopping cart is empty."
            );
        }

        BigDecimal subtotal = calculateSubtotal(cart);

        BigDecimal discount = calculateDiscount(subtotal);

        BigDecimal taxableAmount = subtotal.subtract(discount);

        // The discount is distributed proportionally across
        // the items so that GST is calculated on discounted prices.
        BigDecimal gstAmount = ZERO;

        for (Product product : cart) {

            BigDecimal itemAmount = product.getPrice();

            BigDecimal itemDiscount = subtotal.signum() == 0
                    ? ZERO
                    : discount.multiply(itemAmount)
                            .divide(
                                    subtotal,
                                    10,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal taxableItem = itemAmount.subtract(itemDiscount);

            BigDecimal itemGST = taxableItem
                    .multiply(product.getGstRate())
                    .divide(
                            ONE_HUNDRED,
                            2,
                            RoundingMode.HALF_UP
                    );

            gstAmount = gstAmount.add(itemGST);
        }

        gstAmount = gstAmount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalAmount = taxableAmount
                .add(gstAmount)
                .setScale(2, RoundingMode.HALF_UP);

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // Verify stock and lock the relevant product rows.
                // Aggregate quantities because a product may appear
                // multiple times in the cart.

                java.util.Map<Integer, Integer> quantities =
                        new java.util.HashMap<>();

                for (Product product : cart) {
                    quantities.merge(
                            product.getProductId(),
                            1,
                            Integer::sum
                    );
                }

                String stockSQL = """
                        SELECT stock_quantity
                        FROM products
                        WHERE product_id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement stockStatement =
                             connection.prepareStatement(stockSQL)) {

                    for (var entry : quantities.entrySet()) {

                        stockStatement.setInt(1, entry.getKey());

                        try (ResultSet rs =
                                     stockStatement.executeQuery()) {

                            if (!rs.next()) {
                                throw new SQLException(
                                        "Product not found: "
                                                + entry.getKey()
                                );
                            }

                            int available = rs.getInt("stock_quantity");

                            if (available < entry.getValue()) {
                                throw new SQLException(
                                        "Insufficient stock for product ID "
                                                + entry.getKey()
                                );
                            }
                        }
                    }
                }

                // Save the bill.

                String salesSQL = """
                        INSERT INTO sales
                        (customer_name, phone_number, subtotal,
                         discount, gst_amount, final_amount,
                         payment_method)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """;

                int billId;

                try (PreparedStatement salesStatement =
                             connection.prepareStatement(
                                     salesSQL,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    salesStatement.setString(1, customerName);
                    salesStatement.setString(2, phoneNumber);
                    salesStatement.setBigDecimal(3, subtotal);
                    salesStatement.setBigDecimal(4, discount);
                    salesStatement.setBigDecimal(5, gstAmount);
                    salesStatement.setBigDecimal(6, finalAmount);
                    salesStatement.setString(7, paymentMethod);

                    salesStatement.executeUpdate();

                    try (ResultSet keys =
                                 salesStatement.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not generate bill number."
                            );
                        }

                        billId = keys.getInt(1);
                    }
                }

                // Save each purchased item and decrease inventory.

                String itemSQL = """
                        INSERT INTO sale_items
                        (bill_id, product_id, product_name,
                         quantity, unit_price, total_price)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;

                String updateStockSQL = """
                        UPDATE products
                        SET stock_quantity = stock_quantity - ?
                        WHERE product_id = ?
                          AND stock_quantity >= ?
                        """;

                try (
                        PreparedStatement itemStatement =
                                connection.prepareStatement(itemSQL);

                        PreparedStatement stockUpdate =
                                connection.prepareStatement(updateStockSQL)
                ) {

                    for (var entry : quantities.entrySet()) {

                        int productId = entry.getKey();
                        int quantity = entry.getValue();

                        Product product = null;

                        for (Product p : cart) {
                            if (p.getProductId() == productId) {
                                product = p;
                                break;
                            }
                        }

                        if (product == null) {
                            throw new SQLException(
                                    "Product missing from cart."
                            );
                        }

                        BigDecimal itemTotal = product.getPrice()
                                .multiply(BigDecimal.valueOf(quantity));

                        itemStatement.setInt(1, billId);
                        itemStatement.setInt(2, productId);
                        itemStatement.setString(
                                3,
                                product.getProductName()
                        );
                        itemStatement.setInt(4, quantity);
                        itemStatement.setBigDecimal(
                                5,
                                product.getPrice()
                        );
                        itemStatement.setBigDecimal(6, itemTotal);

                        itemStatement.executeUpdate();

                        stockUpdate.setInt(1, quantity);
                        stockUpdate.setInt(2, productId);
                        stockUpdate.setInt(3, quantity);

                        int updated = stockUpdate.executeUpdate();

                        if (updated != 1) {
                            throw new SQLException(
                                    "Inventory update failed for "
                                            + product.getProductName()
                            );
                        }
                    }
                }

                connection.commit();

                return new BillResult(
                        billId,
                        subtotal,
                        discount,
                        gstAmount,
                        finalAmount
                );

            } catch (SQLException | RuntimeException e) {

                connection.rollback();

                throw e;

            } finally {

                connection.setAutoCommit(true);
            }
        }
    }
}
