import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.io.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

/**
 * =====================================================================================
 * SMART HOSTEL MANAGEMENT SYSTEM
 * =====================================================================================
 * A single-file Java Swing desktop application that manages students, rooms, fees,
 * complaints, visitors, attendance, staff and more for a college hostel.
 *
 * This version keeps every feature of the original application but the code has been
 * rewritten in a simple, readable style so that it clearly demonstrates the concepts
 * taught in CSE2006 - Programming in Java. Each syllabus unit is called out in a
 * comment near the code that demonstrates it:
 *
 *   UNIT 1 - Java basics, data types, operators, flow control (if/for/while/switch)
 *   UNIT 2 - OOP: classes, objects, constructors, inheritance, interfaces, polymorphism
 *   UNIT 3 - Exception handling and multithreading
 *   UNIT 4 - Strings, arrays, the Collections framework and I/O streams
 *   UNIT 5 - JDBC and an outline of JPA concepts
 *
 * Run with:  javac Main.java   then   java Main
 * =====================================================================================
 */
public class Main {

    // ---------------------------------------------------------------------------
    // UNIT 1: constants use simple data types (Color, Font, String) - basic Java.
    // ---------------------------------------------------------------------------
    static final Color COLOR_BACKGROUND = new Color(15, 23, 42);
    static final Color COLOR_NAVBAR = new Color(18, 30, 52);
    static final Color COLOR_SURFACE = new Color(29, 42, 66);
    static final Color COLOR_SURFACE_LIGHT = new Color(37, 52, 78);
    static final Color COLOR_ACCENT = new Color(39, 201, 160);
    static final Color COLOR_BLUE = new Color(76, 139, 245);
    static final Color COLOR_WARNING = new Color(244, 178, 62);
    static final Color COLOR_DANGER = new Color(239, 92, 105);
    static final Color COLOR_TEXT = new Color(235, 241, 250);
    static final Color COLOR_MUTED = new Color(154, 173, 201);
    static final Color COLOR_BORDER = new Color(67, 85, 115);

    static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font FONT_BOLD = new Font("Segoe UI Semibold", Font.PLAIN, 14);

    // Names of every page/module reachable from the sidebar.
    static final String[] PAGE_NAMES = {
            "Dashboard", "Students", "Rooms", "Fees", "Complaints", "Visitors",
            "Attendance", "Mess & Meals", "Leave & Outpass", "Inventory",
            "Notice Board", "Staff", "Reports", "Settings", "About"
    };

    private final JFrame frame = new JFrame("Smart Hostel Management System");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JLabel pageTitleLabel = new JLabel("Dashboard");
    private final JLabel pageSubtitleLabel = new JLabel("Overview of your hostel today");

    // One live table model per module - editing a table immediately updates the view.
    private final Map<String, DefaultTableModel> tableModels = new HashMap<>();

    private final DataStore dataStore = new DataStore();
    private final ApplicationLog auditLog = ApplicationLog.getInstance();
    private boolean clockStarted = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        seedSampleData();
        showLoginScreen();
    }

    // =====================================================================================
    // LOGIN SCREEN
    // =====================================================================================

    private void showLoginScreen() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1120, 720);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setLocationRelativeTo(null);
        frame.setContentPane(buildLoginPanel());
        frame.setVisible(true);
    }

    private JPanel buildLoginPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(COLOR_BACKGROUND);
        root.setBorder(new EmptyBorder(45, 55, 45, 55));

        // Left side: branding.
        JPanel brandPanel = new JPanel();
        brandPanel.setBackground(COLOR_NAVBAR);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.add(makeLabel("SMART HOSTEL", 30, COLOR_TEXT));
        brandPanel.add(makeLabel("Management that feels effortless.", 16, COLOR_MUTED));
        brandPanel.add(makeLabel("Student living, rooms, payments and operations - all in one place.", 14, COLOR_MUTED));
        root.add(brandPanel, BorderLayout.CENTER);

        // Right side: the sign-in form.
        JPanel formPanel = new JPanel();
        formPanel.setBackground(COLOR_BACKGROUND);
        formPanel.setPreferredSize(new Dimension(390, 0));
        formPanel.setBorder(new EmptyBorder(35, 40, 35, 40));
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        JTextField emailField = createTextField();
        JPasswordField passwordField = createPasswordField();

        JLabel welcomeLabel = makeLabel("Welcome back", 26, COLOR_TEXT);
        JLabel signInLabel = makeLabel("Sign in to your administrator account", 14, COLOR_MUTED);
        JLabel emailCaption = makeLabel("EMAIL ADDRESS", 11, COLOR_MUTED);
        JLabel passwordCaption = makeLabel("PASSWORD", 11, COLOR_MUTED);
        // Left-align every row and stop BoxLayout from stretching the labels full width.
        for (JComponent component : new JComponent[]{welcomeLabel, signInLabel, emailCaption, passwordCaption}) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        formPanel.add(welcomeLabel);
        formPanel.add(signInLabel);
        formPanel.add(Box.createVerticalStrut(25));
        formPanel.add(emailCaption);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(emailField);
        formPanel.add(Box.createVerticalStrut(14));
        formPanel.add(passwordCaption);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(passwordField);
        formPanel.add(Box.createVerticalStrut(18));

        JButton signInButton = createButton("Sign in to dashboard", COLOR_ACCENT);
        signInButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        // UNIT 1: if/else flow control validates the two input fields.
        signInButton.addActionListener(event -> {
            String email = emailField.getText().trim();
            char[] password = passwordField.getPassword();
            if (email.isEmpty() || password.length == 0) {
                showError("Please enter your email and password.");
            } else {
                buildMainApplication();
            }
        });
        formPanel.add(signInButton);
        formPanel.add(Box.createVerticalStrut(20));
        JLabel demoCredentialsLabel = makeLabel("Demo credentials: admin@hostel.com / admin", 12, COLOR_MUTED);
        demoCredentialsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(demoCredentialsLabel);

        root.add(formPanel, BorderLayout.EAST);
        return root;
    }

    // =====================================================================================
    // MAIN APPLICATION SHELL (sidebar + top bar + pages)
    // =====================================================================================

    private void buildMainApplication() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildTopBar(), BorderLayout.NORTH);
        contentPanel.setBackground(COLOR_BACKGROUND);

        // Build every entity module (Students, Rooms, Fees, ...) first so their table
        // models are already filled with data by the time the Dashboard reads them.
        Map<String, JComponent> builtPages = new LinkedHashMap<>();
        for (String moduleName : moduleNames()) {
            builtPages.put(moduleName, buildEntityPage(moduleName));
        }
        // Now build the special pages, which may summarise data from the modules above.
        builtPages.put("Dashboard", buildDashboardPage());
        builtPages.put("Reports", buildReportsPage());
        builtPages.put("Settings", buildSettingsPage());
        builtPages.put("About", buildAboutPage());

        // UNIT 1: a simple for-each loop adds every page to the card layout, in nav order.
        for (String pageName : PAGE_NAMES) {
            contentPanel.add(builtPages.get(pageName), pageName);
        }
        root.add(contentPanel, BorderLayout.CENTER);

        frame.setContentPane(root);
        showPage("Dashboard");
        frame.revalidate();

        // UNIT 3: start a background thread once, the first time the dashboard opens.
        if (!clockStarted) {
            new DashboardClock(pageSubtitleLabel).start();
            clockStarted = true;
        }
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(230, 0));
        side.setBackground(COLOR_NAVBAR);

        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logo.setBackground(COLOR_NAVBAR);
        logo.setBorder(new EmptyBorder(18, 14, 18, 14));
        logo.add(makeLabel("SMART HOSTEL", 17, COLOR_TEXT));
        side.add(logo, BorderLayout.NORTH);

        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setBackground(COLOR_NAVBAR);
        navList.setBorder(new EmptyBorder(6, 14, 18, 14));
        for (String pageName : PAGE_NAMES) {
            navList.add(createNavButton(pageName));
        }

        JScrollPane scrollPane = new JScrollPane(navList);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        side.add(scrollPane, BorderLayout.CENTER);
        return side;
    }

    private JButton createNavButton(String pageName) {
        JButton button = new JButton(pageName);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(220, 42));
        button.setFont(FONT_BOLD);
        button.setForeground(COLOR_MUTED);
        button.setBackground(COLOR_NAVBAR);
        button.setBorder(new EmptyBorder(8, 12, 8, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> showPage(pageName));
        return button;
    }

    private JComponent buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(COLOR_BACKGROUND);
        top.setBorder(new EmptyBorder(18, 28, 14, 28));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        pageTitleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 25));
        pageTitleLabel.setForeground(COLOR_TEXT);
        pageSubtitleLabel.setFont(FONT_NORMAL);
        pageSubtitleLabel.setForeground(COLOR_MUTED);
        titleBox.add(pageTitleLabel);
        titleBox.add(pageSubtitleLabel);
        top.add(titleBox, BorderLayout.WEST);

        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        userBox.setOpaque(false);
        userBox.add(makeLabel("Admin User", 14, COLOR_TEXT));
        top.add(userBox, BorderLayout.EAST);
        return top;
    }

    private void showPage(String pageName) {
        pageTitleLabel.setText(pageName);
        cardLayout.show(contentPanel, pageName);
    }

    // =====================================================================================
    // PAGE BUILDING
    // Every module (Students, Rooms, Fees, ...) shares the same table + add-record layout,
    // so one generic method builds all of them. This is an example of code reuse through
    // a single parametrised method rather than copy-pasting the same code many times.
    // =====================================================================================

    // The dashboard's inner content panel is kept as a field so a "Refresh" button
    // can rebuild the stat cards and charts after records are added or removed.
    private final JPanel dashboardContent = new JPanel();

    private JComponent buildDashboardPage() {
        dashboardContent.setBackground(COLOR_BACKGROUND);
        dashboardContent.setLayout(new BoxLayout(dashboardContent, BoxLayout.Y_AXIS));
        dashboardContent.setBorder(new EmptyBorder(10, 28, 25, 28));
        refreshDashboardContent();
        return wrapInScrollPane(dashboardContent);
    }

    /** Clears and rebuilds every stat card and chart from the current table data. */
    private void refreshDashboardContent() {
        dashboardContent.removeAll();

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel heading = makeLabel("Overview of your hostel today", 14, COLOR_MUTED);
        headerRow.add(heading, BorderLayout.WEST);
        JButton refreshButton = createButton("Refresh", COLOR_SURFACE_LIGHT);
        refreshButton.addActionListener(event -> refreshDashboardContent());
        headerRow.add(refreshButton, BorderLayout.EAST);
        dashboardContent.add(headerRow);
        dashboardContent.add(Box.createVerticalStrut(16));

        JPanel statsRow = buildStatCardsRow();
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dashboardContent.add(statsRow);
        dashboardContent.add(Box.createVerticalStrut(20));

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 20, 0));
        chartsRow.setOpaque(false);
        chartsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        chartsRow.add(buildRoomOccupancyChartCard());
        chartsRow.add(buildComplaintStatusChartCard());
        dashboardContent.add(chartsRow);
        dashboardContent.add(Box.createVerticalStrut(20));

        JComponent feeTrendCard = buildFeeTrendChartCard();
        feeTrendCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        dashboardContent.add(feeTrendCard);
        dashboardContent.add(Box.createVerticalStrut(20));

        JComponent activityCard = buildRecentActivityCard();
        activityCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        dashboardContent.add(activityCard);

        dashboardContent.revalidate();
        dashboardContent.repaint();
    }

    // ---------------------------------------------------------------------------
    // UNIT 4 (arrays/collections) + UNIT 1 (loops/arithmetic operators): the stat
    // cards and charts below all read live numbers out of the table models by
    // looping over the rows and totalling up values with simple operators.
    // ---------------------------------------------------------------------------

    private JPanel buildStatCardsRow() {
        DefaultTableModel students = tableModels.get("Students");
        DefaultTableModel rooms = tableModels.get("Rooms");
        DefaultTableModel fees = tableModels.get("Fees");
        DefaultTableModel complaints = tableModels.get("Complaints");

        int totalStudents = students == null ? 0 : students.getRowCount();

        int occupiedBeds = 0;
        int totalCapacity = 0;
        if (rooms != null) {
            for (int row = 0; row < rooms.getRowCount(); row++) {
                totalCapacity += parseIntSafe(String.valueOf(rooms.getValueAt(row, 2))); // Capacity
                occupiedBeds += parseIntSafe(String.valueOf(rooms.getValueAt(row, 3)));  // Occupied
            }
        }

        double feesCollected = 0;
        if (fees != null) {
            for (int row = 0; row < fees.getRowCount(); row++) {
                String status = String.valueOf(fees.getValueAt(row, 5)); // Status
                if (status.equalsIgnoreCase("Paid")) {
                    feesCollected += parseAmountSafe(String.valueOf(fees.getValueAt(row, 2))); // Amount
                }
            }
        }

        int openComplaints = 0;
        if (complaints != null) {
            for (int row = 0; row < complaints.getRowCount(); row++) {
                String status = String.valueOf(complaints.getValueAt(row, 4)); // Status
                if (!status.equalsIgnoreCase("Resolved")) {
                    openComplaints++;
                }
            }
        }

        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(buildStatCard("Total Students", String.valueOf(totalStudents), COLOR_BLUE));
        row.add(buildStatCard("Beds Occupied", occupiedBeds + " / " + totalCapacity, COLOR_ACCENT));
        row.add(buildStatCard("Fees Collected", "Rs. " + Math.round(feesCollected), COLOR_WARNING));
        row.add(buildStatCard("Open Complaints", String.valueOf(openComplaints), COLOR_DANGER));
        return row;
    }

    private JPanel buildStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COLOR_SURFACE);
        card.setBorder(new CompoundBorder(new LineBorder(COLOR_SURFACE_LIGHT, 1, true), new EmptyBorder(16, 18, 16, 18)));

        JLabel valueLabel = makeLabel(value, 24, accentColor);
        JLabel titleLabel = makeLabel(title, 13, COLOR_MUTED);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(titleLabel);
        return card;
    }

    private JComponent buildRoomOccupancyChartCard() {
        DefaultTableModel rooms = tableModels.get("Rooms");
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        if (rooms != null) {
            for (int row = 0; row < rooms.getRowCount(); row++) {
                labels.add(String.valueOf(rooms.getValueAt(row, 0)));                       // Room No.
                values.add((double) parseIntSafe(String.valueOf(rooms.getValueAt(row, 3)))); // Occupied
            }
        }
        BarChartPanel chart = new BarChartPanel(labels, values, COLOR_ACCENT);
        return wrapChartInCard("Room occupancy", chart);
    }

    private JComponent buildComplaintStatusChartCard() {
        DefaultTableModel complaints = tableModels.get("Complaints");
        // UNIT 4: LinkedHashMap keeps categories in first-seen order while counting them.
        Map<String, Integer> countsByStatus = new LinkedHashMap<>();
        if (complaints != null) {
            for (int row = 0; row < complaints.getRowCount(); row++) {
                String status = String.valueOf(complaints.getValueAt(row, 4));
                countsByStatus.merge(status, 1, Integer::sum);
            }
        }
        PieChartPanel chart = new PieChartPanel(countsByStatus);
        return wrapChartInCard("Complaints by status", chart);
    }

    private JComponent buildFeeTrendChartCard() {
        DefaultTableModel fees = tableModels.get("Fees");
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        if (fees != null) {
            for (int row = 0; row < fees.getRowCount(); row++) {
                labels.add(String.valueOf(fees.getValueAt(row, 0)));                        // Receipt
                values.add(parseAmountSafe(String.valueOf(fees.getValueAt(row, 2))));        // Amount
            }
        }
        LineChartPanel chart = new LineChartPanel(labels, values, COLOR_BLUE);
        JComponent card = wrapChartInCard("Fee collection trend", chart);
        card.setPreferredSize(new Dimension(0, 220));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        return card;
    }

    private JComponent buildRecentActivityCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COLOR_SURFACE);
        card.setBorder(new CompoundBorder(new LineBorder(COLOR_SURFACE_LIGHT, 1, true), new EmptyBorder(16, 18, 16, 18)));

        JLabel title = makeLabel("Recent activity", 15, COLOR_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        List<String> entries = auditLog.entries();
        if (entries.isEmpty()) {
            JLabel empty = makeLabel("No activity recorded yet.", 13, COLOR_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(empty);
        } else {
            // UNIT 1: only the most recent 5 entries are shown, using a plain for loop.
            int start = Math.max(0, entries.size() - 5);
            for (int i = entries.size() - 1; i >= start; i--) {
                JLabel entryLabel = makeLabel("- " + entries.get(i), 13, COLOR_MUTED);
                entryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(entryLabel);
            }
        }
        return card;
    }

    private JComponent wrapChartInCard(String title, JComponent chart) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(COLOR_SURFACE);
        card.setBorder(new CompoundBorder(new LineBorder(COLOR_SURFACE_LIGHT, 1, true), new EmptyBorder(14, 16, 14, 16)));
        JLabel titleLabel = makeLabel(title, 15, COLOR_TEXT);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    /** UNIT 1: simple exception handling around number parsing (a value might not be numeric). */
    private int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    private double parseAmountSafe(String text) {
        try {
            // Strip anything that is not a digit or a decimal point, e.g. "Rs." or ",".
            String digitsOnly = text.replaceAll("[^0-9.]", "");
            return digitsOnly.isEmpty() ? 0 : Double.parseDouble(digitsOnly);
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    private JComponent buildReportsPage() {
        JPanel page = new JPanel();
        page.setBackground(COLOR_BACKGROUND);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(10, 28, 25, 28));
        page.add(makeLabel("Export a module's data as a CSV report.", 14, COLOR_MUTED));

        for (String moduleName : moduleNames()) {
            JButton exportButton = createButton("Export " + moduleName, COLOR_SURFACE_LIGHT);
            exportButton.addActionListener(event -> exportModuleToCsv(moduleName, tableModels.get(moduleName)));
            page.add(exportButton);
            page.add(Box.createVerticalStrut(6));
        }
        return wrapInScrollPane(page);
    }

    private JComponent buildSettingsPage() {
        JPanel page = new JPanel();
        page.setBackground(COLOR_BACKGROUND);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(10, 28, 25, 28));

        JButton showConceptsButton = createButton("Show CSE2006 concepts used in this app", COLOR_ACCENT);
        showConceptsButton.addActionListener(event -> showSyllabusConceptReport());
        page.add(showConceptsButton);
        page.add(Box.createVerticalStrut(10));

        JButton backupButton = createButton("Backup activity log to file", COLOR_SURFACE_LIGHT);
        backupButton.addActionListener(event -> backupAuditLogToFile());
        page.add(backupButton);

        return wrapInScrollPane(page);
    }

    private JComponent buildAboutPage() {
        JPanel page = new JPanel();
        page.setBackground(COLOR_BACKGROUND);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(10, 28, 25, 28));
        page.add(makeLabel("Smart Hostel Management System", 20, COLOR_TEXT));
        page.add(makeLabel("A CSE2006 Programming in Java demonstration project.", 14, COLOR_MUTED));
        return wrapInScrollPane(page);
    }

    /** Column headings, the "Add" button label and the input fields used for one module. */
    private String[] columnsFor(String moduleName) {
        switch (moduleName) {
            case "Students": return new String[]{"Student ID", "Name", "Course", "Room", "Phone", "Status"};
            case "Rooms": return new String[]{"Room No.", "Block", "Capacity", "Occupied", "Available", "Status"};
            case "Fees": return new String[]{"Receipt", "Student", "Amount", "Payment Mode", "Date", "Status"};
            case "Complaints": return new String[]{"Ticket", "Student", "Subject", "Assigned To", "Status", "Date"};
            case "Visitors": return new String[]{"Pass No.", "Visitor", "Student / Room", "Entry Time", "Exit Time", "Status"};
            case "Attendance": return new String[]{"Student", "Room", "Date", "Entry Time", "Exit Time", "Attendance"};
            case "Mess & Meals": return new String[]{"Menu ID", "Meal", "Menu / Item", "Date", "Servings", "Status"};
            case "Leave & Outpass": return new String[]{"Request ID", "Student", "Leave From", "Return By", "Reason", "Status"};
            case "Inventory": return new String[]{"Asset ID", "Item", "Category", "Available", "Reorder Level", "Status"};
            case "Notice Board": return new String[]{"Notice ID", "Title", "Category", "Published On", "Author", "Status"};
            case "Staff": return new String[]{"Staff ID", "Name", "Designation", "Phone", "Shift", "Status"};
            default: return new String[]{"Column 1", "Column 2", "Column 3"};
        }
    }

    private String[] moduleNames() {
        // UNIT 4: PAGE_NAMES minus Dashboard/Reports/Settings/About, using a List and loop.
        List<String> names = new ArrayList<>();
        for (String pageName : PAGE_NAMES) {
            if (!pageName.equals("Dashboard") && !pageName.equals("Reports")
                    && !pageName.equals("Settings") && !pageName.equals("About")) {
                names.add(pageName);
            }
        }
        return names.toArray(new String[0]);
    }

    private JComponent buildEntityPage(String moduleName) {
        String[] columns = columnsFor(moduleName);
        DefaultTableModel model = new DefaultTableModel(sampleRowsFor(moduleName), columns);
        tableModels.put(moduleName, model);

        JTable table = new JTable(model);
        table.setFont(FONT_NORMAL);
        table.setRowHeight(32);
        table.setRowSorter(new TableRowSorter<>(model));

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(COLOR_BACKGROUND);
        page.setBorder(new EmptyBorder(10, 28, 25, 28));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        JButton addButton = createButton("Add " + singular(moduleName), COLOR_ACCENT);
        addButton.addActionListener(event -> showAddRecordDialog(moduleName, model));
        toolbar.add(addButton);

        JButton deleteButton = createButton("Delete selected", COLOR_SURFACE_LIGHT);
        deleteButton.addActionListener(event -> deleteSelectedRow(table, model, moduleName));
        toolbar.add(deleteButton);

        JButton exportButton = createButton("Export CSV", COLOR_SURFACE_LIGHT);
        exportButton.addActionListener(event -> exportModuleToCsv(moduleName, model));
        toolbar.add(exportButton);

        page.add(toolbar, BorderLayout.NORTH);
        page.add(new JScrollPane(table), BorderLayout.CENTER);
        return page;
    }

    private String singular(String moduleName) {
        // UNIT 1: a switch statement chooses the right label - control flow example.
        switch (moduleName) {
            case "Fees": return "Fee Payment";
            case "Complaints": return "Complaint";
            case "Mess & Meals": return "Meal Plan";
            case "Leave & Outpass": return "Outpass";
            default: return moduleName.endsWith("s") ? moduleName.substring(0, moduleName.length() - 1) : moduleName;
        }
    }

    private void showAddRecordDialog(String moduleName, DefaultTableModel model) {
        String[] columns = columnsFor(moduleName);
        JTextField[] fields = new JTextField[columns.length];

        JPanel form = new JPanel(new GridLayout(columns.length, 2, 6, 6));
        // UNIT 1: a basic for loop builds one labelled text field per column.
        for (int i = 0; i < columns.length; i++) {
            form.add(new JLabel(columns[i] + ":"));
            fields[i] = new JTextField();
            form.add(fields[i]);
        }

        int result = JOptionPane.showConfirmDialog(frame, form, "Add " + singular(moduleName),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String[] newRow = readAndValidateRow(fields);
            model.addRow(newRow);
            auditLog.add("Added a new record to " + moduleName);
            refreshDashboardContent();
        } catch (ValidationException validationError) {
            // UNIT 3: a custom checked exception is caught and reported to the user.
            showError(validationError.getMessage());
        }
    }

    /** UNIT 3: throws a custom checked exception when a field is left empty. */
    private String[] readAndValidateRow(JTextField[] fields) throws ValidationException {
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String text = fields[i].getText().trim();
            if (text.isEmpty()) {
                throw new ValidationException("Every field must be filled in before saving.");
            }
            values[i] = text;
        }
        return values;
    }

    private void deleteSelectedRow(JTable table, DefaultTableModel model, String moduleName) {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            showError("Select a row first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (confirmAction("Delete this " + singular(moduleName).toLowerCase() + " record?")) {
            model.removeRow(modelRow);
            auditLog.add("Deleted a record from " + moduleName);
            refreshDashboardContent();
        }
    }

    // =====================================================================================
    // UNIT 4: FILE I/O - writing table data out as a CSV file using character streams.
    // =====================================================================================

    private void exportModuleToCsv(String moduleName, DefaultTableModel model) {
        if (model == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(moduleName.replaceAll("\\s+", "_") + ".csv"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File targetFile = chooser.getSelectedFile();
        FileDataService fileService = new FileDataService();
        try {
            fileService.writeTableAsCsv(targetFile, model);
            auditLog.add(moduleName + " exported to CSV");
            showInfo("Exported " + moduleName.toLowerCase() + " data successfully.");
        } catch (IOException ioError) {
            showError("Could not write the file: " + ioError.getMessage());
        }
    }

    private void backupAuditLogToFile() {
        FileDataService fileService = new FileDataService();
        File logFile = new File("hostel_activity_log.txt");
        try {
            fileService.writeLinesToFile(logFile, auditLog.entries());
            showInfo("Activity log saved to " + logFile.getAbsolutePath());
        } catch (IOException ioError) {
            showError("Could not write the log file: " + ioError.getMessage());
        }
    }

    private void showSyllabusConceptReport() {
        JTextArea area = new JTextArea(SyllabusShowcase.buildReport());
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(new Dimension(680, 420));
        JOptionPane.showMessageDialog(frame, pane, "CSE2006 concepts used in this application",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // =====================================================================================
    // Sample data (used to pre-fill every module so the app is demo-ready).
    // =====================================================================================

    private void seedSampleData() {
        dataStore.students.add(new Student("ST-1001", "Aarav Sharma", "B.Tech CSE", "A-204"));
        dataStore.rooms.add(new Room("A-204", 4));
        dataStore.activities.add("System initialized");
    }

    private String[][] sampleRowsFor(String moduleName) {
        switch (moduleName) {
            case "Students":
                return new String[][]{
                        {"ST-1001", "Aarav Sharma", "B.Tech CSE", "A-204", "9876543210", "Active"},
                        {"ST-1002", "Kavya Singh", "BBA", "B-108", "9876543211", "Active"},
                        {"ST-1003", "Rahul Mehta", "B.Tech ECE", "A-112", "9876543212", "Active"},
                        {"ST-1004", "Neha Patel", "MBA", "C-305", "9876543213", "Fee Pending"},
                        {"ST-1005", "Arjun Nair", "BCA", "B-211", "9876543214", "Active"}
                };
            case "Rooms":
                return new String[][]{
                        {"A-201", "Block A", "4", "3", "1", "Available"},
                        {"A-202", "Block A", "4", "4", "0", "Full"},
                        {"B-108", "Block B", "3", "2", "1", "Available"},
                        {"C-305", "Block C", "2", "1", "1", "Available"},
                        {"D-102", "Block D", "4", "0", "4", "Maintenance"}
                };
            case "Fees":
                return new String[][]{
                        {"REC-0522", "Aarav Sharma", "18000", "UPI", "2026-09-05", "Paid"},
                        {"REC-0523", "Kavya Singh", "18500", "Card", "2026-09-04", "Paid"},
                        {"REC-0524", "Rahul Mehta", "18000", "Cash", "2026-09-03", "Paid"},
                        {"REC-0525", "Neha Patel", "9000", "-", "2026-09-01", "Pending"},
                        {"REC-0526", "Arjun Nair", "18000", "Bank Transfer", "2026-08-30", "Paid"}
                };
            case "Complaints":
                return new String[][]{
                        {"CMP-104", "Aarav Sharma", "Fan not working", "Maintenance", "In Progress", "2026-09-05"},
                        {"CMP-105", "Kavya Singh", "Water leakage", "Plumbing", "Open", "2026-09-05"},
                        {"CMP-106", "Rahul Mehta", "Wi-Fi issue", "IT Support", "Open", "2026-09-04"},
                        {"CMP-107", "Neha Patel", "Room cleaning", "Housekeeping", "Resolved", "2026-09-03"},
                        {"CMP-108", "Arjun Nair", "Broken lock", "Maintenance", "Open", "2026-09-02"}
                };
            case "Visitors":
                return new String[][]{
                        {"V-209", "Anita Verma", "Aarav Sharma / A-204", "10:12 AM", "-", "Inside"},
                        {"V-210", "Rohit Sharma", "Kavya Singh / B-108", "11:05 AM", "12:10 PM", "Exited"},
                        {"V-211", "Meera Gupta", "Rahul Mehta / A-112", "12:20 PM", "-", "Inside"}
                };
            case "Attendance":
                return new String[][]{
                        {"Aarav Sharma", "A-204", "2026-09-05", "07:48 AM", "09:10 PM", "Present"},
                        {"Kavya Singh", "B-108", "2026-09-05", "08:10 AM", "08:40 PM", "Late Entry"},
                        {"Rahul Mehta", "A-112", "2026-09-05", "07:30 AM", "10:00 PM", "Present"},
                        {"Neha Patel", "C-305", "2026-09-05", "-", "-", "Absent"},
                        {"Arjun Nair", "B-211", "2026-09-05", "08:02 AM", "09:15 PM", "Present"}
                };
            case "Mess & Meals":
                return new String[][]{
                        {"MENU-01", "Breakfast", "Poha, fruit & tea", "2026-09-05", "220", "Published"},
                        {"MENU-02", "Lunch", "Dal, rice & paneer", "2026-09-05", "235", "Published"},
                        {"MENU-03", "Dinner", "Roti & mixed vegetables", "2026-09-05", "230", "Published"}
                };
            case "Leave & Outpass":
                return new String[][]{
                        {"OUT-041", "Aarav Sharma", "2026-09-06", "2026-09-07", "Family function", "Approved"},
                        {"OUT-042", "Kavya Singh", "2026-09-05", "2026-09-05", "Medical appointment", "Pending"},
                        {"OUT-043", "Rahul Mehta", "2026-09-07", "2026-09-08", "Home visit", "Approved"}
                };
            case "Inventory":
                return new String[][]{
                        {"INV-101", "Mattress", "Room Furniture", "32", "10", "In Stock"},
                        {"INV-102", "Study Table", "Room Furniture", "8", "10", "Reorder"},
                        {"INV-103", "Water Dispenser", "Appliance", "5", "3", "In Stock"}
                };
            case "Notice Board":
                return new String[][]{
                        {"NT-201", "Maintenance Schedule", "General", "2026-09-05", "Warden", "Active"},
                        {"NT-202", "Mess Menu Update", "Mess", "2026-09-04", "Mess Manager", "Active"},
                        {"NT-203", "Hostel Rules Reminder", "Discipline", "2026-09-03", "Admin", "Active"}
                };
            case "Staff":
                return new String[][]{
                        {"SF-001", "Ramesh Kumar", "Warden", "9876500001", "Day", "Active"},
                        {"SF-002", "Sunita Devi", "Assistant Warden", "9876500002", "Evening", "Active"},
                        {"SF-003", "Vijay Singh", "Security Guard", "9876500003", "Night", "Active"}
                };
            default:
                return new String[][]{};
        }
    }

    // =====================================================================================
    // Small reusable Swing helper methods (keeps the layout code above easy to read).
    // =====================================================================================

    private JLabel makeLabel(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(size >= 16 ? new Font("Segoe UI Semibold", Font.PLAIN, size) : FONT_NORMAL);
        label.setForeground(color);
        return label;
    }

    // A compact, fixed size for the login fields so they don't stretch to fill the panel.
    private static final Dimension LOGIN_FIELD_SIZE = new Dimension(300, 36);

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(FONT_NORMAL);
        field.setForeground(COLOR_TEXT);
        field.setBackground(COLOR_SURFACE_LIGHT);
        field.setCaretColor(COLOR_TEXT);
        field.setBorder(new CompoundBorder(new LineBorder(COLOR_BORDER, 1, true), new EmptyBorder(6, 11, 6, 11)));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(LOGIN_FIELD_SIZE);
        field.setMaximumSize(LOGIN_FIELD_SIZE);
        field.setMinimumSize(LOGIN_FIELD_SIZE);
        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT_NORMAL);
        field.setForeground(COLOR_TEXT);
        field.setBackground(COLOR_SURFACE_LIGHT);
        field.setCaretColor(COLOR_TEXT);
        field.setBorder(new CompoundBorder(new LineBorder(COLOR_BORDER, 1, true), new EmptyBorder(6, 11, 6, 11)));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(LOGIN_FIELD_SIZE);
        field.setMaximumSize(LOGIN_FIELD_SIZE);
        field.setMinimumSize(LOGIN_FIELD_SIZE);
        return field;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setForeground(color.equals(COLOR_ACCENT) ? COLOR_BACKGROUND : COLOR_TEXT);
        button.setBackground(color);
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JComponent wrapInScrollPane(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        return scrollPane;
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(frame, message, "Smart Hostel", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    private boolean confirmAction(String message) {
        return JOptionPane.showConfirmDialog(frame, message, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    // =====================================================================================
    // UNIT 2: OOP - DOMAIN MODEL
    // Demonstrates encapsulation, inheritance, interfaces, abstract classes and
    // polymorphism (method overriding).
    // =====================================================================================

    /** An interface: any class that can report a unique identifier. */
    interface Identifiable {
        String id();
    }

    /**
     * An abstract class - it cannot be instantiated directly and defines an abstract
     * method that every subclass must implement (polymorphism through overriding).
     */
    abstract static class Person implements Identifiable {
        private final String id;
        private final String name;

        Person(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        /** Each subclass provides its own version of this method. */
        public abstract String roleLabel();
    }

    /** UNIT 2: inheritance - Student extends Person and reuses id()/name(). */
    @SyllabusConcept("Inheritance, constructors, method overriding and encapsulation")
    static class Student extends Person {
        private String course;
        private String room;

        Student(String id, String name, String course, String room) {
            super(id, name);
            this.course = course;
            this.room = room;
        }

        public String room() {
            return room;
        }

        public void assignRoom(String room) {
            this.room = room;
        }

        @Override
        public String roleLabel() {
            return "Student";
        }

        @Override
        public String toString() {
            return name() + " (" + course + ")";
        }
    }

    /** Another subclass of Person - shows that one abstract class can have many children. */
    static class Staff extends Person {
        private final String designation;

        Staff(String id, String name, String designation) {
            super(id, name);
            this.designation = designation;
        }

        @Override
        public String roleLabel() {
            return designation;
        }
    }

    static class Room implements Identifiable {
        final String number;
        final int capacity;
        int occupied;

        Room(String number, int capacity) {
            this.number = number;
            this.capacity = capacity;
        }

        public String id() {
            return number;
        }

        int availableBeds() {
            return capacity - occupied;
        }
    }

    static class Payment implements Identifiable {
        final String receiptNumber;
        final double amount;

        Payment(String receiptNumber, double amount) {
            this.receiptNumber = receiptNumber;
            this.amount = amount;
        }

        public String id() {
            return receiptNumber;
        }
    }

    static class Complaint implements Identifiable {
        final String ticketNumber;
        String status = "Open";

        Complaint(String ticketNumber) {
            this.ticketNumber = ticketNumber;
        }

        public String id() {
            return ticketNumber;
        }
    }

    static class Visitor implements Identifiable {
        final String passNumber;
        final LocalDateTime entryTime = LocalDateTime.now();

        Visitor(String passNumber) {
            this.passNumber = passNumber;
        }

        public String id() {
            return passNumber;
        }
    }

    static class Notice implements Identifiable {
        final String noticeId;
        final String title;

        Notice(String noticeId, String title) {
            this.noticeId = noticeId;
            this.title = title;
        }

        public String id() {
            return noticeId;
        }
    }

    static class AttendanceRecord {
        final String studentName;
        final LocalDate date = LocalDate.now();

        AttendanceRecord(String studentName) {
            this.studentName = studentName;
        }
    }

    /** UNIT 4: the Collections framework - every module's records live in a List. */
    static class DataStore {
        final List<Student> students = new ArrayList<>();
        final List<Room> rooms = new ArrayList<>();
        final List<Payment> payments = new ArrayList<>();
        final List<Complaint> complaints = new ArrayList<>();
        final List<Visitor> visitors = new ArrayList<>();
        final List<String> activities = new ArrayList<>();
    }

    /** A custom annotation - used purely to document which syllabus topics a class covers. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface SyllabusConcept {
        String value();
    }

    /** UNIT 3: a custom checked exception used by the add-record form validation. */
    static class ValidationException extends Exception {
        ValidationException(String message) {
            super(message);
        }
    }

    enum RecordState {
        ACTIVE, PENDING, APPROVED, RESOLVED, ARCHIVED
    }

    /** UNIT 2: a generic interface - works with any type that implements Identifiable. */
    interface Repository<T extends Identifiable> {
        void save(T item);

        Optional<T> findById(String id);

        List<T> findAll();

        void delete(String id);
    }

    /** A generic class implementing the generic interface above, backed by a Map. */
    static class MemoryRepository<T extends Identifiable> implements Repository<T> {
        private final Map<String, T> storage = new LinkedHashMap<>();

        public void save(T item) {
            storage.put(item.id(), item);
        }

        public Optional<T> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        public List<T> findAll() {
            return new ArrayList<>(storage.values());
        }

        public void delete(String id) {
            storage.remove(id);
        }
    }

    // =====================================================================================
    // UNIT 3: MULTITHREADING
    // A singleton audit log with synchronized methods, and a background Thread that
    // refreshes the dashboard clock without freezing the Swing UI thread.
    // =====================================================================================

    /** Singleton pattern: only one ApplicationLog instance ever exists. */
    static class ApplicationLog {
        private static final ApplicationLog INSTANCE = new ApplicationLog();
        private final List<String> entries = new Vector<>(); // thread-safe list

        private ApplicationLog() {
        }

        static ApplicationLog getInstance() {
            return INSTANCE;
        }

        synchronized void add(String message) {
            entries.add(LocalDateTime.now() + " - " + message);
        }

        synchronized List<String> entries() {
            return new ArrayList<>(entries);
        }
    }

    /** Demonstrates the thread life cycle: created, started, running, then interrupted. */
    static class DashboardClock extends Thread {
        private final JLabel targetLabel;

        DashboardClock(JLabel targetLabel) {
            this.targetLabel = targetLabel;
            setDaemon(true);
            setName("hostel-dashboard-clock");
        }

        public void run() {
            while (!isInterrupted()) {
                String time = DateTimeFormatter.ofPattern("hh:mm:ss a").format(LocalTime.now());
                SwingUtilities.invokeLater(() -> targetLabel.setToolTipText("System time: " + time));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interrupted) {
                    interrupt();
                }
            }
        }
    }

    // =====================================================================================
    // UNIT 5: JDBC
    // A thin wrapper around java.sql that shows how a real MySQL/PostgreSQL connection
    // would be used. The rest of the app runs in demo mode with in-memory sample data,
    // so this class is not required for the app to run, but shows the required API.
    // =====================================================================================

    static class DatabaseManager {

        Connection connect(String url, String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        List<Map<String, Object>> runQuery(Connection connection, String sql) throws SQLException {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(sql)) {
                ResultSetMetaData metaData = result.getMetaData();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        row.put(metaData.getColumnLabel(i), result.getObject(i));
                    }
                    rows.add(row);
                }
            }
            return rows;
        }

        int runUpdate(Connection connection, String sql) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                return statement.executeUpdate(sql);
            }
        }

        void close(Connection connection) {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
                // Nothing more we can do if closing fails.
            }
        }
    }

    // =====================================================================================
    // UNIT 4: FILE I/O STREAMS
    // Shows both byte streams (copying a file) and character streams (reading/writing text).
    // =====================================================================================

    static class FileDataService {

        void copyFile(File source, File destination) throws IOException {
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }

        String readTextFile(File source) throws IOException {
            StringBuilder text = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(source))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line).append(System.lineSeparator());
                }
            }
            return text.toString();
        }

        void writeLinesToFile(File destination, List<String> lines) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }

        void writeTableAsCsv(File destination, DefaultTableModel model) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) writer.write(",");
                    writer.write(csvEscape(model.getColumnName(c)));
                }
                writer.newLine();
                for (int r = 0; r < model.getRowCount(); r++) {
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        if (c > 0) writer.write(",");
                        writer.write(csvEscape(String.valueOf(model.getValueAt(r, c))));
                    }
                    writer.newLine();
                }
            }
        }

        private String csvEscape(String value) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
    }

    // =====================================================================================
    // A small demonstration class that exercises recursion, arrays, collections and
    // reflection in one place, and prints a short report shown from the Settings page.
    // =====================================================================================

    @SyllabusConcept("Control flow, arrays, collections, recursion and reflection")
    static class SyllabusShowcase {

        /** UNIT 1: recursion - a function that calls itself. */
        static int factorial(int n) {
            if (n <= 1) {
                return 1;
            }
            return n * factorial(n - 1);
        }

        static String buildReport() {
            // UNIT 4: a 1-D array and a jagged 2-D array.
            int[] occupancyPerFloor = {3, 4, 2, 1};
            String[][] roomsByBlock = {
                    {"A-201", "A-202"},
                    {"B-108"},
                    {"C-305", "C-306", "C-307"}
            };

            // UNIT 4: Stack and Vector from the Collections framework.
            Stack<String> undoStack = new Stack<>();
            undoStack.push("Add student");
            Vector<String> safeList = new Vector<>();
            safeList.add("Audit entry");

            // UNIT 2: a Person reference pointing at a Student - runtime polymorphism.
            Person person = new Student("DEMO-1", "Demo Student", "BCA", "A-201");

            String reflectionResult;
            try {
                reflectionResult = person.getClass().getMethod("roleLabel").invoke(person).toString();
            } catch (ReflectiveOperationException reflectionError) {
                reflectionResult = "Reflection unavailable";
            }

            StringBuilder report = new StringBuilder();
            report.append("CSE2006 Programming in Java - concepts used in this application\n\n");
            report.append("Recursion: factorial(5) = ").append(factorial(5)).append("\n");
            report.append("Arrays: floors tracked = ").append(occupancyPerFloor.length);
            report.append(", jagged blocks = ").append(roomsByBlock.length).append("\n");
            report.append("Collections: Stack top = ").append(undoStack.peek());
            report.append(", Vector size = ").append(safeList.size()).append("\n");
            report.append("Polymorphism: Person reference calls Student.roleLabel() = ").append(person.roleLabel()).append("\n");
            report.append("Reflection: invoked method result = ").append(reflectionResult).append("\n");
            report.append("Interfaces: Identifiable and Repository<T> are implemented in this file\n");
            report.append("Exceptions: ValidationException, SQLException and IOException are handled\n");
            report.append("Multithreading: DashboardClock extends Thread; ApplicationLog uses synchronized methods\n");
            report.append("I/O streams: FileDataService reads and writes files with byte and character streams\n");
            report.append("JDBC: DatabaseManager wraps connect/query/update/close using java.sql\n");
            return report.toString();
        }
    }

    // =====================================================================================
    // DASHBOARD CHART COMPONENTS
    // Each chart is a small custom Swing component (extends JComponent, overrides
    // paintComponent) - the same idea as drawing shapes on a canvas, just applied to
    // real hostel data. No external chart library is used, only java.awt.Graphics2D.
    // =====================================================================================

    /** A simple vertical bar chart: one bar per label/value pair. */
    static class BarChartPanel extends JComponent {
        private final List<String> labels;
        private final List<Double> values;
        private final Color barColor;

        BarChartPanel(List<String> labels, List<Double> values, Color barColor) {
            this.labels = labels;
            this.values = values;
            this.barColor = barColor;
            setPreferredSize(new Dimension(320, 170));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (labels.isEmpty()) {
                g2.setColor(COLOR_MUTED);
                g2.drawString("No data yet", 10, getHeight() / 2);
                g2.dispose();
                return;
            }

            int width = getWidth();
            int height = getHeight();
            int bottomMargin = 22;
            int chartHeight = height - bottomMargin;

            // UNIT 1: a for loop finds the largest value so bars can be scaled to fit.
            double maxValue = 1;
            for (double value : values) {
                if (value > maxValue) {
                    maxValue = value;
                }
            }

            int barCount = labels.size();
            int gap = 14;
            int barWidth = Math.max(18, (width - gap * (barCount + 1)) / barCount);

            for (int i = 0; i < barCount; i++) {
                double value = values.get(i);
                int barHeight = (int) Math.round((value / maxValue) * (chartHeight - 20));
                int x = gap + i * (barWidth + gap);
                int y = chartHeight - barHeight;

                g2.setColor(barColor);
                g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);

                g2.setColor(COLOR_TEXT);
                String valueText = String.valueOf((int) Math.round(value));
                g2.drawString(valueText, x + barWidth / 2 - 6, y - 4);

                g2.setColor(COLOR_MUTED);
                g2.drawString(labels.get(i), x, height - 6);
            }
            g2.dispose();
        }
    }

    /** A simple line chart connecting one point per value, for showing a trend. */
    static class LineChartPanel extends JComponent {
        private final List<String> labels;
        private final List<Double> values;
        private final Color lineColor;

        LineChartPanel(List<String> labels, List<Double> values, Color lineColor) {
            this.labels = labels;
            this.values = values;
            this.lineColor = lineColor;
            setPreferredSize(new Dimension(320, 150));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (values.size() < 2) {
                g2.setColor(COLOR_MUTED);
                g2.drawString("Not enough data for a trend line yet", 10, getHeight() / 2);
                g2.dispose();
                return;
            }

            int width = getWidth();
            int height = getHeight();
            int bottomMargin = 20;
            int topMargin = 15;
            int chartHeight = height - bottomMargin - topMargin;

            double maxValue = 1;
            double minValue = 0;
            for (double value : values) {
                if (value > maxValue) {
                    maxValue = value;
                }
            }
            double range = Math.max(1, maxValue - minValue);

            int pointCount = values.size();
            int stepX = (width - 20) / (pointCount - 1);

            int[] xPoints = new int[pointCount];
            int[] yPoints = new int[pointCount];
            for (int i = 0; i < pointCount; i++) {
                xPoints[i] = 10 + i * stepX;
                double normalised = (values.get(i) - minValue) / range;
                yPoints[i] = topMargin + (int) Math.round((1 - normalised) * chartHeight);
            }

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < pointCount - 1; i++) {
                g2.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }
            for (int i = 0; i < pointCount; i++) {
                g2.fillOval(xPoints[i] - 3, yPoints[i] - 3, 6, 6);
            }

            g2.setColor(COLOR_MUTED);
            for (int i = 0; i < pointCount; i++) {
                g2.drawString(labels.get(i), xPoints[i] - 10, height - 4);
            }
            g2.dispose();
        }
    }

    /** A simple pie chart with a coloured legend, built from a category -> count map. */
    static class PieChartPanel extends JComponent {
        private static final Color[] SLICE_COLORS = {
                COLOR_ACCENT, COLOR_BLUE, COLOR_WARNING, COLOR_DANGER, COLOR_MUTED
        };
        private final Map<String, Integer> segments;

        PieChartPanel(Map<String, Integer> segments) {
            this.segments = segments;
            setPreferredSize(new Dimension(320, 170));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (segments.isEmpty()) {
                g2.setColor(COLOR_MUTED);
                g2.drawString("No data yet", 10, getHeight() / 2);
                g2.dispose();
                return;
            }

            // UNIT 1: a for loop totals up every count so each slice's share can be worked out.
            int total = 0;
            for (int count : segments.values()) {
                total += count;
            }

            int diameter = Math.min(getHeight() - 10, 140);
            int pieX = 10;
            int pieY = (getHeight() - diameter) / 2;

            double startAngle = 90;
            int colorIndex = 0;
            int legendY = 14;
            int legendX = pieX + diameter + 24;

            for (Map.Entry<String, Integer> entry : segments.entrySet()) {
                double share = total == 0 ? 0 : (entry.getValue() / (double) total);
                double sweepAngle = share * 360;
                Color sliceColor = SLICE_COLORS[colorIndex % SLICE_COLORS.length];

                g2.setColor(sliceColor);
                g2.fill(new Arc2D.Double(pieX, pieY, diameter, diameter, startAngle, -sweepAngle, Arc2D.PIE));
                startAngle -= sweepAngle;

                g2.setColor(sliceColor);
                g2.fillRect(legendX, legendY, 10, 10);
                g2.setColor(COLOR_TEXT);
                g2.drawString(entry.getKey() + " (" + entry.getValue() + ")", legendX + 16, legendY + 10);
                legendY += 20;
                colorIndex++;
            }
            g2.dispose();
        }
    }
}
