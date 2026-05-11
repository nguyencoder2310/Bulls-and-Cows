import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Random;

public class LoginPanel extends JPanel {

    private final GameClient client;
    private JTextField nickField;             // shared nickname field
    private JTextField nickJoinField;         // nickname on join screen
    private JTextField codeField;             // room code for join
    private JTextField serverIpField;         // LAN IP
    private JTextField nickLocalField;        // nickname on local screen
    private JTextField radminIpCreateField;   // Radmin IP on create screen
    private JTextField radminNickCreateField; // nickname on create screen (Radmin)
    private JTextField radminNickJoinField;   // nickname on join screen (Radmin)
    private JTextField radminCodeField;       // room code for Radmin join
    private JTextField radminIpJoinField;     // Radmin IP on join screen
    private JTextField lanIpDisplayField;     // visible IP field on LAN screen

    private JPanel     cardArea;
    private CardLayout cardAreaLayout;

    public LoginPanel(GameClient client) {
        this.client = client;
        buildUI();
    }

    // ── Public API called by GameClient ──────────────────────────────────
    public String getNickname() {
        if (radminNickJoinField != null && !radminNickJoinField.getText().trim().isEmpty()
                && radminNickJoinField.isShowing()) return radminNickJoinField.getText().trim();
        if (radminNickCreateField != null && !radminNickCreateField.getText().trim().isEmpty()
                && radminNickCreateField.isShowing()) return radminNickCreateField.getText().trim();
        if (nickJoinField  != null && !nickJoinField.getText().trim().isEmpty()
                && nickJoinField.isShowing())  return nickJoinField.getText().trim();
        if (nickLocalField != null && !nickLocalField.getText().trim().isEmpty())
            return nickLocalField.getText().trim();
        if (nickField != null) return nickField.getText().trim();
        return "Player";
    }

    public String getServerIp() {
        return serverIpField == null ? "auto" : serverIpField.getText().trim();
    }

    public void setServerIp(String ip) {
        if (serverIpField != null) serverIpField.setText(ip);
        if (lanIpDisplayField != null) {
            lanIpDisplayField.setText(ip);
            // Highlight để user thấy đã tự điền
            lanIpDisplayField.setForeground(GOLD);
        }
    }

    /** Trả về IP Radmin từ màn hình đang hiển thị */
    public String getRadminIp() {
        if (radminIpJoinField != null && radminIpJoinField.isShowing())
            return radminIpJoinField.getText().trim();
        if (radminIpCreateField != null)
            return radminIpCreateField.getText().trim();
        return "";
    }

    public String getRoomCode() {
        if (radminCodeField != null && radminCodeField.isShowing())
            return radminCodeField.getText().trim().toUpperCase();
        return codeField == null ? "" : codeField.getText().trim().toUpperCase();
    }

    // ── Build ─────────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(BG);

        StarfieldPanel stars = new StarfieldPanel();
        stars.setLayout(new BorderLayout());

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        content.add(buildTitle(), BorderLayout.NORTH);

        cardAreaLayout = new CardLayout();
        cardArea = new JPanel(cardAreaLayout);
        cardArea.setOpaque(false);
        cardArea.add(buildMainMenu(),         "main");
        cardArea.add(buildCreateScreen(),     "create");
        cardArea.add(buildJoinScreen(),       "join");
        cardArea.add(buildLocalScreen(),      "local");
        cardArea.add(buildRadminCreateScreen(),"radmin_create");
        cardArea.add(buildRadminJoinScreen(), "radmin_join");

        content.add(cardArea, BorderLayout.CENTER);
        stars.add(content, BorderLayout.CENTER);
        add(stars, BorderLayout.CENTER);
    }

    // ── Title block ───────────────────────────────────────────────────────
    private JPanel buildTitle() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(14, 0, 32, 0));

        JLabel emoji = new JLabel("🐂 🐄");
        emoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
        JLabel title = makeLabel("BULLS & COWS", 26, Font.BOLD, GOLD);
        title.setFont(new Font("Courier New", Font.BOLD, 26));
        JLabel sub   = makeLabel("Number Guessing · Multiplayer", 11, Font.PLAIN, SUBTEXT);

        for (JLabel l : new JLabel[]{emoji, title, sub}) {
            l.setAlignmentX(CENTER_ALIGNMENT);
            p.add(l);
            p.add(Box.createVerticalStrut(4));
        }
        return p;
    }

    // ── Main menu ─────────────────────────────────────────────────────────
    private JPanel buildMainMenu() {
        JPanel p = centeredColumn();

        p.add(Box.createVerticalGlue());
        p.add(menuBtn("  🖥  Tạo Phòng (Host)",   FROST,  "radmin_create"));
        p.add(vgap(14));
        p.add(menuBtn("  🎮  Vào Phòng (Join)",    ORANGE, "radmin_join"));
        p.add(vgap(14));
        p.add(menuBtn("  📡  Chơi Mạng LAN",        PURPLE, "local"));
        p.add(Box.createVerticalGlue());

        // Chú thích Radmin
        JLabel hint = new JLabel("* Tạo Phòng / Vào Phòng dùng Radmin VPN");
        hint.setFont(new Font("Courier New", Font.PLAIN, 10));
        hint.setForeground(SUBTEXT);
        hint.setAlignmentX(CENTER_ALIGNMENT);
        p.add(hint);
        p.add(vgap(8));
        return p;
    }

    // ── Create screen ─────────────────────────────────────────────────────
    private JPanel buildCreateScreen() {
        JPanel p = centeredColumn();
        p.add(Box.createVerticalGlue());
        p.add(screenTitle("Tạo Phòng Mới"));
        p.add(vgap(20));
        p.add(fieldLbl("Nickname của bạn"));
        p.add(vgap(6));
        nickField = styledField("Player1");
        p.add(nickField);
        p.add(vgap(26));
        p.add(actionBtn("Tạo phòng  →", FROST, () -> {
            if (validateNick(nickField)) client.connectOnline("CREATE");
        }));
        p.add(vgap(10));
        p.add(backBtn());
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Join screen ───────────────────────────────────────────────────────
    private JPanel buildJoinScreen() {
        JPanel p = centeredColumn();
        p.add(Box.createVerticalGlue());
        p.add(screenTitle("Vào Phòng"));
        p.add(vgap(20));
        p.add(fieldLbl("Nickname của bạn"));
        p.add(vgap(6));
        nickJoinField = styledField("Player1");
        p.add(nickJoinField);
        p.add(vgap(14));
        p.add(fieldLbl("Mã phòng (Room Code)"));
        p.add(vgap(6));
        codeField = styledField("ABCD");
        codeField.setFont(new Font("Courier New", Font.BOLD, 22));
        codeField.setHorizontalAlignment(JTextField.CENTER);
        p.add(codeField);
        p.add(vgap(26));
        p.add(actionBtn("Vào phòng  →", ORANGE, () -> {
            if (validateNick(nickJoinField)) client.connectOnline("JOIN");
        }));
        p.add(vgap(10));
        p.add(backBtn());
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Local/LAN screen ──────────────────────────────────────────────────
    private JPanel buildLocalScreen() {
        JPanel p = centeredColumn();
        serverIpField = new JTextField("auto");

        p.add(Box.createVerticalGlue());
        p.add(screenTitle("Chơi Mạng LAN"));
        p.add(vgap(20));
        p.add(fieldLbl("Nickname của bạn"));
        p.add(vgap(6));
        nickLocalField = styledField("Player1");
        p.add(nickLocalField);
        p.add(vgap(14));
        p.add(fieldLbl("Server IP  (để trống = tự tìm)"));
        p.add(vgap(6));
        lanIpDisplayField = styledField("auto");
        // Reset màu khi user tự gõ
        lanIpDisplayField.addCaretListener(e -> lanIpDisplayField.setForeground(new Color(0xEC, 0xEF, 0xF4)));
        p.add(lanIpDisplayField);
        p.add(vgap(20));

        // Two-button row
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(340, 46));
        row.setAlignmentX(CENTER_ALIGNMENT);

        JButton findBtn = tinyBtn("📡 Tự tìm", PURPLE);
        findBtn.addActionListener(e -> {
            if (validateNick(nickLocalField)) {
                serverIpField.setText("auto");
                lanIpDisplayField.setText("Đang tìm...");
                lanIpDisplayField.setForeground(SUBTEXT);
                client.discoverServer();
            }
        });
        JButton connBtn = tinyBtn("Kết nối  →", FROST);
        connBtn.addActionListener(e -> {
            if (validateNick(nickLocalField)) {
                serverIpField.setText(lanIpDisplayField.getText().trim());
                client.connectToServer();
            }
        });
        row.add(findBtn);
        row.add(connBtn);
        p.add(row);
        p.add(vgap(10));
        p.add(backBtn());
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Radmin – Create screen ────────────────────────────────────────────
    private JPanel buildRadminCreateScreen() {
        JPanel p = centeredColumn();
        p.add(Box.createVerticalGlue());
        p.add(screenTitle("🖥  Tạo Phòng (Radmin)"));
        p.add(vgap(8));

        JLabel info = new JLabel("<html><center>Bạn là <b>HOST</b> — chạy GameServer trước,<br>rồi nhấn Tạo phòng.</center></html>");
        info.setFont(new Font("Courier New", Font.PLAIN, 11));
        info.setForeground(SUBTEXT);
        info.setAlignmentX(CENTER_ALIGNMENT);
        p.add(info);
        p.add(vgap(16));

        p.add(fieldLbl("Nickname của bạn"));
        p.add(vgap(6));
        radminNickCreateField = styledField("Player1");
        p.add(radminNickCreateField);
        p.add(vgap(14));

        p.add(fieldLbl("IP Radmin của bạn (máy host)  ví dụ: 26.x.x.x"));
        p.add(vgap(6));
        radminIpCreateField = styledField("26.0.0.1");
        p.add(radminIpCreateField);
        p.add(vgap(26));

        p.add(actionBtn("Tạo phòng  →", FROST, () -> {
            if (validateNick(radminNickCreateField)) client.connectOnline("CREATE");
        }));
        p.add(vgap(10));
        p.add(backBtn());
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Radmin – Join screen ──────────────────────────────────────────────
    private JPanel buildRadminJoinScreen() {
        JPanel p = centeredColumn();
        p.add(Box.createVerticalGlue());
        p.add(screenTitle("🎮  Vào Phòng (Radmin)"));
        p.add(vgap(16));

        p.add(fieldLbl("Nickname của bạn"));
        p.add(vgap(6));
        radminNickJoinField = styledField("Player1");
        p.add(radminNickJoinField);
        p.add(vgap(14));

        p.add(fieldLbl("IP Radmin của host  (hỏi người tạo phòng)"));
        p.add(vgap(6));
        radminIpJoinField = styledField("26.0.0.1");
        p.add(radminIpJoinField);
        p.add(vgap(14));

        p.add(fieldLbl("Mã phòng (Room Code)"));
        p.add(vgap(6));
        radminCodeField = styledField("ABCD");
        radminCodeField.setFont(new Font("Courier New", Font.BOLD, 22));
        radminCodeField.setHorizontalAlignment(JTextField.CENTER);
        p.add(radminCodeField);
        p.add(vgap(26));

        p.add(actionBtn("Vào phòng  →", ORANGE, () -> {
            if (validateNick(radminNickJoinField)) client.connectOnline("JOIN");
        }));
        p.add(vgap(10));
        p.add(backBtn());
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private boolean validateNick(JTextField f) {
        if (f.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập nickname trước nhé!");
            return false;
        }
        return true;
    }

    private JPanel centeredColumn() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private Component vgap(int h) { return Box.createVerticalStrut(h); }

    private JLabel makeLabel(String text, int size, int style, Color fg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Courier New", style, size));
        l.setForeground(fg);
        return l;
    }

    private JLabel screenTitle(String text) {
        JLabel l = makeLabel(text, 18, Font.BOLD, GOLD);
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private JLabel fieldLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Courier New", Font.PLAIN, 11));
        l.setForeground(SUBTEXT);
        l.setAlignmentX(CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(340, 18));
        return l;
    }

    private JTextField styledField(String def) {
        JTextField tf = new JTextField(def, 14);
        tf.setBackground(new Color(0x12, 0x1C, 0x30));
        tf.setForeground(new Color(0xEC, 0xEF, 0xF4));
        tf.setCaretColor(GOLD);
        tf.setFont(new Font("Courier New", Font.PLAIN, 15));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FROST.darker(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tf.setMaximumSize(new Dimension(340, 42));
        tf.setAlignmentX(CENTER_ALIGNMENT);
        return tf;
    }

    /** Large menu button (main screen) */
    private JPanel menuBtn(String text, Color color, String card) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = getModel().isRollover();
                // Shadow
                g2.setColor(new Color(0,0,0,80));
                g2.fillRoundRect(3, 5, getWidth()-6, getHeight()-5, 18, 18);
                // Body
                Color body = hov ? color : color.darker();
                g2.setColor(body);
                g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-4, 18, 18);
                // Border glow
                g2.setColor(hov ? color.brighter() : color);
                g2.setStroke(new java.awt.BasicStroke(1.6f));
                g2.drawRoundRect(0, 0, getWidth()-2, getHeight()-4, 18, 18);
                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight()-4+fm.getAscent()-fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Courier New", Font.BOLD, 15));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(290, 52));
        btn.setMaximumSize(new Dimension(340, 52));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.addActionListener(e -> cardAreaLayout.show(cardArea, card));

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.add(btn);
        wrap.setAlignmentX(CENTER_ALIGNMENT);
        return wrap;
    }

    /** Smaller action button (sub-screens) */
    private JButton actionBtn(String text, Color color, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Courier New", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(340, 46));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JButton tinyBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Courier New", Font.BOLD, 13));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton backBtn() {
        JButton btn = new JButton("← Quay lại");
        btn.setFont(new Font("Courier New", Font.PLAIN, 12));
        btn.setForeground(SUBTEXT);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.addActionListener(e -> cardAreaLayout.show(cardArea, "main"));
        return btn;
    }

    // ── Colors ────────────────────────────────────────────────────────────
    static final Color BG      = new Color(0x0D, 0x14, 0x26);
    static final Color FROST   = new Color(0x5E, 0x81, 0xAC);
    static final Color ORANGE  = new Color(0xD0, 0x87, 0x70);
    static final Color PURPLE  = new Color(0xB4, 0x8E, 0xAD);
    static final Color GOLD    = new Color(0xEB, 0xCB, 0x8B);
    static final Color SUBTEXT = new Color(0x6B, 0x74, 0x8A);

    // ── Starfield ─────────────────────────────────────────────────────────
    static class StarfieldPanel extends JPanel {
        private final int[][] stars = new int[90][3];
        StarfieldPanel() {
            setBackground(BG);
            Random rng = new Random(77);
            for (int[] s : stars) {
                s[0] = rng.nextInt(1000);
                s[1] = rng.nextInt(1000);
                s[2] = 1 + rng.nextInt(3);
            }
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            for (int[] s : stars) {
                int x = s[0] * getWidth()  / 1000;
                int y = s[1] * getHeight() / 1000;
                int r = s[2];
                float a = r == 3 ? 0.85f : r == 2 ? 0.55f : 0.3f;
                g2.setColor(new Color(1f, 1f, 1f, a));
                g2.fillOval(x, y, r, r);
            }
        }
    }
}