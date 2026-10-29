package src;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.RenderingHints;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main extends JFrame {

    private static final Color GREEN = new Color(24, 102, 71);
    private static final Color DARK_GREEN = new Color(17, 74, 52);
    private static final Color PAGE_BACKGROUND = new Color(241, 245, 242);
    private static final Color MUTED = new Color(105, 117, 110);
    private static final Color BORDER = new Color(224, 232, 226);

    private final ProductDAO productDAO = new ProductDAO();
    private final BillingService billingService = new BillingService();

    private final List<Product> cart = new ArrayList<>();
    private List<Product> products = new ArrayList<>();

    private final JTextField customerField = new JTextField(15);
    private final JTextField phoneField = new JTextField(12);

    private final JComboBox<Product> productCombo = new JComboBox<>();

    private final JSpinner quantitySpinner =
            new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

    private final JComboBox<String> paymentCombo =
            new JComboBox<>(new String[]{"Cash", "UPI", "Card"});

    private final DefaultTableModel cartModel = new DefaultTableModel(
            new String[]{
                    "Product ID", "Product Name", "Price",
                    "GST %", "Quantity", "Total"
            }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable cartTable = new JTable(cartModel);

    private final JLabel subtotalLabel = new JLabel("Subtotal: ₹0.00");
    private final JLabel discountLabel = new JLabel("Discount: ₹0.00");
    private final JLabel gstLabel = new JLabel("GST: ₹0.00");
    private final JLabel totalLabel = new JLabel("Total: ₹0.00");

    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public Main() {
        setTitle("Supermarket Billing System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(PAGE_BACKGROUND);

        createHeader();
        createProductPanel();
        createCartPanel();
        createFooter();

        loadProducts();
    }

    private void createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 0, 18));

        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(DARK_GREEN);
        brand.setBorder(BorderFactory.createEmptyBorder(13, 18, 13, 18));

        JLabel title = new JLabel("SUPERMARKET");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Billing");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(new Color(202, 228, 213));
        JPanel brandText = new JPanel(new GridLayout(0, 1, 0, 2));
        brandText.setOpaque(false);
        brandText.add(title);
        brandText.add(subtitle);
        brand.add(brandText, BorderLayout.WEST);

        JPanel customerPanel = createCardPanel();
        customerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        customerPanel.setBorder(cardBorder(8, 12));
        customerPanel.add(new JLabel("Customer Name:"));
        customerPanel.add(customerField);
        customerPanel.add(new JLabel("Phone:"));
        customerPanel.add(phoneField);

        header.add(brand, BorderLayout.NORTH);
        header.add(customerPanel, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
    }

    private void createProductPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBorder(cardBorder(14, 14));

        JButton addButton = new JButton("Add to Cart");
        JButton refreshButton = new JButton("Refresh Products");

        panel.add(sectionTitle("ADD PRODUCTS"));
        panel.add(Box.createVerticalStrut(14));
        JLabel productLabel = fieldLabel("Product");
        productLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(productLabel);
        panel.add(Box.createVerticalStrut(5));
        productCombo.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, productCombo.getPreferredSize().height
        ));
        productCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(productCombo);
        panel.add(Box.createVerticalStrut(12));
        JLabel quantityLabel = fieldLabel("Quantity");
        JPanel quantityRow = new JPanel(new BorderLayout(12, 0));
        quantityRow.setOpaque(false);
        quantityRow.add(quantityLabel, BorderLayout.WEST);
        quantityRow.add(quantitySpinner, BorderLayout.EAST);
        quantityRow.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, quantityRow.getPreferredSize().height
        ));
        quantityRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(quantityRow);
        panel.add(Box.createVerticalStrut(14));
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 7));
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(refreshButton);
        actions.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, actions.getPreferredSize().height
        ));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(actions);
        panel.setPreferredSize(new Dimension(300, panel.getPreferredSize().height));

        addButton.addActionListener(e -> addToCart());
        refreshButton.addActionListener(e -> loadProducts());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        wrapper.add(panel, BorderLayout.NORTH);

        add(wrapper, BorderLayout.WEST);
    }

    private void createCartPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(cardBorder(14, 14));
        panel.add(sectionTitle("SHOPPING CART"), BorderLayout.NORTH);

        styleTable(cartTable);
        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton removeButton = new JButton("Remove Selected Item");
        removeButton.addActionListener(e -> removeSelectedItem());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(removeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));
        wrapper.add(panel, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private void createFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 9));
        footer.setOpaque(false);

        JPanel totalsPanel = createCardPanel();
        totalsPanel.setLayout(new GridLayout(1, 4, 10, 0));
        totalsPanel.setBorder(cardBorder(11, 14));
        styleTotalLabel(subtotalLabel);
        styleTotalLabel(discountLabel);
        styleTotalLabel(gstLabel);
        totalLabel.setForeground(GREEN);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 15));
        totalsPanel.add(subtotalLabel);
        totalsPanel.add(discountLabel);
        totalsPanel.add(gstLabel);
        totalsPanel.add(totalLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        buttons.setOpaque(false);

        JButton generateButton = new JButton("Generate Bill") {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        JButton clearButton = new JButton("Clear Cart");
        JButton manageProductsButton = new JButton("Manage Products");
        JButton salesHistoryButton = new JButton("Sales History");

        buttons.add(new JLabel("Payment:"));
        buttons.add(paymentCombo);
        buttons.add(generateButton);
        buttons.add(clearButton);
        buttons.add(manageProductsButton);
        buttons.add(salesHistoryButton);
        stylePrimaryButton(generateButton);

        generateButton.addActionListener(e -> generateBill());
        clearButton.addActionListener(e -> clearCart());

        manageProductsButton.addActionListener(e -> {
            ProductManagementFrame frame = new ProductManagementFrame();
            frame.setVisible(true);
        });

        salesHistoryButton.addActionListener(e -> {
            SalesHistoryFrame frame = new SalesHistoryFrame();
            frame.setVisible(true);
        });

        footer.add(totalsPanel, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));

        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private javax.swing.border.Border cardBorder(int verticalPadding, int horizontalPadding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(
                        verticalPadding, horizontalPadding,
                        verticalPadding, horizontalPadding
                )
        );
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DARK_GREEN);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(29);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setForeground(new Color(47, 57, 51));
        table.setSelectionBackground(new Color(221, 239, 228));
        table.setSelectionForeground(DARK_GREEN);

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(235, 242, 237));
        header.setForeground(DARK_GREEN);
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 33));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component cell = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column
                );
                if (!isSelected) {
                    cell.setBackground(row % 2 == 0
                            ? Color.WHITE : new Color(248, 250, 248));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return cell;
            }
        };
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    private void styleTotalLabel(JLabel label) {
        label.setForeground(MUTED);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
    }

    private void stylePrimaryButton(JButton button) {
        button.setForeground(Color.WHITE);
        button.setBackground(GREEN);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    private void loadProducts() {
        try {
            products = productDAO.getAllProducts();
            productCombo.removeAllItems();

            for (Product product : products) {
                productCombo.addItem(product);
            }

            if (products.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No products found. Add products to the database."
                );
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load products:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void addToCart() {
        Product selected = (Product) productCombo.getSelectedItem();

        if (selected == null) {
            JOptionPane.showMessageDialog(
                    this, "Please select a product."
            );
            return;
        }

        int quantity = (Integer) quantitySpinner.getValue();
        int alreadyAdded = 0;

        for (Product product : cart) {
            if (product.getProductId() == selected.getProductId()) {
                alreadyAdded++;
            }
        }

        if (alreadyAdded + quantity > selected.getStockQuantity()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Insufficient stock for " + selected.getProductName()
            );
            return;
        }

        for (int i = 0; i < quantity; i++) {
            cart.add(selected);
        }

        refreshCartTable();
    }

    private void removeSelectedItem() {
        int selectedRow = cartTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    this, "Please select an item to remove."
            );
            return;
        }

        int productId = ((Number)
                cartModel.getValueAt(selectedRow, 0)).intValue();

        for (int i = cart.size() - 1; i >= 0; i--) {
            if (cart.get(i).getProductId() == productId) {
                cart.remove(i);
                break;
            }
        }

        refreshCartTable();
    }

    private Map<Integer, List<Product>> groupCartItems() {
        Map<Integer, List<Product>> grouped = new LinkedHashMap<>();

        for (Product product : cart) {
            grouped.computeIfAbsent(
                    product.getProductId(),
                    key -> new ArrayList<>()
            ).add(product);
        }

        return grouped;
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);

        BigDecimal subtotal = BigDecimal.ZERO;

        for (List<Product> items : groupCartItems().values()) {
            Product product = items.get(0);
            int quantity = items.size();

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            subtotal = subtotal.add(lineTotal);

            cartModel.addRow(new Object[]{
                    product.getProductId(),
                    product.getProductName(),
                    currency.format(product.getPrice()),
                    product.getGstRate() + "%",
                    quantity,
                    currency.format(lineTotal)
            });
        }

        BigDecimal discountRate = BigDecimal.ZERO;

        if (subtotal.compareTo(new BigDecimal("5000")) >= 0) {
            discountRate = new BigDecimal("10");
        } else if (subtotal.compareTo(new BigDecimal("2000")) >= 0) {
            discountRate = new BigDecimal("5");
        }

        BigDecimal discount = subtotal.multiply(discountRate)
                .divide(
                        new BigDecimal("100"),
                        2,
                        java.math.RoundingMode.HALF_UP
                );

        BigDecimal taxableSubtotal = subtotal.subtract(discount);
        BigDecimal gst = BigDecimal.ZERO;

        for (List<Product> items : groupCartItems().values()) {
            Product product = items.get(0);

            BigDecimal itemSubtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(items.size()));

            BigDecimal itemDiscount = subtotal.signum() == 0
                    ? BigDecimal.ZERO
                    : discount.multiply(itemSubtotal)
                            .divide(
                                    subtotal,
                                    10,
                                    java.math.RoundingMode.HALF_UP
                            );

            BigDecimal itemGST = itemSubtotal.subtract(itemDiscount)
                    .multiply(product.getGstRate())
                    .divide(
                            new BigDecimal("100"),
                            2,
                            java.math.RoundingMode.HALF_UP
                    );

            gst = gst.add(itemGST);
        }

        BigDecimal total = taxableSubtotal.add(gst);

        subtotalLabel.setText("Subtotal: " + currency.format(subtotal));
        discountLabel.setText("Discount: " + currency.format(discount));
        gstLabel.setText("GST: " + currency.format(gst));
        totalLabel.setText("Total: " + currency.format(total));
    }

    private void generateBill() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty.");
            return;
        }

        String customerName = customerField.getText().trim();
        String phoneNumber = phoneField.getText().trim();
        String paymentMethod = (String) paymentCombo.getSelectedItem();

        if (customerName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Please enter the customer's name."
            );
            return;
        }

        if (!phoneNumber.isEmpty() && !phoneNumber.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(
                    this, "Enter a valid 10-digit phone number."
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Complete this sale?",
                "Confirm Billing",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        List<Product> billedProducts = new ArrayList<>(cart);

        try {
            BillingService.BillResult result = billingService.completeSale(
                    customerName,
                    phoneNumber,
                    paymentMethod,
                    cart
            );

            ReceiptFrame receiptFrame = new ReceiptFrame(
                    result,
                    customerName,
                    phoneNumber,
                    paymentMethod,
                    billedProducts
            );
            receiptFrame.setVisible(true);

            cart.clear();
            refreshCartTable();
            loadProducts();

            customerField.setText("");
            phoneField.setText("");
            quantitySpinner.setValue(1);

        } catch (SQLException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to complete sale:\n" + e.getMessage(),
                    "Billing Error",
                    JOptionPane.ERROR_MESSAGE
            );

            loadProducts();
        }
    }

    private void clearCart() {
        if (cart.isEmpty()) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Clear all items from the cart?",
                "Clear Cart",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            cart.clear();
            refreshCartTable();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main application = new Main();
            application.setVisible(true);
        });
    }
}
