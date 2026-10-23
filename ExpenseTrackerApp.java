package com.smartexpensetracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public final class ExpenseTrackerApp extends JFrame {
    private static final Color BACKGROUND = new Color(246, 248, 252);
    private static final Color CARD = Color.WHITE;
    private static final Color TEXT = new Color(28, 39, 58);
    private static final Color MUTED = new Color(111, 124, 145);
    private static final Color ACCENT = new Color(67, 97, 238);
    private static final Color ALERT = new Color(190, 54, 76);

    private final CsvExpenseStore store;
    private final List<Expense> expenses = new ArrayList<Expense>();
    private final ExpenseTableModel tableModel = new ExpenseTableModel();
    private final JTextField amountField = new JTextField();
    private final JTextField dateField = new JTextField(LocalDate.now().toString());
    private final JTextField budgetField = new JTextField();
    private final JLabel spentValue = new JLabel();
    private final JLabel budgetStatus = new JLabel();
    private final JTable expenseTable = new JTable(tableModel);
    private BigDecimal monthlyBudget = BigDecimal.ZERO;

    private ExpenseTrackerApp(CsvExpenseStore store) throws IOException {
        super("Smart Expense Tracker");
        this.store = store;
        expenses.addAll(store.loadExpenses());
        monthlyBudget = store.loadMonthlyBudget();
        buildWindow();
        refreshDashboard();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String home = System.getProperty("user.home");
                    Path dataDirectory = Paths.get(home, ".smart-expense-tracker");
                    ExpenseTrackerApp app = new ExpenseTrackerApp(new CsvExpenseStore(dataDirectory));
                    app.setVisible(true);
                } catch (IOException exception) {
                    JOptionPane.showMessageDialog(null,
                            "The saved expense data could not be loaded.\n" + exception.getMessage(),
                            "Unable to open expense data", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void buildWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 660));
        setSize(1120, 780);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(30, 34, 32, 34));
        content.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(buildSummary(), BorderLayout.NORTH);
        center.add(buildWorkspace(), BorderLayout.CENTER);
        content.add(center, BorderLayout.CENTER);
        setContentPane(content);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel eyebrow = createLabel("PERSONAL FINANCE  /  OVERVIEW", 11, ACCENT, Font.BOLD);
        JLabel title = new JLabel("Smart Expense Tracker");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("A little clarity goes a long way. Here's your spending at a glance.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 4));
        text.setOpaque(false);
        text.add(eyebrow);
        text.add(title);
        text.add(subtitle);
        panel.add(text, BorderLayout.WEST);
        return panel;
    }

    private JPanel buildSummary() {
        JPanel cards = new JPanel(new GridLayout(1, 2, 18, 0));
        cards.setOpaque(false);
        cards.add(buildSpentCard());
        cards.add(buildBudgetCard());
        return cards;
    }

    private JPanel buildSpentCard() {
        JPanel card = createCard(new BorderLayout(0, 11));
        JLabel label = createLabel("SPENT THIS MONTH", 11, MUTED, Font.BOLD);
        spentValue.setFont(new Font("SansSerif", Font.BOLD, 31));
        spentValue.setForeground(TEXT);
        card.add(label, BorderLayout.NORTH);
        card.add(spentValue, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBudgetCard() {
        JPanel card = createCard(new BorderLayout(0, 9));
        card.add(createLabel("MONTHLY BUDGET", 11, MUTED, Font.BOLD), BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout(9, 0));
        controls.setOpaque(false);
        budgetField.setToolTipText("Enter a positive amount with up to two decimal places");
        styleTextField(budgetField);
        controls.add(budgetField, BorderLayout.CENTER);
        JButton saveButton = createButton("Save budget", true);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveBudget();
            }
        });
        controls.add(saveButton, BorderLayout.EAST);
        card.add(controls, BorderLayout.CENTER);
        budgetStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        budgetStatus.setForeground(MUTED);
        card.add(budgetStatus, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(18, 0));
        workspace.setOpaque(false);
        workspace.add(buildExpenseForm(), BorderLayout.WEST);
        workspace.add(buildExpenseList(), BorderLayout.CENTER);
        return workspace;
    }

    private JPanel buildExpenseForm() {
        JPanel card = createCard(new BorderLayout(0, 18));
        card.setPreferredSize(new Dimension(285, 0));
        JLabel heading = createLabel("Add an expense", 18, TEXT, Font.BOLD);
        card.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 7));
        fields.setOpaque(false);
        fields.add(createLabel("Amount", 13, TEXT, Font.PLAIN));
        amountField.setToolTipText("Use a decimal point and up to two decimal places");
        styleTextField(amountField);
        fields.add(amountField);
        fields.add(createLabel("Category", 13, TEXT, Font.PLAIN));
        JComboBox<ExpenseCategory> categoryField = new JComboBox<ExpenseCategory>(ExpenseCategory.values());
        categoryField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        categoryField.setBackground(CARD);
        categoryField.setForeground(TEXT);
        categoryField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 239)),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        fields.add(categoryField);
        fields.add(createLabel("Date (YYYY-MM-DD)", 13, TEXT, Font.PLAIN));
        dateField.setToolTipText("Enter a date in YYYY-MM-DD format");
        styleTextField(dateField);
        fields.add(dateField);

        JButton addButton = createButton("Add expense", true);
        addButton.setMargin(new Insets(10, 12, 10, 12));
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addExpense(categoryField);
            }
        });
        JPanel formBody = new JPanel(new BorderLayout(0, 17));
        formBody.setOpaque(false);
        formBody.add(fields, BorderLayout.NORTH);
        formBody.add(addButton, BorderLayout.SOUTH);
        card.add(formBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildExpenseList() {
        JPanel card = createCard(new BorderLayout(0, 14));
        JPanel titleRow = new JPanel(new BorderLayout(10, 0));
        titleRow.setOpaque(false);
        JPanel listHeading = new JPanel(new GridLayout(2, 1, 0, 4));
        listHeading.setOpaque(false);
        listHeading.add(createLabel("Recent expenses", 18, TEXT, Font.BOLD));
        listHeading.add(createLabel("Your latest activity, all in one place", 12, MUTED, Font.PLAIN));
        titleRow.add(listHeading, BorderLayout.WEST);
        JButton removeButton = createButton("Remove selected", false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeSelectedExpense();
            }
        });
        titleRow.add(removeButton, BorderLayout.EAST);
        card.add(titleRow, BorderLayout.NORTH);

        expenseTable.setFillsViewportHeight(true);
        expenseTable.setRowHeight(34);
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expenseTable.setShowGrid(false);
        expenseTable.setIntercellSpacing(new Dimension(0, 0));
        expenseTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        expenseTable.setForeground(TEXT);
        expenseTable.setBackground(CARD);
        expenseTable.setSelectionBackground(new Color(235, 239, 255));
        expenseTable.setSelectionForeground(TEXT);
        expenseTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        expenseTable.getTableHeader().setBackground(new Color(248, 249, 252));
        expenseTable.getTableHeader().setForeground(MUTED);
        expenseTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        expenseTable.getTableHeader().setReorderingAllowed(false);
        expenseTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                Component component = super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                component.setForeground(TEXT);
                component.setBackground(selected
                        ? new Color(235, 239, 255)
                        : (row % 2 == 0 ? CARD : new Color(249, 250, 253)));
                setHorizontalAlignment(column == 2 ? RIGHT : LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return component;
            }
        });
        JScrollPane scrollPane = new JScrollPane(expenseTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(235, 238, 244)));
        scrollPane.getViewport().setBackground(CARD);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private void addExpense(JComboBox<ExpenseCategory> categoryField) {
        try {
            BigDecimal amount = parseMoney(amountField.getText(), "Expense amount");
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            ExpenseCategory category = (ExpenseCategory) categoryField.getSelectedItem();
            Expense expense = new Expense(amount, category, date);

            List<Expense> updatedExpenses = new ArrayList<Expense>(expenses);
            updatedExpenses.add(expense);
            store.saveExpenses(updatedExpenses);
            expenses.clear();
            expenses.addAll(updatedExpenses);

            amountField.setText("");
            dateField.setText(LocalDate.now().toString());
            refreshDashboard();
        } catch (DateTimeParseException exception) {
            showInputError("Enter a valid date in YYYY-MM-DD format.");
        } catch (IllegalArgumentException exception) {
            showInputError(exception.getMessage());
        } catch (IOException exception) {
            showStorageError("The expense could not be saved.", exception);
        }
    }

    private void saveBudget() {
        try {
            BigDecimal budget = parseMoney(budgetField.getText(), "Monthly budget");
            store.saveMonthlyBudget(budget);
            monthlyBudget = budget;
            refreshDashboard();
        } catch (IllegalArgumentException exception) {
            showInputError(exception.getMessage());
        } catch (IOException exception) {
            showStorageError("The monthly budget could not be saved.", exception);
        }
    }

    private void removeSelectedExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select an expense to remove.",
                    "No expense selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Expense selectedExpense = tableModel.getExpenseAt(selectedRow);
        List<Expense> updatedExpenses = new ArrayList<Expense>(expenses);
        updatedExpenses.remove(selectedExpense);
        try {
            store.saveExpenses(updatedExpenses);
            expenses.clear();
            expenses.addAll(updatedExpenses);
            refreshDashboard();
        } catch (IOException exception) {
            showStorageError("The selected expense could not be removed.", exception);
        }
    }

    private void refreshDashboard() {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal spent = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            if (YearMonth.from(expense.getDate()).equals(currentMonth)) {
                spent = spent.add(expense.getAmount());
            }
        }
        spentValue.setText(formatMoney(spent));
        budgetField.setText(monthlyBudget.signum() > 0 ? monthlyBudget.toPlainString() : "");
        if (monthlyBudget.signum() == 0) {
            budgetStatus.setText("Set a budget to track your monthly limit.");
            budgetStatus.setForeground(MUTED);
        } else if (spent.compareTo(monthlyBudget) >= 0) {
            BigDecimal overBudget = spent.subtract(monthlyBudget);
            budgetStatus.setText(overBudget.signum() == 0
                    ? "Monthly budget limit reached"
                    : "Budget reached — " + formatMoney(overBudget) + " over");
            budgetStatus.setForeground(ALERT);
        } else {
            budgetStatus.setText(formatMoney(monthlyBudget.subtract(spent)) + " remaining this month");
            budgetStatus.setForeground(new Color(37, 125, 86));
        }

        List<Expense> sortedExpenses = new ArrayList<Expense>(expenses);
        Collections.sort(sortedExpenses, new Comparator<Expense>() {
            @Override
            public int compare(Expense left, Expense right) {
                return right.getDate().compareTo(left.getDate());
            }
        });
        tableModel.setExpenses(sortedExpenses);
    }

    private static BigDecimal parseMoney(String input, String fieldName) {
        String value = input.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than zero.");
            }
            if (amount.scale() > 2) {
                throw new IllegalArgumentException(fieldName + " can have at most two decimal places.");
            }
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a number, such as 12.50.");
        }
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }

    private void showInputError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your entry", JOptionPane.WARNING_MESSAGE);
    }

    private void showStorageError(String message, IOException exception) {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(),
                "Storage error", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel createCard(BorderLayout layout) {
        return new CardPanel(layout);
    }

    private static void styleTextField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(new Color(249, 250, 253));
        field.setCaretColor(ACCENT);
        field.setMargin(new Insets(10, 12, 10, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 239)),
                BorderFactory.createEmptyBorder(1, 2, 1, 2)));
    }

    private static JLabel createLabel(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", style, size));
        label.setForeground(color);
        return label;
    }

    private static JButton createButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(primary ? ACCENT : new Color(240, 243, 247));
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT : new Color(230, 234, 242)),
                BorderFactory.createEmptyBorder(8, 13, 8, 13)));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    @SuppressWarnings("serial")
    private static final class CardPanel extends JPanel {
        private CardPanel(BorderLayout layout) {
            super(layout);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(19, 20, 19, 20));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(new Color(31, 48, 78, 12));
            graphics2D.fillRoundRect(1, 3, getWidth() - 3, getHeight() - 4, 20, 20);
            graphics2D.setColor(CARD);
            graphics2D.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 5, 20, 20);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }

    @SuppressWarnings("serial")
    private static final class ExpenseTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Date", "Category", "Amount"};
        private List<Expense> rows = Collections.emptyList();

        public void setExpenses(List<Expense> expenses) {
            rows = new ArrayList<Expense>(expenses);
            fireTableDataChanged();
        }

        public Expense getExpenseAt(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Expense expense = rows.get(row);
            switch (column) {
                case 0:
                    return expense.getDate().toString();
                case 1:
                    return expense.getCategory().getLabel();
                case 2:
                    return formatMoney(expense.getAmount());
                default:
                    throw new IndexOutOfBoundsException("Unknown column: " + column);
            }
        }
    }
}
