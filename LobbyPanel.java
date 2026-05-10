import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class LobbyPanel extends JPanel {

    private final GameClient client;
    private JTextPane chatArea;
    private JTextField chatField;
    private JTextField roomCodeField;
    private JSpinner digitSpinner;

    public LobbyPanel(GameClient client) {
        this.client = client;
        buildUI();
    }

    // ── Dark theme colors (khớp LoginPanel & RoomPanel) ──────────────────
    private static final Color BG       = new Color(0x0D, 0x14, 0x26);
    private static final Color BG_SEC   = new Color(0x12, 0x1C, 0x30);
    private static final Color BG_TOP   = new Color(0x16, 0x22, 0x38);
    private static final Color BORDER_C = new Color(0x2A, 0x3A, 0x55);
    private static final Color FROST    = new Color(0x5E, 0x81, 0xAC);
    private static final Color ORANGE   = new Color(0xD0, 0x87, 0x70);
    private static final Color GREEN    = new Color(0xA3, 0xBE, 0x8C);
    private static final Color GOLD     = new Color(0xEB, 0xCB, 0x8B);
    private static final Color FG       = new Color(0xEC, 0xEF, 0xF4);
    private static final Color SUBTEXT  = new Color(0x6B, 0x74, 0x8A);

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Top bar ───────────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topPanel.setBackground(BG_TOP);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_C, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JLabel digitLbl = new JLabel("Số chữ số:");
        digitLbl.setForeground(FG);
        digitLbl.setFont(new java.awt.Font("Courier New", java.awt.Font.PLAIN, 12));
        topPanel.add(digitLbl);

        digitSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 6, 1));
        digitSpinner.setPreferredSize(new Dimension(52, 28));
        digitSpinner.setBackground(BG_SEC);
        JComponent editor = digitSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(BG_SEC);
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(GOLD);
            ((JSpinner.DefaultEditor) editor).getTextField().setCaretColor(GOLD);
        }
        topPanel.add(digitSpinner);

        JButton createBtn = darkBtn("\uD83C\uDFE0  Tạo Phòng", GREEN);
        createBtn.addActionListener(e -> {
            int d = (Integer) digitSpinner.getValue();
            client.sendMsg("CREATE_ROOM|" + d);
        });
        topPanel.add(createBtn);

        // Separator
        JLabel sep = new JLabel("│");
        sep.setForeground(BORDER_C);
        topPanel.add(sep);

        JLabel codeLbl = new JLabel("Mã phòng:");
        codeLbl.setForeground(FG);
        codeLbl.setFont(new java.awt.Font("Courier New", java.awt.Font.PLAIN, 12));
        topPanel.add(codeLbl);

        roomCodeField = new JTextField("", 7);
        roomCodeField.setBackground(BG_SEC);
        roomCodeField.setForeground(GOLD);
        roomCodeField.setCaretColor(GOLD);
        roomCodeField.setFont(new java.awt.Font("Courier New", java.awt.Font.BOLD, 14));
        roomCodeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FROST, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        roomCodeField.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (!code.isEmpty()) client.sendMsg("JOIN_ROOM|" + code.toUpperCase());
        });
        topPanel.add(roomCodeField);

        JButton joinBtn = darkBtn("\uD83D\uDEAA  Vào Phòng", ORANGE);
        joinBtn.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (!code.isEmpty()) client.sendMsg("JOIN_ROOM|" + code.toUpperCase());
        });
        topPanel.add(joinBtn);
        add(topPanel, BorderLayout.NORTH);

        // ── Chat area ─────────────────────────────────────────────────────
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(BG_SEC);
        chatArea.setForeground(FG);
        chatArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_C, 1),
                " \uD83D\uDCAC  Lobby Chat",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new java.awt.Font("Courier New", java.awt.Font.BOLD, 11),
                FROST));
        scroll.getViewport().setBackground(BG_SEC);
        scroll.setBackground(BG);
        add(scroll, BorderLayout.CENTER);

        // ── Bottom input ──────────────────────────────────────────────────
        JPanel btm = new JPanel(new BorderLayout(6, 0));
        btm.setBackground(BG);
        btm.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        chatField = new JTextField();
        chatField.setBackground(BG_SEC);
        chatField.setForeground(FG);
        chatField.setCaretColor(GOLD);
        chatField.setFont(new java.awt.Font("Courier New", java.awt.Font.PLAIN, 13));
        chatField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_C, 1),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        chatField.addActionListener(e -> sendChat());

        JButton sendBtn = darkBtn("Gửi", FROST);
        sendBtn.addActionListener(e -> sendChat());

        btm.add(chatField, BorderLayout.CENTER);
        btm.add(sendBtn,   BorderLayout.EAST);
        add(btm, BorderLayout.SOUTH);
    }

    private JButton darkBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new java.awt.Font("Courier New", java.awt.Font.BOLD, 12));
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void sendChat() {
        String msg = chatField.getText().trim();
        if (!msg.isEmpty()) {
            client.sendMsg("CHAT|" + msg);
            chatField.setText("");
        }
    }

    public void appendText(String text, Color color) {
        StyledDocument doc = chatArea.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (BadLocationException ignored) {}
        chatArea.setCaretPosition(doc.getLength());
    }

    public void appendJoinButton(String roomCode) {
        JButton joinBtn = new JButton("\u25B6 Join " + roomCode);
        joinBtn.setBackground(new Color(0x5E, 0x81, 0xAC));
        joinBtn.setForeground(Color.WHITE);
        joinBtn.setFocusPainted(false);
        joinBtn.setFont(new java.awt.Font("Courier New", java.awt.Font.BOLD, 11));
        joinBtn.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        joinBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        joinBtn.addActionListener(e -> {
            if (client.getCurrentRoom() == null) {
                client.sendMsg("JOIN_ROOM|" + roomCode);
            } else {
                JOptionPane.showMessageDialog(client,
                        "Bạn đang ở trong phòng rồi!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });
        chatArea.setCaretPosition(chatArea.getStyledDocument().getLength());
        chatArea.insertComponent(joinBtn);
    }
}