package src;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> getAllProducts() throws SQLException {

        List<Product> products=new ArrayList<>();

        String sql = """
                SELECT product_id, product_name, price,
                       stock_quantity, gst_rate
                FROM products
                ORDER BY product_name
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {

                Product product = new Product(
                        resultSet.getInt("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getInt("stock_quantity"),
                        resultSet.getBigDecimal("gst_rate")
                );

                products.add(product);
            }
        }

        return products;
    }

    public Product getProductById(int productId)
            throws SQLException {

        String sql = """
                SELECT product_id, product_name, price,
                       stock_quantity, gst_rate
                FROM products
                WHERE product_id = ?
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, productId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new Product(
                            resultSet.getInt("product_id"),
                            resultSet.getString("product_name"),
                            resultSet.getBigDecimal("price"),
                            resultSet.getInt("stock_quantity"),
                            resultSet.getBigDecimal("gst_rate")
                    );
                }
            }
        }

        return null;
    }
}
