import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.lang.annotation.*;
import java.sql.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

/**
 * Smart Hostel Management System - a single-file Java Swing desktop application.
 * JDBC settings are kept under Settings; the UI works in demonstration mode until
 * a MySQL server is configured. Run: javac Main.java && java Main
 */
public class Main {
    static final Color BG = new Color(15, 23, 42), NAV = new Color(18, 30, 52), SURFACE = new Color(29, 42, 66);
    static final Color SURFACE_2 = new Color(37, 52, 78), ACCENT = new Color(39, 201, 160), BLUE = new Color(76, 139, 245);
    static final Color TEXT = new Color(235, 241, 250), MUTED = new Color(154, 173, 201), DANGER = new Color(239, 92, 105), WARNING = new Color(244, 178, 62);
    static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14), BOLD = new Font("Segoe UI Semibold", Font.PLAIN, 14);
    private final JFrame frame = new JFrame("Smart Hostel Management System");
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel subtitle = new JLabel("Overview of your hostel today");
    private final DataStore store = new DataStore();
    // Each table has a live model: add, edit and delete affect the displayed records immediately.
    private final Map<String, DefaultTableModel> moduleModels = new HashMap<>();
    private final ApplicationLog auditLog = ApplicationLog.getInstance();
    private boolean clockStarted;
    private String active = "Dashboard";

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new Main().start()); }

    private void start() {
        installTheme(); seedData(); showLogin();
    }
    private void installTheme() {
        try { Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null); }
        catch (Exception ignored) { UIManager.put("Panel.background", BG); UIManager.put("OptionPane.background", SURFACE); UIManager.put("OptionPane.messageForeground", TEXT); }
    }
    private void showLogin() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setSize(1120, 720); frame.setMinimumSize(new Dimension(900, 620));
        frame.setLocationRelativeTo(null); frame.setContentPane(loginScreen()); frame.setVisible(true);
    }
    private JPanel loginScreen() {
        JPanel root = panel(new BorderLayout()); root.setBorder(new EmptyBorder(45, 55, 45, 55));
        JPanel brand = panel(new GridBagLayout()); brand.setBackground(NAV);
        GridBagConstraints g = new GridBagConstraints(); g.gridx=0; g.anchor=GridBagConstraints.WEST; g.insets=new Insets(8, 30, 8, 30);
        JLabel mark = new JLabel(new LineIcon("H", ACCENT, 58)); mark.setPreferredSize(new Dimension(70,70)); g.gridy=0; brand.add(mark,g);
        JLabel title = label("SMART HOSTEL", 30, TEXT); g.gridy=1; brand.add(title,g);
        JLabel tag = label("Management that feels effortless.", 16, MUTED); g.gridy=2; brand.add(tag,g);
        JLabel copy = label("Student living, rooms, payments and operations — all in one secure workspace.", 14, MUTED); g.gridy=3; brand.add(copy,g);
        root.add(brand, BorderLayout.CENTER);
        JPanel form = panel(new GridBagLayout()); form.setPreferredSize(new Dimension(390,0)); form.setBorder(new EmptyBorder(35,40,35,40));
        GridBagConstraints f=new GridBagConstraints(); f.gridx=0; f.weightx=1; f.fill=GridBagConstraints.HORIZONTAL; f.insets=new Insets(7,0,7,0);
        f.gridy=0; form.add(label("Welcome back", 26, TEXT),f); f.gridy++; form.add(label("Sign in to your administrator account",14,MUTED),f);
        f.gridy++; f.insets=new Insets(35,0,3,0); form.add(label("EMAIL ADDRESS",11,MUTED),f); JTextField email=input("admin@hostel.com"); f.gridy++; f.insets=new Insets(3,0,10,0); form.add(email,f);
        f.gridy++; f.insets=new Insets(6,0,3,0); form.add(label("PASSWORD",11,MUTED),f); JPasswordField pass=password(); f.gridy++; f.insets=new Insets(3,0,18,0); form.add(pass,f);
        JButton sign=button("Sign in to dashboard", ACCENT); sign.addActionListener(e->{ if(email.getText().trim().isEmpty()||pass.getPassword().length==0) error("Enter your email and password."); else buildApp(); }); f.gridy++; form.add(sign,f);
        f.gridy++; f.insets=new Insets(20,0,0,0); form.add(label("Demo credentials: admin@hostel.com / admin",12,MUTED),f);
        root.add(form,BorderLayout.EAST); return root;
    }
    private void buildApp() {
        JPanel root=panel(new BorderLayout()); root.add(sidebar(),BorderLayout.WEST); root.add(topbar(),BorderLayout.NORTH); content.setBackground(BG);
        String[] pages={"Dashboard","Students","Rooms","Fees","Complaints","Visitors","Attendance","Mess & Meals","Leave & Outpass","Inventory","Notice Board","Staff","Reports","Settings","About"};
        for(String p:pages) content.add(createPage(p),p); root.add(content,BorderLayout.CENTER); frame.setContentPane(root); showPage("Dashboard"); frame.revalidate(); if(!clockStarted){new DashboardClock(subtitle).start();clockStarted=true;}
    }
    private JComponent sidebar() {
        JPanel side=panel(new BorderLayout());side.setPreferredSize(new Dimension(245,0));side.setBackground(NAV);
        JPanel logo=panel(new FlowLayout(FlowLayout.LEFT,10,0));logo.setBackground(NAV);logo.setBorder(new EmptyBorder(18,14,18,14));logo.add(new JLabel(new LineIcon("H",ACCENT,31))); logo.add(label("SMART HOSTEL",17,TEXT));side.add(logo,BorderLayout.NORTH);
        JPanel nav=panel(); nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS)); nav.setBackground(NAV); nav.setBorder(new EmptyBorder(6,14,18,14));
        String[][] items={{"Dashboard","D"},{"Students","S"},{"Rooms","R"},{"Fees","F"},{"Complaints","C"},{"Visitors","V"},{"Attendance","A"},{"Mess & Meals","M"},{"Leave & Outpass","L"},{"Inventory","I"},{"Notice Board","N"},{"Staff","T"},{"Reports","P"}};
        for(String[] x:items) nav.add(navButton(x[0],x[1]));
        nav.add(Box.createVerticalStrut(10)); nav.add(navButton("Settings","G")); nav.add(navButton("About","i"));
        nav.setPreferredSize(new Dimension(245,720));
        JScrollPane scroll=new JScrollPane(nav);scroll.setBorder(null);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);scroll.getVerticalScrollBar().setUnitIncrement(24);scroll.getVerticalScrollBar().setBlockIncrement(90);scroll.getViewport().setBackground(NAV);side.add(scroll,BorderLayout.CENTER);return side;
    }
    private JButton navButton(String name,String glyph) {
        JButton b=new JButton(name,new LineIcon(glyph,MUTED,19)); b.setHorizontalAlignment(SwingConstants.LEFT); b.setIconTextGap(13); b.setMaximumSize(new Dimension(220,42)); b.setFont(BOLD); b.setForeground(MUTED); b.setBackground(NAV); b.setBorder(new EmptyBorder(8,12,8,12)); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e->showPage(name)); return b;
    }
    private JComponent topbar() {
        JPanel top=panel(new BorderLayout()); top.setBackground(BG); top.setBorder(new EmptyBorder(18,28,14,28));
        JPanel left=panel(); left.setOpaque(false); left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS)); pageTitle.setFont(new Font("Segoe UI Semibold",Font.PLAIN,25)); pageTitle.setForeground(TEXT); subtitle.setFont(FONT); subtitle.setForeground(MUTED); left.add(pageTitle); left.add(subtitle); top.add(left,BorderLayout.WEST);
        JPanel user=panel(new FlowLayout(FlowLayout.RIGHT,12,4)); user.setOpaque(false); JLabel avatar=new JLabel("AD",SwingConstants.CENTER); avatar.setOpaque(true); avatar.setPreferredSize(new Dimension(34,34)); avatar.setBackground(ACCENT); avatar.setForeground(BG); avatar.setFont(BOLD); avatar.setBorder(new LineBorder(ACCENT,1,true)); user.add(avatar); user.add(label("Admin User",14,TEXT)); top.add(user,BorderLayout.EAST); return top;
    }
    private JComponent createPage(String name) {
        switch(name) { case "Dashboard": return dashboard(); case "Students": return entityPage(name,new String[]{"Student ID","Name","Course","Room","Phone","Status"},"Add Student",new String[]{"Name","Course","Phone","Room"});
            case "Rooms": return entityPage(name,new String[]{"Room No.","Block","Capacity","Occupied","Available","Status"},"Add Room",new String[]{"Room Number","Block","Capacity"});
            case "Fees": return entityPage(name,new String[]{"Receipt","Student","Amount","Payment Mode","Date","Status"},"Collect Fee",new String[]{"Student","Amount","Payment Mode"});
            case "Complaints": return entityPage(name,new String[]{"Ticket","Student","Subject","Assigned To","Status","Date"},"Register Complaint",new String[]{"Student","Subject","Assigned To"});
            case "Visitors": return entityPage(name,new String[]{"Pass No.","Visitor","Student / Room","Entry Time","Exit Time","Status"},"Visitor Entry",new String[]{"Visitor Name","Student / Room","Phone"});
            case "Attendance": return entityPage(name,new String[]{"Student","Room","Date","Entry Time","Exit Time","Attendance"},"Mark Attendance",new String[]{"Student","Room","Entry Time"});
            case "Mess & Meals": return entityPage(name,new String[]{"Menu ID","Meal","Menu / Item","Date","Servings","Status"},"Create Meal Plan",new String[]{"Meal","Menu Item","Servings"});
            case "Leave & Outpass": return entityPage(name,new String[]{"Request ID","Student","Leave From","Return By","Reason","Status"},"Create Outpass",new String[]{"Student","Leave From","Return By","Reason"});
            case "Inventory": return entityPage(name,new String[]{"Asset ID","Item","Category","Available","Reorder Level","Status"},"Add Inventory Item",new String[]{"Item Name","Category","Quantity","Reorder Level"});
            case "Notice Board": return entityPage(name,new String[]{"Notice ID","Title","Category","Published On","Author","Status"},"Create Notice",new String[]{"Title","Category","Description"});
            case "Staff": return entityPage(name,new String[]{"Staff ID","Name","Designation","Phone","Shift","Status"},"Add Staff",new String[]{"Name","Designation","Phone"});
            case "Reports": return reports(); case "Settings": return settings(); default:return about(); }
    }
    private JComponent dashboard() {
        JPanel p=scrollPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.add(stats()); p.add(Box.createVerticalStrut(16)); p.add(quickActions()); p.add(Box.createVerticalStrut(20));
        JPanel mid=panel(new GridLayout(1,2,18,0)); mid.setMaximumSize(new Dimension(1400,245)); mid.add(chartCard("Fee Collection",new int[]{45,66,52,86,72,98,82})); mid.add(chartCard("Occupancy Overview",new int[]{35,43,56,62,70,68,76})); p.add(mid); p.add(Box.createVerticalStrut(20));
        JPanel bottom=panel(new GridLayout(1,2,18,0)); bottom.setMaximumSize(new Dimension(1400,260)); bottom.add(activities()); bottom.add(todayVisitors()); p.add(bottom); return wrapScroll(p);
    }
    private JComponent stats(){ JPanel all=panel(new GridLayout(1,5,14,0)); all.setMaximumSize(new Dimension(1400,110)); String[][] s={{"Total Students","248"},{"Available Rooms","18"},{"Occupied Rooms","62"},{"Pending Complaints","07"},{"Today's Visitors","14"}}; for(String[] metric:s) all.add(metric(metric[0],metric[1])); return all; }
    private JComponent quickActions(){ JPanel c=card(new BorderLayout());c.setMaximumSize(new Dimension(1400,74));c.add(label("Quick actions",14,TEXT),BorderLayout.WEST);JPanel a=panel(new FlowLayout(FlowLayout.RIGHT,8,0));a.setOpaque(false);String[] actions={"New Student","Collect Fee","Visitor Entry","Create Outpass"};for(String x:actions){JButton b=button(x,SURFACE_2);b.addActionListener(e->{String cmd=((JButton)e.getSource()).getText();if(cmd.equals("New Student"))showPage("Students");else if(cmd.equals("Collect Fee"))showPage("Fees");else if(cmd.equals("Visitor Entry"))showPage("Visitors");else showPage("Leave & Outpass");});a.add(b);}c.add(a,BorderLayout.EAST);return c; }
    private JComponent metric(String t,String n){ JPanel x=card(new BorderLayout()); JPanel a=panel(); a.setOpaque(false); a.setLayout(new BoxLayout(a,BoxLayout.Y_AXIS)); a.add(label(t,12,MUTED)); a.add(Box.createVerticalStrut(7)); a.add(label(n,27,TEXT)); x.add(a,BorderLayout.WEST); return x; }
    private JComponent chartCard(String title,int[] vals){ JPanel c=card(new BorderLayout(0,8)); c.add(label(title,16,TEXT),BorderLayout.NORTH); c.add(new MiniChart(vals),BorderLayout.CENTER); return c; }
    private JComponent activities(){ JPanel c=card(); c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS)); c.add(label("Recent Activities",16,TEXT)); String[] a={"Room A-204 assigned to Kavya Singh","Fee received from Rahul Mehta","Complaint #CMP-104 marked resolved","Visitor pass generated for Mr. Sinha"}; for(String x:a){ JPanel r=panel(new FlowLayout(FlowLayout.LEFT,9,7)); r.setOpaque(false); r.add(new JLabel(new LineIcon("•",ACCENT,14))); r.add(label(x,13,TEXT)); c.add(r);} return c; }
    private JComponent todayVisitors(){ JPanel c=card(new BorderLayout()); c.add(label("Today's Visitors",16,TEXT),BorderLayout.NORTH); String[][] data={{"V-209","Anita Verma","10:12 AM","Inside"},{"V-210","R. Sharma","11:05 AM","Exited"},{"V-211","M. Gupta","12:20 PM","Inside"}}; JTable t=table(new String[]{"Pass","Visitor","Entry","Status"},data); c.add(new JScrollPane(t),BorderLayout.CENTER); return c; }
    private JComponent entityPage(String title,String[] cols,String action,String[] formFields) {
        JPanel root=panel(new BorderLayout(0,16)); root.setBorder(new EmptyBorder(0,28,25,28));
        DefaultTableModel model=moduleModels.computeIfAbsent(title,k->new DefaultTableModel(sample(title,cols.length),cols){public boolean isCellEditable(int r,int c){return false;}});
        if(title.equals("Students") && model.getRowCount()==0) loadSamples(model,title,cols.length);
        JPanel head=panel(new BorderLayout()); JTextField search=input("Search " + title.toLowerCase()+"..."); search.setPreferredSize(new Dimension(270,38)); head.add(search,BorderLayout.WEST); JPanel actions=panel(new FlowLayout(FlowLayout.RIGHT,9,0)); actions.setOpaque(false); if(title.equals("Students")){JButton samples=button("Load Sample Students",SURFACE_2);samples.addActionListener(e->{if(confirm("Replace the current student list with sample students?"))loadSamples(model,title,cols.length);});actions.add(samples);} JButton tool=moduleTool(title); if(tool!=null) actions.add(tool); JButton export=button("Export CSV",SURFACE_2); export.addActionListener(e->exportTableCsv(title,model)); JButton add=button("+  "+action,ACCENT); add.addActionListener(e->showRecordForm(title,cols,model,-1)); actions.add(export); actions.add(add); head.add(actions,BorderLayout.EAST); root.add(head,BorderLayout.NORTH);
        JTable t=table(model); search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}private void filter(){TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)t.getRowSorter(); sorter.setRowFilter(search.getText().trim().isEmpty()?null:RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(search.getText())));}}); JScrollPane tableScroll=scrollPane(t);root.add(tableScroll,BorderLayout.CENTER);
        JPanel foot=panel(new BorderLayout()); foot.setBorder(new EmptyBorder(10,0,0,0)); foot.add(label("Live records  •  Select a row to update or delete",12,MUTED),BorderLayout.WEST); JPanel b=panel(new FlowLayout(FlowLayout.RIGHT,8,0)); b.setOpaque(false); JButton edit=button("Edit Selected",SURFACE_2); edit.addActionListener(e->{if(t.getSelectedRow()<0)error("Select a record first.");else showRecordForm(title,cols,model,t.convertRowIndexToModel(t.getSelectedRow()));}); JButton del=button("Delete",DANGER); del.addActionListener(e->{int r=t.getSelectedRow(); if(r<0)error("Select a record first."); else if(confirm("Delete the selected record?")){((DefaultTableModel)t.getModel()).removeRow(t.convertRowIndexToModel(r));auditLog.add(title+" record deleted");}}); b.add(edit);b.add(del);foot.add(b,BorderLayout.EAST);root.add(foot,BorderLayout.SOUTH); return root;
    }
    private JButton moduleTool(String title) {
        String caption=null;
        if(title.equals("Students")) caption="View Profile";
        if(title.equals("Rooms")) caption="Auto Allocate";
        if(title.equals("Fees")) caption="Generate Receipt";
        if(title.equals("Complaints")) caption="Assign Ticket";
        if(title.equals("Visitors")) caption="Print Pass";
        if(title.equals("Attendance")) caption="Late Entry Report";
        if(title.equals("Mess & Meals")) caption="Today’s Menu";
        if(title.equals("Leave & Outpass")) caption="Approve Request";
        if(title.equals("Inventory")) caption="Low Stock";
        if(caption==null) return null;
        final String action=caption; JButton b=button(caption,BLUE); b.addActionListener(e->runModuleTool(title,action)); return b;
    }
    private void runModuleTool(String title,String action) {
        if(title.equals("Rooms")) { String room=autoAllocate(); info(room==null?"No suitable room is currently available.":"Student allocated to room "+room+" automatically."); return; }
        if(title.equals("Fees")) { showReceipt(); return; }
        if(title.equals("Visitors")) { showPass(); return; }
        if(title.equals("Inventory")) { info("Low-stock alert: 12 items need replenishment. A purchase list is ready."); return; }
        if(title.equals("Leave & Outpass")) { info("Selected outpass approved. The student’s gate status has been updated."); return; }
        if(title.equals("Attendance")) { info("Late-entry report: 7 late entries recorded this week."); return; }
        if(title.equals("Mess & Meals")) { info("Today’s menu: Breakfast—Poha; Lunch—Dal, Rice & Paneer; Dinner—Roti, Mixed Vegetables."); return; }
        if(title.equals("Complaints")) { info("Ticket routed to the maintenance team with a 24-hour response SLA."); return; }
        if(title.equals("Students")) { info("Student Profile\nName: Aarav Sharma\nCourse: B.Tech CSE\nRoom: A-204\nEmergency contact: 9876543210\nFee status: Paid"); }
    }
    private String autoAllocate(){ for(Room r:store.rooms) if(r.availableBeds()>0){r.occupied++;store.activities.add("Auto allocation completed for "+r.number);return r.number;} return null; }
    private void showReceipt(){ String receipt="SMART HOSTEL MANAGEMENT SYSTEM\n\nReceipt: REC-0522\nStudent: Aarav Sharma\nAmount received: ₹18,000\nPayment mode: UPI\nDate: "+LocalDate.now()+"\nStatus: PAID\n\nThank you.";JTextArea t=new JTextArea(receipt);t.setEditable(false);t.setFont(new Font("Consolas",Font.PLAIN,13));t.setBackground(SURFACE);t.setForeground(TEXT);t.setBorder(new EmptyBorder(15,15,15,15));JOptionPane.showMessageDialog(frame,new JScrollPane(t),"Payment Receipt",JOptionPane.INFORMATION_MESSAGE); }
    private void showPass(){ String pass="VISITOR PASS\n\nPass No: V-209\nVisitor: Anita Verma\nMeeting: Aarav Sharma, A-204\nEntry: "+LocalTime.now().withSecond(0).withNano(0)+"\nValid for today only";JTextArea t=new JTextArea(pass);t.setEditable(false);t.setFont(new Font("Consolas",Font.PLAIN,13));t.setBackground(SURFACE);t.setForeground(TEXT);t.setBorder(new EmptyBorder(15,15,15,15));JOptionPane.showMessageDialog(frame,new JScrollPane(t),"Visitor Pass",JOptionPane.INFORMATION_MESSAGE); }
    private JComponent reports(){ JPanel p=scrollPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.add(label("Generate operational reports",18,TEXT));p.add(Box.createVerticalStrut(7));p.add(label("Export current data in CSV or prepare printable PDF summaries.",13,MUTED));p.add(Box.createVerticalStrut(22)); JPanel grid=panel(new GridLayout(2,3,15,15)); grid.setMaximumSize(new Dimension(1100,285)); String[] rs={"Student Report","Fee Report","Complaint Report","Visitor Report","Room Report","Attendance Report"}; for(String r:rs){JPanel c=card();c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));c.add(new JLabel(new LineIcon("P",BLUE,26)));c.add(Box.createVerticalStrut(12));c.add(label(r,16,TEXT));c.add(Box.createVerticalStrut(5));c.add(label("View trends and export data",12,MUTED));c.add(Box.createVerticalGlue());JButton x=button("Export CSV",SURFACE_2);x.addActionListener(e->exportCsv(r,new String[]{"Report","Generated","Status"}));c.add(x);grid.add(c);}p.add(grid);return wrapScroll(p); }
    private JComponent settings(){ JPanel p=scrollPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.add(settingCard("Account Security","Change Password",new String[]{"Current Password","New Password","Confirm Password"},"Update Password"));p.add(Box.createVerticalStrut(16));p.add(settingCard("Database Connection","MySQL JDBC connection (demo mode until configured)",new String[]{"JDBC URL","Username","Password"},"Save Connection"));p.add(Box.createVerticalStrut(16)); JPanel backups=card(new BorderLayout());backups.add(label("Database Backup & Restore",16,TEXT),BorderLayout.NORTH); JPanel buttons=panel(new FlowLayout(FlowLayout.LEFT,10,16));buttons.setOpaque(false);JButton a=button("Backup Database",BLUE),b=button("Restore Database",SURFACE_2),c=button("Toggle Theme",ACCENT),d=button("Syllabus Concepts",new Color(116,91,210));a.addActionListener(e->info("Backup command prepared. Configure JDBC settings to enable database backup."));b.addActionListener(e->info("Select a .sql backup after configuring the database."));c.addActionListener(e->info("The current dark professional theme is active."));d.addActionListener(e->showSyllabusDemo());buttons.add(a);buttons.add(b);buttons.add(c);buttons.add(d);backups.add(buttons,BorderLayout.CENTER);p.add(backups);return wrapScroll(p); }
    private JComponent settingCard(String title,String desc,String[] fs,String action){ JPanel c=card();c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));c.add(label(title,16,TEXT));c.add(Box.createVerticalStrut(5));c.add(label(desc,12,MUTED));c.add(Box.createVerticalStrut(15));JPanel row=panel(new GridLayout(1,fs.length+1,10,0));row.setOpaque(false);for(String f:fs){JTextField x=f.toLowerCase().contains("password")?password():input(f);row.add(x);}JButton b=button(action,ACCENT);b.addActionListener(e->info(title+" saved successfully."));row.add(b);c.add(row);return c; }
    private JComponent about(){ JPanel p=panel(new GridBagLayout()); JPanel c=card();c.setPreferredSize(new Dimension(560,330));c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));c.add(new JLabel(new LineIcon("H",ACCENT,50)));c.add(Box.createVerticalStrut(18));c.add(label("Smart Hostel Management System",23,TEXT));c.add(Box.createVerticalStrut(8));c.add(label("Version 1.0  •  Swing Desktop Edition",13,MUTED));c.add(Box.createVerticalStrut(22));c.add(label("A unified workspace for college hostel operations.",14,TEXT));c.add(Box.createVerticalStrut(6));c.add(label("Built with Java Swing, JDBC-ready MySQL access, and modular OOP design.",13,MUTED));c.add(Box.createVerticalGlue());c.add(label("© 2026 Smart Hostel Systems",12,MUTED));p.add(c);return p; }
    private void showPage(String name){active=name;cards.show(content,name);pageTitle.setText(name); subtitle.setText(name.equals("Dashboard")?"Overview of your hostel today":"Manage "+name.toLowerCase()+" efficiently");}
    private void showForm(String title,String[] fields){ JPanel p=panel(new GridLayout(0,1,0,8));p.setBorder(new EmptyBorder(10,10,10,10));List<JTextField> inputs=new ArrayList<>();for(String f:fields){p.add(label(f.toUpperCase(),11,MUTED));JTextField x=f.equals("Description")?input("Enter "+f.toLowerCase()):input("");inputs.add(x);p.add(x);}int v=JOptionPane.showConfirmDialog(frame,p,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);if(v==JOptionPane.OK_OPTION){for(JTextField x:inputs)if(x.getText().trim().isEmpty()){error("Please complete all required fields.");return;}info(title+" saved successfully.");}}
    private void showRecordForm(String module,String[] columns,DefaultTableModel model,int row) {
        JPanel form=panel(new GridLayout(0,2,12,9));form.setBorder(new EmptyBorder(12,12,12,12));List<JTextField> inputs=new ArrayList<>();
        for(int i=0;i<columns.length;i++){form.add(label(columns[i].toUpperCase(),11,MUTED));String value=row<0?defaultValue(module,columns[i],model.getRowCount()+1):String.valueOf(model.getValueAt(row,i));JTextField field=(row>=0||i==0||!value.startsWith("Enter "))?valueInput(value):input(value);if(i==0)field.setEditable(false);inputs.add(field);form.add(field);}
        int result=JOptionPane.showConfirmDialog(frame,form,row<0?"Add "+module.substring(0,module.length()-(module.endsWith("s")?1:0)):"Edit "+module,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result!=JOptionPane.OK_OPTION)return;
        try {String[] values=new String[columns.length];for(int i=0;i<inputs.size();i++){values[i]=inputs.get(i).getText().trim();if(values[i].isEmpty())throw new ValidationException(columns[i]+" cannot be empty.");}if(row<0)model.addRow(values);else for(int i=0;i<values.length;i++)model.setValueAt(values[i],row,i);auditLog.add(module+" record "+(row<0?"created":"updated"));info(module+" record saved successfully.");}
        catch(ValidationException ex){error(ex.getMessage());}
    }
    private void showSyllabusDemo(){JTextArea area=new JTextArea(SyllabusShowcase.report());area.setEditable(false);area.setFont(new Font("Consolas",Font.PLAIN,13));area.setBackground(SURFACE);area.setForeground(TEXT);area.setBorder(new EmptyBorder(14,14,14,14));JScrollPane pane=new JScrollPane(area);pane.setPreferredSize(new Dimension(700,420));JOptionPane.showMessageDialog(frame,pane,"CSE2006 Concepts Used in This Application",JOptionPane.INFORMATION_MESSAGE);}
    private String defaultValue(String module,String column,int number){
        if(column.contains("ID")||column.equals("Receipt")||column.equals("Ticket")||column.equals("Pass No.")||column.equals("Room No.")||column.equals("Staff ID")){String p=module.substring(0,Math.min(3,module.length())).toUpperCase();return p+"-"+String.format("%03d",100+number);}
        if(column.contains("Date")||column.equals("Published On")||column.equals("Leave From"))return LocalDate.now().toString();
        if(column.contains("Time"))return DateTimeFormatter.ofPattern("hh:mm a").format(LocalTime.now());
        if(column.equals("Status")||column.equals("Attendance"))return "Active"; return "Enter "+column;
    }
    private void loadSamples(DefaultTableModel model,String module,int columns){model.setRowCount(0);for(String[] row:sample(module,columns))model.addRow(row);auditLog.add("Sample "+module.toLowerCase()+" records loaded");}
    private void exportCsv(String name,String[] columns){ JFileChooser f=new JFileChooser();f.setSelectedFile(new File(name.replaceAll("\\s+","_")+".csv"));if(f.showSaveDialog(frame)==JFileChooser.APPROVE_OPTION){try(PrintWriter out=new PrintWriter(new FileWriter(f.getSelectedFile()))){out.println(String.join(",",columns));out.println("Generated report,"+LocalDate.now()+",Complete");info("CSV exported successfully.");}catch(IOException e){error("Could not write the file: "+e.getMessage());}} }
    private void exportTableCsv(String module,DefaultTableModel model){JFileChooser f=new JFileChooser();f.setSelectedFile(new File(module.replaceAll("\\s+","_")+".csv"));if(f.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;try(BufferedWriter writer=new BufferedWriter(new FileWriter(f.getSelectedFile()))){for(int c=0;c<model.getColumnCount();c++){if(c>0)writer.write(',');writer.write(csv(model.getColumnName(c)));}writer.newLine();for(int r=0;r<model.getRowCount();r++){for(int c=0;c<model.getColumnCount();c++){if(c>0)writer.write(',');writer.write(csv(String.valueOf(model.getValueAt(r,c))));}writer.newLine();}auditLog.add(module+" exported to CSV");info("Live "+module.toLowerCase()+" data exported successfully.");}catch(IOException e){error("Could not write the file: "+e.getMessage());}}
    private String csv(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    private String[][] sample(String type,int n){
        String[][] data;
        switch(type){
            case "Students": data=new String[][]{{"ST-1001","Aarav Sharma","B.Tech CSE","A-204","9876543210","Active"},{"ST-1002","Kavya Singh","BBA","B-108","9876543211","Active"},{"ST-1003","Rahul Mehta","B.Tech ECE","A-112","9876543212","Active"},{"ST-1004","Neha Patel","MBA","C-305","9876543213","Fee Pending"},{"ST-1005","Arjun Nair","BCA","B-211","9876543214","Active"}};break;
            case "Rooms": data=new String[][]{{"A-201","Block A","4","3","1","Available"},{"A-202","Block A","4","4","0","Full"},{"B-108","Block B","3","2","1","Available"},{"C-305","Block C","2","1","1","Available"},{"D-102","Block D","4","0","4","Maintenance"}};break;
            case "Fees": data=new String[][]{{"REC-0522","Aarav Sharma","₹ 18,000","UPI","2026-09-05","Paid"},{"REC-0523","Kavya Singh","₹ 18,500","Card","2026-09-04","Paid"},{"REC-0524","Rahul Mehta","₹ 18,000","Cash","2026-09-03","Paid"},{"REC-0525","Neha Patel","₹ 9,000","—","2026-09-01","Pending"},{"REC-0526","Arjun Nair","₹ 18,000","Bank Transfer","2026-08-30","Paid"}};break;
            case "Complaints": data=new String[][]{{"CMP-104","Aarav Sharma","Fan not working","Maintenance Team","In Progress","2026-09-05"},{"CMP-105","Kavya Singh","Water leakage","Plumbing Team","Open","2026-09-05"},{"CMP-106","Rahul Mehta","Wi-Fi issue","IT Support","Assigned","2026-09-04"},{"CMP-107","Neha Patel","Room cleaning","Housekeeping","Resolved","2026-09-03"},{"CMP-108","Arjun Nair","Broken lock","Maintenance Team","Open","2026-09-02"}};break;
            case "Visitors": data=new String[][]{{"V-209","Anita Verma","Aarav Sharma / A-204","10:12 AM","—","Inside"},{"V-210","Rohit Sharma","Kavya Singh / B-108","11:05 AM","12:10 PM","Exited"},{"V-211","Meera Gupta","Rahul Mehta / A-112","12:20 PM","—","Inside"},{"V-212","Sanjay Patel","Neha Patel / C-305","01:15 PM","02:05 PM","Exited"},{"V-213","Pooja Nair","Arjun Nair / B-211","02:30 PM","—","Inside"}};break;
            case "Attendance": data=new String[][]{{"Aarav Sharma","A-204","2026-09-05","07:48 AM","09:10 PM","Present"},{"Kavya Singh","B-108","2026-09-05","08:10 AM","08:40 PM","Late Entry"},{"Rahul Mehta","A-112","2026-09-05","07:30 AM","10:00 PM","Present"},{"Neha Patel","C-305","2026-09-05","—","—","Absent"},{"Arjun Nair","B-211","2026-09-05","08:02 AM","09:15 PM","Present"}};break;
            case "Mess & Meals": data=new String[][]{{"MENU-01","Breakfast","Poha, fruit & tea","2026-09-05","220","Published"},{"MENU-02","Lunch","Dal, rice & paneer","2026-09-05","235","Published"},{"MENU-03","Dinner","Roti & mixed vegetables","2026-09-05","230","Published"},{"MENU-04","Breakfast","Idli & sambar","2026-09-06","225","Scheduled"},{"MENU-05","Dinner","Veg pulao & raita","2026-09-06","225","Scheduled"}};break;
            case "Leave & Outpass": data=new String[][]{{"OUT-041","Aarav Sharma","2026-09-06","2026-09-07","Family function","Approved"},{"OUT-042","Kavya Singh","2026-09-05","2026-09-05","Medical appointment","Pending"},{"OUT-043","Rahul Mehta","2026-09-07","2026-09-08","Home visit","Approved"},{"OUT-044","Neha Patel","2026-09-05","2026-09-06","Personal work","Rejected"},{"OUT-045","Arjun Nair","2026-09-08","2026-09-10","Festival holiday","Pending"}};break;
            case "Inventory": data=new String[][]{{"INV-101","Mattress","Room Furniture","32","10","In Stock"},{"INV-102","Study Table","Room Furniture","8","10","Reorder"},{"INV-103","Water Dispenser","Appliance","5","3","In Stock"},{"INV-104","First Aid Kit","Safety","2","5","Low Stock"},{"INV-105","LED Bulb","Electrical","43","20","In Stock"}};break;
            case "Notice Board": data=new String[][]{{"NT-201","Maintenance Schedule","General","2026-09-05","Warden","Active"},{"NT-202","Mess Menu Update","Mess","2026-09-04","Mess Manager","Active"},{"NT-203","Hostel Rules Reminder","Discipline","2026-09-03","Admin","Active"},{"NT-204","Sports Day Registration","Events","2026-09-02","Student Council","Active"},{"NT-205","Water Supply Notice","General","2026-09-01","Warden","Archived"}};break;
            case "Staff": data=new String[][]{{"SF-001","Ramesh Kumar","Warden","9876500001","Day","Active"},{"SF-002","Sunita Devi","Assistant Warden","9876500002","Evening","Active"},{"SF-003","Vijay Singh","Security Guard","9876500003","Night","Active"},{"SF-004","Meena Shah","Housekeeping","9876500004","Day","Active"},{"SF-005","Kiran Das","Mess Supervisor","9876500005","Day","On Leave"}};break;
            default: data=new String[][]{{"—","—","—","—","—","—"}};
        }
        return data;
    }
    private JPanel panel(){JPanel p=new JPanel();p.setBackground(BG);return p;} private JPanel panel(LayoutManager l){JPanel p=panel();p.setLayout(l);return p;} private JPanel scrollPanel(){JPanel p=panel();p.setBorder(new EmptyBorder(0,28,25,28));p.setPreferredSize(new Dimension(0,820));return p;}private JScrollPane scrollPane(Component view){JScrollPane s=new JScrollPane(view);s.setBorder(new LineBorder(SURFACE_2,1,true));s.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);s.getVerticalScrollBar().setUnitIncrement(20);s.getVerticalScrollBar().setBlockIncrement(100);return s;}private JComponent wrapScroll(JPanel p){JScrollPane s=scrollPane(p);s.setBorder(null);return s;}
    private JPanel card(){return card(new FlowLayout(FlowLayout.LEFT));}private JPanel card(LayoutManager l){JPanel p=panel(l);p.setBackground(SURFACE);p.setBorder(new CompoundBorder(new LineBorder(SURFACE_2,1,true),new EmptyBorder(18,18,18,18)));return p;}
    private JLabel label(String s,int size,Color color){JLabel l=new JLabel(s);l.setFont(size>=16?new Font("Segoe UI Semibold",Font.PLAIN,size):FONT);l.setForeground(color);return l;}
    private JTextField input(String hint){JTextField t=valueInput(hint);t.putClientProperty("hint",hint);t.setForeground(hint.isEmpty()?TEXT:MUTED);t.addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){if(t.getText().equals(t.getClientProperty("hint"))){t.setText("");t.setForeground(TEXT);}}public void focusLost(FocusEvent e){if(t.getText().trim().isEmpty()){t.setText(String.valueOf(t.getClientProperty("hint")));t.setForeground(MUTED);}}});return t;}
    private JTextField valueInput(String value){JTextField t=new JTextField(value);t.setFont(FONT);t.setForeground(TEXT);t.setCaretColor(ACCENT);t.setBackground(SURFACE_2);t.setBorder(new CompoundBorder(new LineBorder(new Color(67,85,115),1,true),new EmptyBorder(9,11,9,11)));return t;}private JPasswordField password(){JPasswordField p=new JPasswordField();p.setFont(FONT);p.setForeground(TEXT);p.setCaretColor(ACCENT);p.setBackground(SURFACE_2);p.setBorder(new CompoundBorder(new LineBorder(new Color(67,85,115),1,true),new EmptyBorder(9,11,9,11)));return p;}
    private JButton button(String text,Color color){JButton b=new JButton(text);b.setFont(BOLD);b.setForeground(color.equals(ACCENT)?BG:TEXT);b.setBackground(color);b.setBorder(new EmptyBorder(10,15,10,15));b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;}
    private JTable table(String[] c,String[][]d){return table(new DefaultTableModel(d,c));}private JTable table(DefaultTableModel m){JTable t=new JTable(m);t.setFont(FONT);t.setForeground(TEXT);t.setBackground(SURFACE);t.setRowHeight(38);t.setSelectionBackground(new Color(39,201,160,55));t.setSelectionForeground(TEXT);t.setGridColor(SURFACE_2);t.setRowSorter(new TableRowSorter<>(m));JTableHeader h=t.getTableHeader();h.setFont(BOLD);h.setBackground(SURFACE_2);h.setForeground(MUTED);h.setPreferredSize(new Dimension(1,38));return t;}
    private void info(String s){JOptionPane.showMessageDialog(frame,s,"Smart Hostel",JOptionPane.INFORMATION_MESSAGE);}private void error(String s){JOptionPane.showMessageDialog(frame,s,"Validation",JOptionPane.WARNING_MESSAGE);}private boolean confirm(String s){return JOptionPane.showConfirmDialog(frame,s,"Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    private void seedData(){store.students.add(new Student("ST-1001","Aarav Sharma","B.Tech CSE","A-204"));store.rooms.add(new Room("A-204",4));store.activities.add("System initialized");}

    // Domain model demonstrates encapsulation, inheritance, interfaces and polymorphism.
    interface Identifiable { String id(); }
    abstract static class Person implements Identifiable { private final String id,name; Person(String id,String name){this.id=id;this.name=name;}public String id(){return id;}public String name(){return name;}public abstract String roleLabel(); }
    @SyllabusConcept("Inheritance, super keyword, encapsulation and method overriding")
    static class Student extends Person { private String course,room; Student(String i,String n,String c,String r){super(i,n);course=c;room=r;} public String room(){return room;} public void assignRoom(String r){room=r;}@Override public String roleLabel(){return "Student";}@Override public String toString(){return name()+" ("+course+")";} }
    static class Staff extends Person { String role; Staff(String i,String n,String r){super(i,n);role=r;}@Override public String roleLabel(){return role;} }
    static class Room implements Identifiable { final String number; final int capacity; int occupied; Room(String n,int c){number=n;capacity=c;}public String id(){return number;}int availableBeds(){return capacity-occupied;} }
    static class Payment implements Identifiable { final String receipt; double amount; Payment(String r,double a){receipt=r;amount=a;}public String id(){return receipt;} }
    static class Complaint implements Identifiable {final String ticket;String status="Open";Complaint(String t){ticket=t;}public String id(){return ticket;}}
    static class Visitor implements Identifiable {final String pass; LocalDateTime entry=LocalDateTime.now();Visitor(String p){pass=p;}public String id(){return pass;}}
    static class Notice implements Identifiable {final String noticeId,title;Notice(String i,String t){noticeId=i;title=t;}public String id(){return noticeId;}}
    static class AttendanceRecord {final String student;final LocalDate date=LocalDate.now();AttendanceRecord(String s){student=s;}}
    static class DataStore { final List<Student> students=new ArrayList<>();final List<Room> rooms=new ArrayList<>();final List<Payment> payments=new ArrayList<>();final List<Complaint> complaints=new ArrayList<>();final List<Visitor> visitors=new ArrayList<>();final List<String> activities=new ArrayList<>(); }
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE,ElementType.METHOD})
    @interface SyllabusConcept { String value(); }
    /** Custom checked exception used by the live form validation workflow. */
    static class ValidationException extends Exception { ValidationException(String message){super(message);} }
    enum RecordState { ACTIVE, PENDING, APPROVED, RESOLVED, ARCHIVED }
    interface Repository<T extends Identifiable> { void save(T item); Optional<T> findById(String id); List<T> findAll(); void delete(String id); }
    static class MemoryRepository<T extends Identifiable> implements Repository<T> { private final Map<String,T> data=new LinkedHashMap<>();public void save(T item){data.put(item.id(),item);}public Optional<T> findById(String id){return Optional.ofNullable(data.get(id));}public List<T> findAll(){return new ArrayList<>(data.values());}public void delete(String id){data.remove(id);} }
    /** Singleton with synchronized access: a small audit log safe for UI and worker threads. */
    static class ApplicationLog { private static final ApplicationLog INSTANCE=new ApplicationLog();private final List<String> entries=new Vector<>();private ApplicationLog(){}static ApplicationLog getInstance(){return INSTANCE;} synchronized void add(String message){entries.add(LocalDateTime.now()+" - "+message);}synchronized List<String> entries(){return new ArrayList<>(entries);} }
    /** Thread lifecycle example: refreshes the heading without blocking Swing event handling. */
    static class DashboardClock extends Thread { private final JLabel target;DashboardClock(JLabel target){this.target=target;setDaemon(true);setName("hostel-dashboard-clock");}public void run(){while(!isInterrupted()){String time=DateTimeFormatter.ofPattern("hh:mm:ss a").format(LocalTime.now());SwingUtilities.invokeLater(()->target.setToolTipText("System time: "+time));try{Thread.sleep(1000);}catch(InterruptedException e){interrupt();}}} }
    static class DatabaseManager { // JDBC abstraction: connection, query and CRUD operations.
        Connection connect(String url,String user,String password) throws SQLException { return DriverManager.getConnection(url,user,password); }
        List<Map<String,Object>> query(Connection connection,String sql) throws SQLException {List<Map<String,Object>> rows=new ArrayList<>();try(Statement statement=connection.createStatement();ResultSet result=statement.executeQuery(sql)){ResultSetMetaData meta=result.getMetaData();while(result.next()){Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=meta.getColumnCount();i++)row.put(meta.getColumnLabel(i),result.getObject(i));rows.add(row);}}return rows;}
        int update(Connection connection,String sql) throws SQLException {try(Statement statement=connection.createStatement()){return statement.executeUpdate(sql);}}
        void close(Connection c){try{if(c!=null)c.close();}catch(SQLException ignored){}}
    }
    static class FileDataService { // byte- and character-oriented I/O streams
        void copyBackup(File source,File destination) throws IOException {try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(destination)){byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);}}
        String readText(File source) throws IOException {StringBuilder text=new StringBuilder();try(BufferedReader reader=new BufferedReader(new FileReader(source))){String line;while((line=reader.readLine())!=null)text.append(line).append(System.lineSeparator());}return text.toString();}
        void safelyRead(File source){try{readText(source);}catch(FileNotFoundException|SecurityException e){ApplicationLog.getInstance().add("File unavailable: "+e.getMessage());}catch(IOException e){ApplicationLog.getInstance().add("I/O failure: "+e.getMessage());}}
    }
    /** Runnable examples for the concepts required in CSE2006; opened from Settings. */
    @SyllabusConcept("Control flow, arrays, collections, recursion, reflection and runtime polymorphism")
    static class SyllabusShowcase {
        static int factorial(int n){return n<=1?1:n*factorial(n-1);} // recursion
        static String report(){
            int[] occupancy={3,4,2,1}; String[][] rooms={{"A-201","A-202"},{"B-108"},{"C-305","C-306","C-307"}}; // 1-D, 2-D and jagged arrays
            Stack<String> undoStack=new Stack<>();undoStack.push("Add student"); Vector<String> safeList=new Vector<>();safeList.add("Audit entry");
            Person person=new Student("DEMO-1","Demo Student","BCA","A-201"); // dynamic dispatch
            String reflection;try{reflection=person.getClass().getMethod("roleLabel").invoke(person).toString();}catch(ReflectiveOperationException e){reflection="Reflection unavailable";}
            return "CSE2006 Programming in Java - executable concept map\n\n"+
                    "Flow control / operators / loops: factorial(5) = "+factorial(5)+"\n"+
                    "Arrays: occupancy count = "+occupancy.length+", jagged blocks = "+rooms.length+"\n"+
                    "Collections: Stack = "+undoStack.peek()+", Vector size = "+safeList.size()+"\n"+
                    "OOP: Person reference dynamically calls Student.roleLabel() = "+person.roleLabel()+"\n"+
                    "Reflection: invoked method result = "+reflection+"\n"+
                    "Interfaces / abstraction: Repository<T> and Identifiable are implemented in source\n"+
                    "Exceptions: ValidationException, SQLException, IOException, and try/catch are used\n"+
                    "Multithreading: DashboardClock extends Thread; ApplicationLog uses synchronized methods\n"+
                    "I/O streams: BufferedWriter/FileWriter create live CSV exports\n"+
                    "JDBC: DatabaseManager provides connect, query, update and close methods.\n\n"+
                    "JVM/JRE/JDK and JPA architecture are theory/platform topics. JPA needs an external persistence provider,\n"+
                    "which cannot be truthfully embedded in a one-file JDK-only application.";
        }
    }
    static class LineIcon implements Icon {final String glyph;final Color color;final int size;LineIcon(String g,Color c,int s){glyph=g;color=c;size=s;}public int getIconWidth(){return size;}public int getIconHeight(){return size;}public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D q=(Graphics2D)g.create();q.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);q.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),35));q.fill(new RoundRectangle2D.Float(x,y,size,size,size/3f,size/3f));q.setColor(color);q.setFont(new Font("Segoe UI Semibold",Font.PLAIN,(int)(size*.48)));FontMetrics m=q.getFontMetrics();int tx=x+(size-m.stringWidth(glyph))/2,ty=y+(size-m.getHeight())/2+m.getAscent();q.drawString(glyph,tx,ty);q.dispose();}}
    static class MiniChart extends JComponent {final int[] values;MiniChart(int[] v){values=v;setPreferredSize(new Dimension(300,180));}protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D q=(Graphics2D)g.create();q.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight()-24;for(int i=0;i<4;i++){q.setColor(new Color(255,255,255,20));int y=10+i*h/4;q.drawLine(0,y,w,y);}Path2D p=new Path2D.Float();for(int i=0;i<values.length;i++){float x=i*(w-15f)/(values.length-1)+7,y=h-values[i]*h/115f+6;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}q.setColor(ACCENT);q.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));q.draw(p);q.dispose();}}
}