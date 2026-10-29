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
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReceiptFrame extends JFrame {

    private static final Color GREEN = new Color(24, 102, 71);
    private static final Color DARK_GREEN = new Color(17, 74, 52);
    private static final Color PAGE_BACKGROUND = new Color(241, 245, 242);
    private static final Color MUTED = new Color(105, 117, 110);
    private static final Color BORDER = new Color(224, 232, 226);

    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public ReceiptFrame(
            BillingService.BillResult result,
            String customerName,
            String phoneNumber,
            String paymentMethod,
            List<Product> billedProducts
    ) {
        setTitle("Sales Receipt - Bill #" + result.getBillId());
        setSize(900, 680);
        setMinimumSize(new Dimension(760, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(PAGE_BACKGROUND);
        setLayout(new BorderLayout(0, 12));

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 0, 18));
        header.add(createBrandPanel(result), BorderLayout.NORTH);
        header.add(createDetailsPanel(
                customerName, phoneNumber, paymentMethod
        ), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel itemsWrapper = new JPanel(new BorderLayout());
        itemsWrapper.setOpaque(false);
        itemsWrapper.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        itemsWrapper.add(createItemsPanel(billedProducts), BorderLayout.CENTER);
        add(itemsWrapper, BorderLayout.CENTER);

        add(createFooter(result), BorderLayout.SOUTH);
    }

    private JPanel createBrandPanel(BillingService.BillResult result) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_GREEN);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("SUPERMARKET");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JLabel subtitle = new JLabel("Sales receipt");
        subtitle.setForeground(new Color(202, 228, 213));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 2));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        JLabel billNumber = new JLabel("BILL  #" + result.getBillId());
        billNumber.setForeground(Color.WHITE);
        billNumber.setFont(new Font("Arial", Font.BOLD, 14));

        panel.add(heading, BorderLayout.WEST);
        panel.add(billNumber, BorderLayout.EAST);
        return panel;
    }

    private JPanel createDetailsPanel(
            String customerName,
            String phoneNumber,
            String paymentMethod
    ) {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout(0, 9));
        panel.add(sectionTitle("BILL DETAILS"), BorderLayout.NORTH);

        String date = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd MMM yyyy  |  hh:mm a")
        );
        JPanel details = new JPanel(new GridLayout(1, 4, 14, 0));
        details.setOpaque(false);
        details.add(detail("CUSTOMER", customerName));
        details.add(detail("PHONE",
                phoneNumber.isEmpty() ? "Not provided" : phoneNumber));
        details.add(detail("DATE & TIME", date));
        details.add(detail("PAYMENT", paymentMethod));
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createItemsPanel(List<Product> billedProducts) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{
                        "Product", "Quantity", "Unit Price", "GST", "Total"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        Map<Integer, List<Product>> grouped = new LinkedHashMap<>();
        for (Product product : billedProducts) {
            grouped.computeIfAbsent(
                    product.getProductId(),
                    key -> new ArrayList<>()
            ).add(product);
        }

        for (List<Product> items : grouped.values()) {
            Product product = items.get(0);
            int quantity = items.size();
            BigDecimal amount = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            model.addRow(new Object[]{
                    product.getProductName(),
                    quantity,
                    currency.format(product.getPrice()),
                    product.getGstRate() + "%",
                    currency.format(amount)
            });
        }

        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.add(sectionTitle("PURCHASED ITEMS"), BorderLayout.NORTH);

        JTable table = new JTable(model);
        styleTable(table);
        table.getColumnModel().getColumn(1).setPreferredWidth(85);
        table.getColumnModel().getColumn(1).setMaxWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(3).setMaxWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFooter(BillingService.BillResult result) {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));
        footer.add(createTotalsPanel(result), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close Receipt");
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(GREEN);
        closeButton.setFont(new Font("Arial", Font.BOLD, 12));
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(true);
        closeButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        closeButton.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(closeButton);
        footer.add(actions, BorderLayout.SOUTH);
        return footer;
    }

    private JPanel createTotalsPanel(BillingService.BillResult result) {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout(14, 0));

        JPanel amounts = new JPanel(new GridLayout(1, 3, 12, 0));
        amounts.setOpaque(false);
        amounts.add(amount("SUBTOTAL", result.getSubtotal(), MUTED));
        amounts.add(amount("DISCOUNT", result.getDiscount(), GREEN));
        amounts.add(amount("GST", result.getGstAmount(), MUTED));

        JPanel total = new JPanel(new GridLayout(0, 1, 0, 3));
        total.setOpaque(false);
        JLabel caption = sectionTitle("GRAND TOTAL");
        JLabel value = new JLabel(currency.format(result.getFinalAmount()));
        value.setForeground(GREEN);
        value.setFont(new Font("Arial", Font.BOLD, 20));
        total.add(caption);
        total.add(value);

        panel.add(amounts, BorderLayout.CENTER);
        panel.add(total, BorderLayout.EAST);
        return panel;
    }

    private JPanel amount(String caption, BigDecimal value, Color valueColor) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.setOpaque(false);

        JLabel label = new JLabel(caption);
        label.setForeground(MUTED);
        label.setFont(new Font("Arial", Font.BOLD, 10));
        JLabel amount = new JLabel(currency.format(value));
        amount.setForeground(valueColor);
        amount.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(label);
        panel.add(amount);
        return panel;
    }

    private JPanel detail(String caption, String value) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 3));
        panel.setOpaque(false);

        JLabel label = new JLabel(caption);
        label.setForeground(MUTED);
        label.setFont(new Font("Arial", Font.BOLD, 10));
        JLabel text = new JLabel(value);
        text.setForeground(new Color(43, 53, 47));
        text.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(label);
        panel.add(text);
        return panel;
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
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                if (column == 1 || column == 3) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (column >= 2) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return cell;
            }
        };
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DARK_GREEN);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }
}
