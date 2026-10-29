package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;

public class SalesHistoryFrame extends JFrame {

    private final JTextField billSearchField = new JTextField(10);

    private final DefaultTableModel billsModel = new DefaultTableModel(
            new String[]{
                    "Bill ID", "Customer", "Phone", "Date",
                    "Subtotal", "Discount", "GST", "Final Amount", "Payment"
            }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel itemsModel = new DefaultTableModel(
            new String[]{
                    "Product ID", "Product Name", "Quantity",
                    "Unit Price", "Total Price"
            }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable billsTable = new JTable(billsModel);
    private final JTable itemsTable = new JTable(itemsModel);
    private final JLabel summaryLabel = new JLabel("Sales summary: loading...");

    public SalesHistoryFrame() {
        setTitle("Supermarket - Sales History");
        setSize(1150, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(createTopPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(billsTable),
                new JScrollPane(itemsTable)
        );
        splitPane.setResizeWeight(0.65);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(summaryLabel, BorderLayout.WEST);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadBills(""));
        bottomPanel.add(refreshButton, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        add(bottomPanel, BorderLayout.SOUTH);

        billsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        billsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && billsTable.getSelectedRow() >= 0) {
                loadItemsForSelectedBill();
            }
        });

        loadBills("");
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Search Bills"));

        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        panel.add(new JLabel("Bill ID:"));
        panel.add(billSearchField);
        panel.add(searchButton);
        panel.add(showAllButton);

        searchButton.addActionListener(e -> searchBill());
        showAllButton.addActionListener(e -> {
            billSearchField.setText("");
            loadBills("");
        });

        return panel;
    }

    private void searchBill() {
        String billId = billSearchField.getText().trim();

        if (billId.isEmpty()) {
            loadBills("");
            return;
        }

        if (!billId.matches("\\d+")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid numeric bill ID.",
                    "Invalid Bill ID",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        loadBills(billId);
    }

    private void loadBills(String billId) {
        billsModel.setRowCount(0);
        itemsModel.setRowCount(0);

        StringBuilder sql = new StringBuilder("""
                SELECT bill_id, customer_name, phone_number, bill_date,
                       subtotal, discount, gst_amount, final_amount, payment_method
                FROM sales
                """);

        boolean filterById = billId != null && !billId.isBlank();
        if (filterById) {
            sql.append(" WHERE bill_id = ?");
        }
        sql.append(" ORDER BY bill_date DESC, bill_id DESC");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            if (filterById) {
                ps.setInt(1, Integer.parseInt(billId));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    billsModel.addRow(new Object[]{
                            rs.getInt("bill_id"),
                            rs.getString("customer_name"),
                            rs.getString("phone_number"),
                            rs.getTimestamp("bill_date"),
                            rs.getBigDecimal("subtotal"),
                            rs.getBigDecimal("discount"),
                            rs.getBigDecimal("gst_amount"),
                            rs.getBigDecimal("final_amount"),
                            rs.getString("payment_method")
                    });
                }
            }

            loadDailySummary(con);

            if (filterById && billsModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "No bill found with ID " + billId + "."
                );
            }

        } catch (SQLException | NumberFormatException e) {
            showError("Unable to load sales history", e);
        }
    }

    private void loadDailySummary(Connection con) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total_bills,
                       COALESCE(SUM(final_amount), 0) AS total_sales
                FROM sales
                WHERE DATE(bill_date) = CURDATE()
                """;

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int count = rs.getInt("total_bills");
                BigDecimal sales = rs.getBigDecimal("total_sales");

                summaryLabel.setText(
                        "Today's bills: " + count
                                + "   |   Today's sales: ₹" + sales
                );
            }
        }
    }

    private void loadItemsForSelectedBill() {
        int row = billsTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        int billId = ((Number) billsModel.getValueAt(row, 0)).intValue();
        itemsModel.setRowCount(0);

        String sql = """
                SELECT product_id, product_name, quantity,
                       unit_price, total_price
                FROM sale_items
                WHERE bill_id = ?
                ORDER BY sale_item_id
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, billId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    itemsModel.addRow(new Object[]{
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price"),
                            rs.getBigDecimal("total_price")
                    });
                }
            }

        } catch (SQLException e) {
            showError("Unable to load items for bill " + billId, e);
        }
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(
                this,
                message + ":\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
