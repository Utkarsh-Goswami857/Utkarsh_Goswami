package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;

public class ProductManagementFrame extends JFrame {

    private final JTextField idField = new JTextField(10);
    private final JTextField nameField = new JTextField(15);
    private final JTextField priceField = new JTextField(10);
    private final JTextField stockField = new JTextField(10);
    private final JTextField gstField = new JTextField(10);
    private final JTextField searchField = new JTextField(15);

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Product ID", "Product Name", "Price", "Stock", "GST %"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);

    public ProductManagementFrame() {
        setTitle("Supermarket - Product Management");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(createFormPanel(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                populateForm();
            }
        });

        loadProducts("");
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 5, 10, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Product Details"));

        panel.add(new JLabel("Product ID"));
        panel.add(new JLabel("Product Name"));
        panel.add(new JLabel("Price (₹)"));
        panel.add(new JLabel("Stock Quantity"));
        panel.add(new JLabel("GST (%)"));

        panel.add(idField);
        panel.add(nameField);
        panel.add(priceField);
        panel.add(stockField);
        panel.add(gstField);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JButton addButton = new JButton("Add Product");
        JButton updateButton = new JButton("Update Product");
        JButton deleteButton = new JButton("Delete Product");
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Form");

        panel.add(addButton);
        panel.add(updateButton);
        panel.add(deleteButton);
        panel.add(new JLabel("Search ID/Name:"));
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(refreshButton);
        panel.add(clearButton);

        addButton.addActionListener(e -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        searchButton.addActionListener(e -> loadProducts(searchField.getText().trim()));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadProducts("");
        });
        clearButton.addActionListener(e -> clearForm());

        return panel;
    }

    private void loadProducts(String search) {
        model.setRowCount(0);

        String sql = """
                SELECT product_id, product_name, price, stock_quantity, gst_rate
                FROM products
                WHERE CAST(product_id AS CHAR) LIKE ?
                   OR product_name LIKE ?
                ORDER BY product_id
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + search + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getBigDecimal("price"),
                            rs.getInt("stock_quantity"),
                            rs.getBigDecimal("gst_rate")
                    });
                }
            }
        } catch (SQLException e) {
            showError("Unable to load products", e);
        }
    }

    private boolean validateForm() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            int stock = Integer.parseInt(stockField.getText().trim());
            BigDecimal gst = new BigDecimal(gstField.getText().trim());

            if (id <= 0) {
                throw new IllegalArgumentException("Product ID must be positive.");
            }
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Product name cannot be empty.");
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price must be greater than zero.");
            }
            if (stock < 0) {
                throw new IllegalArgumentException("Stock cannot be negative.");
            }
            if (gst.compareTo(BigDecimal.ZERO) < 0
                    || gst.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("GST must be between 0 and 100.");
            }
            return true;

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter valid numbers for ID, price, stock, and GST.",
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );
        }
        return false;
    }

    private void addProduct() {
        if (!validateForm()) {
            return;
        }

        String sql = """
                INSERT INTO products
                (product_id, product_name, price, stock_quantity, gst_rate)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(idField.getText().trim()));
            ps.setString(2, nameField.getText().trim());
            ps.setBigDecimal(3, new BigDecimal(priceField.getText().trim()));
            ps.setInt(4, Integer.parseInt(stockField.getText().trim()));
            ps.setBigDecimal(5, new BigDecimal(gstField.getText().trim()));

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Product added successfully!");
            clearForm();
            loadProducts(searchField.getText().trim());

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "This product ID already exists.",
                    "Duplicate Product",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException e) {
            showError("Unable to add product", e);
        }
    }

    private void updateProduct() {
        if (!validateForm()) {
            return;
        }

        String sql = """
                UPDATE products
                SET product_name = ?, price = ?, stock_quantity = ?, gst_rate = ?
                WHERE product_id = ?
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nameField.getText().trim());
            ps.setBigDecimal(2, new BigDecimal(priceField.getText().trim()));
            ps.setInt(3, Integer.parseInt(stockField.getText().trim()));
            ps.setBigDecimal(4, new BigDecimal(gstField.getText().trim()));
            ps.setInt(5, Integer.parseInt(idField.getText().trim()));

            int rows = ps.executeUpdate();

            if (rows == 0) {
                JOptionPane.showMessageDialog(this, "Product not found.");
            } else {
                JOptionPane.showMessageDialog(this, "Product updated successfully!");
                loadProducts(searchField.getText().trim());
            }
        } catch (SQLException e) {
            showError("Unable to update product", e);
        }
    }

    private void deleteProduct() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product from the table first.");
            return;
        }

        int productId = ((Number) model.getValueAt(row, 0)).intValue();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete product ID " + productId + "?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM products WHERE product_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Product deleted successfully!");
                clearForm();
                loadProducts(searchField.getText().trim());
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "This product is linked to previous sales and cannot be deleted.",
                    "Cannot Delete Product",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException e) {
            showError("Unable to delete product", e);
        }
    }

    private void populateForm() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }

        idField.setText(model.getValueAt(row, 0).toString());
        nameField.setText(model.getValueAt(row, 1).toString());
        priceField.setText(model.getValueAt(row, 2).toString());
        stockField.setText(model.getValueAt(row, 3).toString());
        gstField.setText(model.getValueAt(row, 4).toString());
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        priceField.setText("");
        stockField.setText("");
        gstField.setText("");
        table.clearSelection();
        idField.requestFocusInWindow();
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
