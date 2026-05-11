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

        // ── Top bar (2 hàng) ──────────────────────────────────────────────
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        topPanel.setBackground(BG_TOP);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_C, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        // Hàng 1: Tạo phòng (chỉ còn nút, không cần spinner nữa)
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setOpaque(false);

        JButton createBtn = darkBtn("🏠  Tạo Phòng", GREEN);
        createBtn.addActionListener(e -> showCreateRoomDialog());
        row1.add(createBtn);
        topPanel.add(row1);

        // Hàng 2: Vào phòng bằng mã
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);

        JLabel codeLbl = new JLabel("Mã phòng:");
        codeLbl.setForeground(FG);
        codeLbl.setFont(new java.awt.Font("Courier New", java.awt.Font.PLAIN, 12));
        row2.add(codeLbl);

        roomCodeField = new JTextField("", 7);
        roomCodeField.setBackground(BG_SEC);
        roomCodeField.setForeground(GOLD);
        roomCodeField.setCaretColor(GOLD);
        roomCodeField.setFont(new java.awt.Font("Courier New", java.awt.Font.BOLD, 14));
        roomCodeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FROST, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        roomCodeField.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (!code.isEmpty()) client.sendMsg("JOIN_ROOM|" + code.toUpperCase());
        });
        row2.add(roomCodeField);

        JButton joinBtn = darkBtn("🚪  Vào Phòng", ORANGE);
        joinBtn.addActionListener(e -> {
            String code = roomCodeField.getText().trim();
            if (!code.isEmpty()) client.sendMsg("JOIN_ROOM|" + code.toUpperCase());
        });
        row2.add(joinBtn);
        topPanel.add(row2);

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

    private void showCreateRoomDialog() {
        // ── Màu & font dùng trong dialog ──────────────────────────────────
        Color dlgBg    = new Color(0x16, 0x22, 0x38);
        Color dlgCard  = new Color(0x0D, 0x14, 0x26);
        Color accent   = new Color(0x5E, 0x81, 0xAC);
        Font  fntBold  = new java.awt.Font("Courier New", java.awt.Font.BOLD,   13);
        Font  fntPlain = new java.awt.Font("Courier New", java.awt.Font.PLAIN,  12);

        // ── Outer dialog ──────────────────────────────────────────────────
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Tạo Phòng",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setUndecorated(true);
        dlg.setSize(360, 420);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(dlgBg);
        root.setBorder(BorderFactory.createLineBorder(accent, 2));
        dlg.setContentPane(root);

        // ── Title bar ─────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(0x1E, 0x2E, 0x48));
        titleBar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));

        JLabel titleLbl = new JLabel("✚  Tạo Phòng Mới");
        titleLbl.setForeground(GOLD);
        titleLbl.setFont(new java.awt.Font("Courier New", java.awt.Font.BOLD, 14));
        titleBar.add(titleLbl, BorderLayout.WEST);

        JButton closeX = new JButton("✕");
        closeX.setForeground(SUBTEXT);
        closeX.setFont(new java.awt.Font("Courier New", java.awt.Font.BOLD, 13));
        closeX.setOpaque(false); closeX.setContentAreaFilled(false);
        closeX.setBorderPainted(false); closeX.setFocusPainted(false);
        closeX.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeX.addActionListener(e -> dlg.dispose());
        titleBar.add(closeX, BorderLayout.EAST);
        root.add(titleBar, BorderLayout.NORTH);

        // ── Body ──────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(dlgBg);
        body.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));

        // Helper: section label
        java.util.function.Function<String, JLabel> sectionLbl = txt -> {
            JLabel l = new JLabel(txt);
            l.setFont(fntPlain);
            l.setForeground(SUBTEXT);
            l.setAlignmentX(LEFT_ALIGNMENT);
            return l;
        };

        // ── 1. Số chữ số ─────────────────────────────────────────────────
        body.add(sectionLbl.apply("Số chữ số của số bí mật"));
        body.add(Box.createVerticalStrut(8));

        JPanel digitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        digitRow.setOpaque(false);
        digitRow.setAlignmentX(LEFT_ALIGNMENT);
        int[] digitVal = {4};
        JButton[] digitBtns = new JButton[5]; // 2..6
        Color selCol = FROST;
        Color unselCol = dlgCard;
        for (int i = 0; i < 5; i++) {
            int d = i + 2;
            JButton db = new JButton(String.valueOf(d));
            db.setFont(fntBold);
            db.setFocusPainted(false);
            db.setBorderPainted(false);
            db.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            db.setPreferredSize(new Dimension(50, 36));
            db.setBackground(d == 4 ? selCol : unselCol);
            db.setForeground(d == 4 ? Color.WHITE : SUBTEXT);
            digitBtns[i] = db;
            db.addActionListener(ev -> {
                digitVal[0] = d;
                for (int j = 0; j < 5; j++) {
                    boolean sel = (j + 2) == d;
                    digitBtns[j].setBackground(sel ? selCol : unselCol);
                    digitBtns[j].setForeground(sel ? Color.WHITE : SUBTEXT);
                }
            });
            digitRow.add(db);
            if (i < 4) digitRow.add(Box.createHorizontalStrut(4));
        }
        body.add(digitRow);
        body.add(Box.createVerticalStrut(20));

        // ── 2. Giới hạn người chơi ────────────────────────────────────────
        body.add(sectionLbl.apply("Giới hạn người chơi"));
        body.add(Box.createVerticalStrut(8));

        JPanel playerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        playerRow.setOpaque(false);
        playerRow.setAlignmentX(LEFT_ALIGNMENT);
        int[] playerVal = {4};
        String[] playerLabels = {"2", "3", "4", "5", "6", "∞"};
        int[]    playerVals   = {2, 3, 4, 5, 6, 99};
        JButton[] playerBtns  = new JButton[playerLabels.length];
        for (int i = 0; i < playerLabels.length; i++) {
            int pv = playerVals[i];
            JButton pb = new JButton(playerLabels[i]);
            pb.setFont(fntBold);
            pb.setFocusPainted(false);
            pb.setBorderPainted(false);
            pb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pb.setPreferredSize(new Dimension(42, 36));
            pb.setBackground(pv == 4 ? selCol : unselCol);
            pb.setForeground(pv == 4 ? Color.WHITE : SUBTEXT);
            playerBtns[i] = pb;
            pb.addActionListener(ev -> {
                playerVal[0] = pv;
                for (int j = 0; j < playerBtns.length; j++) {
                    boolean sel = playerVals[j] == pv;
                    playerBtns[j].setBackground(sel ? selCol : unselCol);
                    playerBtns[j].setForeground(sel ? Color.WHITE : SUBTEXT);
                }
            });
            playerRow.add(pb);
            if (i < playerLabels.length - 1) playerRow.add(Box.createHorizontalStrut(4));
        }
        body.add(playerRow);
        body.add(Box.createVerticalStrut(20));

        // ── 3. Chế độ chơi ───────────────────────────────────────────────
        body.add(sectionLbl.apply("Chế độ chơi"));
        body.add(Box.createVerticalStrut(8));

        String[][] modes = {
                {"Chế độ 1", "Mỗi người đoán lần lượt  ✔"},
                {"Chế độ 2", "Đoán đồng thời  (sắp ra mắt)"},
                {"Chế độ 3", "Đấu tốc độ  (sắp ra mắt)"}
        };
        int[] modeVal = {1};
        JPanel modePanel = new JPanel();
        modePanel.setLayout(new BoxLayout(modePanel, BoxLayout.Y_AXIS));
        modePanel.setOpaque(false);
        modePanel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel[] modeCards = new JPanel[3];

        for (int i = 0; i < modes.length; i++) {
            int mi = i + 1;
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(mi == 1 ? new Color(0x1E, 0x33, 0x55) : dlgCard);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(mi == 1 ? accent : BORDER_C, mi == 1 ? 2 : 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            card.setAlignmentX(LEFT_ALIGNMENT);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            modeCards[i] = card;

            JLabel nameL = new JLabel(modes[i][0]);
            nameL.setFont(fntBold);
            nameL.setForeground(mi == 1 ? GOLD : FG);

            JLabel descL = new JLabel(modes[i][1]);
            descL.setFont(new java.awt.Font("Courier New", java.awt.Font.PLAIN, 10));
            descL.setForeground(SUBTEXT);

            JPanel txt = new JPanel();
            txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
            txt.setOpaque(false);
            txt.add(nameL); txt.add(descL);

            JLabel dot = new JLabel(mi == 1 ? "●" : "○");
            dot.setFont(new java.awt.Font("Courier New", java.awt.Font.BOLD, 14));
            dot.setForeground(mi == 1 ? GOLD : SUBTEXT);

            card.add(dot, BorderLayout.WEST);
            card.add(txt, BorderLayout.CENTER);

            // Chế độ 2, 3 chưa mở — mờ đi và không cho click
            if (mi != 1) {
                card.setBackground(new Color(0x0D, 0x14, 0x26));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x1A, 0x25, 0x3A), 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                card.setCursor(Cursor.getDefaultCursor());
                nameL.setForeground(new Color(0x35, 0x45, 0x60));
                descL.setForeground(new Color(0x35, 0x45, 0x60));
                dot.setForeground(new Color(0x35, 0x45, 0x60));
            }

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (mi != 1) return; // chỉ chế độ 1 được chọn
                    modeVal[0] = mi;
                    for (int j = 0; j < modeCards.length; j++) {
                        boolean sel = (j + 1) == mi;
                        modeCards[j].setBackground(sel ? new Color(0x1E, 0x33, 0x55) : dlgCard);
                        modeCards[j].setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(sel ? accent : BORDER_C, sel ? 2 : 1),
                                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                        JLabel n = (JLabel)((JPanel)modeCards[j].getComponent(1)).getComponent(0);
                        JLabel dotJ = (JLabel) modeCards[j].getComponent(0);
                        n.setForeground(sel ? GOLD : FG);
                        dotJ.setText(sel ? "●" : "○");
                        dotJ.setForeground(sel ? GOLD : SUBTEXT);
                    }
                }
            });

            modePanel.add(card);
            if (i < modes.length - 1) modePanel.add(Box.createVerticalStrut(6));
        }
        body.add(modePanel);

        root.add(body, BorderLayout.CENTER);

        // ── Footer: nút Tạo ───────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(new Color(0x1E, 0x2E, 0x48));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_C));

        JButton cancelBtn = new JButton("Huỷ");
        cancelBtn.setFont(fntPlain);
        cancelBtn.setForeground(SUBTEXT);
        cancelBtn.setOpaque(false); cancelBtn.setContentAreaFilled(false);
        cancelBtn.setBorderPainted(false); cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dlg.dispose());

        JButton okBtn = darkBtn("✔  Tạo Phòng", GREEN);
        okBtn.addActionListener(e -> {
            // Giữ nguyên format server cũ: CREATE_ROOM|digits
            // maxPlayers và mode sẽ được dùng khi server hỗ trợ sau
            client.sendMsg("CREATE_ROOM|" + digitVal[0]);
            dlg.dispose();
        });

        footer.add(cancelBtn);
        footer.add(okBtn);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setVisible(true);
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