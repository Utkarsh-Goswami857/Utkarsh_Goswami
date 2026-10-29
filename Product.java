package src;

import java.math.BigDecimal;

public class Product {

    private int productId;
    private String productName;
    private BigDecimal price;
    private int stockQuantity;
    private BigDecimal gstRate;

    public Product(
            int productId,
            String productName,
            BigDecimal price,
            int stockQuantity,
            BigDecimal gstRate
    ) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.gstRate = gstRate;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    @Override
    public String toString() {
        return productId + " - " + productName
                + " | ₹" + price
                + " | Stock: " + stockQuantity;
    }
}
