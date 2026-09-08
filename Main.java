import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
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

        // UNIT 1: a simple for-each loop builds one card-layout page per module.
        for (String pageName : PAGE_NAMES) {
            contentPanel.add(buildPage(pageName), pageName);
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

    private JComponent buildPage(String pageName) {
        switch (pageName) {
            case "Dashboard":
                return buildDashboardPage();
            case "Reports":
                return buildReportsPage();
            case "Settings":
                return buildSettingsPage();
            case "About":
                return buildAboutPage();
            default:
                return buildEntityPage(pageName);
        }
    }

    private JComponent buildDashboardPage() {
        JPanel page = new JPanel();
        page.setBackground(COLOR_BACKGROUND);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(10, 28, 25, 28));

        page.add(makeLabel("Students: " + dataStore.students.size(), 16, COLOR_TEXT));
        page.add(makeLabel("Rooms: " + dataStore.rooms.size(), 16, COLOR_TEXT));
        page.add(makeLabel("Recent activity:", 14, COLOR_MUTED));
        for (String activity : dataStore.activities) {
            page.add(makeLabel("- " + activity, 13, COLOR_MUTED));
        }
        return wrapInScrollPane(page);
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
                        {"ST-1003", "Rahul Mehta", "B.Tech ECE", "A-112", "9876543212", "Active"}
                };
            case "Rooms":
                return new String[][]{
                        {"A-201", "Block A", "4", "3", "1", "Available"},
                        {"A-202", "Block A", "4", "4", "0", "Full"},
                        {"B-108", "Block B", "3", "2", "1", "Available"}
                };
            case "Fees":
                return new String[][]{
                        {"REC-0522", "Aarav Sharma", "18000", "UPI", "2026-09-05", "Paid"},
                        {"REC-0523", "Kavya Singh", "18500", "Card", "2026-09-04", "Paid"}
                };
            case "Complaints":
                return new String[][]{
                        {"CMP-104", "Aarav Sharma", "Fan not working", "Maintenance", "In Progress", "2026-09-05"},
                        {"CMP-105", "Kavya Singh", "Water leakage", "Plumbing", "Open", "2026-09-05"}
                };
            case "Visitors":
                return new String[][]{
                        {"V-209", "Anita Verma", "Aarav Sharma / A-204", "10:12 AM", "-", "Inside"}
                };
            case "Attendance":
                return new String[][]{
                        {"Aarav Sharma", "A-204", "2026-09-05", "07:48 AM", "09:10 PM", "Present"}
                };
            case "Mess & Meals":
                return new String[][]{
                        {"MENU-01", "Breakfast", "Poha, fruit & tea", "2026-09-05", "220", "Published"}
                };
            case "Leave & Outpass":
                return new String[][]{
                        {"OUT-041", "Aarav Sharma", "2026-09-06", "2026-09-07", "Family function", "Approved"}
                };
            case "Inventory":
                return new String[][]{
                        {"INV-101", "Mattress", "Room Furniture", "32", "10", "In Stock"}
                };
            case "Notice Board":
                return new String[][]{
                        {"NT-201", "Maintenance Schedule", "General", "2026-09-05", "Warden", "Active"}
                };
            case "Staff":
                return new String[][]{
                        {"SF-001", "Ramesh Kumar", "Warden", "9876500001", "Day", "Active"}
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
}
