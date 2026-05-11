import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * RoomPanel – dark red/black portrait layout.
 */
public class RoomPanel extends JPanel {

    // ── Theme ─────────────────────────────────────────────────────────────
    static final Color BG       = new Color(0x1A, 0x1A, 0x2E);
    static final Color PANEL_BG = new Color(0x16, 0x21, 0x3E);
    static final Color ENTRY_BG = new Color(0x0F, 0x34, 0x60);
    static final Color ACCENT   = new Color(0xE9, 0x45, 0x60);
    static final Color GOLD     = new Color(0xF5, 0xA6, 0x23);
    static final Color GREEN_C  = new Color(0x4C, 0xAF, 0x50);
    static final Color TEXT     = new Color(0xEA, 0xEA, 0xEA);
    static final Color SUBTEXT  = new Color(0x88, 0x92, 0xA4);

    private final GameClient client;

    // ── UI refs ────────────────────────────────────────────────────────────
    private JLabel     roomInfoLabel, turnLabel, attemptsLabel, timerLabel;
    private JLabel     guessDisplay;
    private JButton    startBtn, setSecretBtn;
    private JTextField secretField;

    // ready system
    private JLabel     mySecretDisplayLabel; // hiển thị "Số của bạn: XXXX" trong game
    private JLabel     readyCountLabel;       // "X/Y sẵn sàng"
    private JLabel     readyDialogCountLabel; // label trong dialog
    private JButton    dialogStartBtn;        // nút Bắt đầu/Sẵn sàng trong dialog
    private boolean    isReady     = false;
    private boolean    isOwner     = false;
    private int        readyCount  = 0;
    private int        totalCount  = 0;

    // recent history (header + up to 2 rows)
    private JPanel     historyPanel;
    private JScrollPane histScrollPane;

    // keypad card switcher
    private NumPad  numPad;
    private JPanel  cardStack;
    private JLabel  opponentLabel;
    private String  guessInput = "";
    private int     numDigits  = 4;

    // chat bubble
    private ChatBubbleButton chatBubble;
    private JDialog          chatDialog;
    private JTextArea        chatArea;       // giữ để clearChat() không lỗi
    private JPanel           bubbleChatPanel;
    private JScrollPane      bubbleChatScroll;
    private JTextField       chatField;
    private JButton          sendChatBtn;
    private int              unreadCount = 0;
    private String           myNickname  = null; // set từ GameClient

    // dialogs
    private JDialog historyDialog;
    private JPanel  histTablePanel;
    private JDialog            trackerDialog;
    private NumberTrackerPanel trackerPanel;
    private JDialog gameOverDialog;
    private JDialog pendingSecretDialog; // dialog chuẩn bị, đóng khi game bắt đầu

    // data
    private List<GuessEntry> guessHistory  = new ArrayList<>();
    private List<GuessEntry> recentGuesses = new ArrayList<>();
    private int              attemptCount  = 0;

    // timer
    private long startTime;
    private javax.swing.Timer swingTimer;

    // ══════════════════════════════════════════════════════════════════════
    public RoomPanel(GameClient client) {
        this.client = client;
        buildUI();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        chatBubble = new ChatBubbleButton();
    }

    private JPanel buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        roomInfoLabel = lbl("Phòng: ---", 12, Font.BOLD, TEXT);
        turnLabel     = lbl("",           12, Font.BOLD, GOLD);
        p.add(roomInfoLabel, BorderLayout.WEST);
        p.add(turnLabel,     BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(BG);
        c.setBorder(BorderFactory.createEmptyBorder(8, 14, 6, 14));
        c.add(buildStatsBar(),  BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setBackground(BG);
        mid.add(buildHistArea(), BorderLayout.NORTH);
        mid.add(buildInputArea(), BorderLayout.CENTER);
        c.add(mid, BorderLayout.CENTER);
        return c;
    }

    private JPanel buildStatsBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ENTRY_BG, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setBackground(PANEL_BG);

        attemptsLabel = lbl("Lần thứ: 0", 11, Font.BOLD, GOLD);
        left.add(attemptsLabel);

        JButton histBtn = mkBtn("[ Lịch sử đoán ]", ENTRY_BG, TEXT, 10);
        histBtn.addActionListener(e -> showHistoryDialog());
        left.add(histBtn);

        JButton trackBtn = mkBtn("[ Theo dõi số ]", new Color(0x20,0x10,0x3A), TEXT, 10);
        trackBtn.addActionListener(e -> showTrackerDialog());
        left.add(trackBtn);

        JButton helpBtn = mkBtn("?", new Color(0x3B,0x42,0x52), new Color(0xEB,0xCB,0x8B), 11);
        helpBtn.setPreferredSize(new java.awt.Dimension(26, 24));
        helpBtn.setToolTipText("Luật chơi");
        helpBtn.addActionListener(e -> showRulesDialog());
        left.add(helpBtn);

        timerLabel = lbl("00:00", 11, Font.BOLD, SUBTEXT);
        bar.add(left,       BorderLayout.WEST);
        bar.add(timerLabel, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildHistArea() {
        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setBackground(PANEL_BG);
        historyPanel.add(makeHistRow("#", "Số đoán", "Bò", "Bê", true));

        histScrollPane = new JScrollPane(historyPanel);
        histScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ENTRY_BG, 1),
                "--- Lịch sử đoán ---",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Courier New", Font.PLAIN, 9), SUBTEXT));
        histScrollPane.setBackground(PANEL_BG);
        histScrollPane.getViewport().setBackground(PANEL_BG);
        histScrollPane.setPreferredSize(new Dimension(0, 200));
        return histScrollPane;
    }

    private JPanel buildInputArea() {
        JPanel area = new JPanel(new BorderLayout(0, 6));
        area.setBackground(BG);

        // Guess display
        guessDisplay = new JLabel("_ _ _ _", SwingConstants.CENTER);
        guessDisplay.setFont(new Font("Courier New", Font.BOLD, 32));
        guessDisplay.setForeground(TEXT);
        guessDisplay.setBackground(ENTRY_BG);
        guessDisplay.setOpaque(true);
        guessDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 2),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        area.add(guessDisplay, BorderLayout.NORTH);

        numPad = new NumPad();

        // opponentLabel nổi phía trên bàn phím khi không phải lượt mình
        opponentLabel = lbl("", 14, Font.ITALIC, SUBTEXT);
        opponentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        opponentLabel.setVisible(false);

        // cardStack chỉ dùng để switch giữa: label đối thủ và ô trống
        // numPad luôn hiển thị bên dưới, chỉ enable/disable
        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
        centerPanel.setBackground(BG);
        centerPanel.add(opponentLabel, BorderLayout.NORTH);

        JPanel padWrapper = new JPanel(new GridBagLayout());
        padWrapper.setBackground(BG);
        padWrapper.add(numPad);
        centerPanel.add(padWrapper, BorderLayout.CENTER);

        // Giữ cardStack field để không lỗi compile, nhưng dùng centerPanel thực sự
        cardStack = new JPanel();
        cardStack.setBackground(BG);

        area.add(centerPanel, BorderLayout.CENTER);
        return area;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ENTRY_BG));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(ENTRY_BG);
        row.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        // Trái: số bí mật của mình
        mySecretDisplayLabel = lbl("Số của bạn: ---", 12, Font.BOLD, GOLD);
        row.add(mySecretDisplayLabel, BorderLayout.WEST);

        // Giữa: HOST: nickname (thay cho đếm sẵn sàng)
        readyCountLabel = lbl("", 11, Font.PLAIN, SUBTEXT);
        readyCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(readyCountLabel, BorderLayout.CENTER);

        // Phải: chỉ nút Rời
        JButton leaveBtn = mkBtn("[ Rời ]", ACCENT, Color.WHITE, 11);
        leaveBtn.addActionListener(e -> client.sendMsg("LEAVE_ROOM|"));
        row.add(leaveBtn, BorderLayout.EAST);

        bar.add(row, BorderLayout.CENTER);

        // Hidden fields giữ để không lỗi compile
        startBtn     = mkBtn("", ENTRY_BG, TEXT, 11); startBtn.setVisible(false);
        secretField  = new JTextField(1);              secretField.setVisible(false);
        setSecretBtn = mkBtn("", ENTRY_BG, TEXT, 11); setSecretBtn.setVisible(false);

        return bar;
    }

    /** Hiển thị "HOST: nickname" ở bottom bar khi vào phòng */
    public void setHostDisplay(String hostNick) {
        if (readyCountLabel != null)
            readyCountLabel.setText("HOST: " + hostNick);
    }

    /** Chỉ set flag, không hiện dialog — gọi khi ROOM_CREATED/JOINED */
    public void setIsOwner(boolean owner) {
        this.isOwner = owner;
        this.isReady = false;
    }

    /** Gọi khi vào phòng — hiện dialog ngay (dùng cho setOwner cũ nếu cần) */
    public void setOwner(boolean owner) {
        this.isOwner = owner;
        this.isReady = false;
        // Không hiện dialog ở đây — chờ GAME_START với numDigits đúng
    }

    /** Cập nhật đếm sẵn sàng và trạng thái nút trong dialog */
    public void updateReadyStatus(int ready, int total) {
        SwingUtilities.invokeLater(() -> {
            // Chỉ cập nhật label trong dialog sẵn sàng
            if (readyDialogCountLabel != null)
                readyDialogCountLabel.setText(ready + "/" + total + " người sẵn sàng");
            // Bật nút Bắt đầu trong dialog khi tất cả đã sẵn sàng
            if (isOwner && dialogStartBtn != null) {
                boolean allReady = total > 1 && ready >= total;
                dialogStartBtn.setEnabled(allReady);
                dialogStartBtn.setBackground(allReady ? GREEN_C : new Color(0x2A, 0x3A, 0x2A));
                dialogStartBtn.setForeground(allReady ? Color.WHITE : Color.GRAY);
                dialogStartBtn.setText(allReady ? "▶  Bắt đầu!" : "[ Chờ mọi người sẵn sàng ]");
            }
        });
    }

    /** Toast 3 giây góc trên phải: "X đã sẵn sàng!" */
    public void showReadyToast(String playerName) {
        SwingUtilities.invokeLater(() -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner == null) return;
            JWindow toast = new JWindow(owner);
            JPanel tp = new JPanel(new BorderLayout());
            tp.setBackground(new Color(0x12, 0x30, 0x12));
            tp.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GREEN_C, 2),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)));
            JLabel msg = new JLabel("✔  " + playerName + " đã sẵn sàng!");
            msg.setFont(new Font("Courier New", Font.BOLD, 12));
            msg.setForeground(GREEN_C);
            tp.add(msg);
            toast.setContentPane(tp);
            toast.pack();
            try {
                Point loc = owner.getLocationOnScreen();
                toast.setLocation(loc.x + owner.getWidth() - toast.getWidth() - 14, loc.y + 44);
            } catch (Exception ignored) {}
            toast.setVisible(true);
            new javax.swing.Timer(3000, ev -> toast.dispose()) {{ setRepeats(false); start(); }};
        });
    }


    public void setMySecretDisplay(String secret) {
        if (mySecretDisplayLabel != null)
            mySecretDisplayLabel.setText("Số của bạn: " + secret);
    }

    /** Đóng dialog sẵn sàng/đặt số */
    public void closeSecretDialog() {
        if (pendingSecretDialog != null) {
            pendingSecretDialog.dispose();
            pendingSecretDialog = null;
        }
        readyDialogCountLabel = null;
        dialogStartBtn = null;
    }

    /** Alias dùng bởi GameClient */
    public void closeReadyDialog() { closeSecretDialog(); }

    /** Hiện dialog sẵn sàng ngay khi vào phòng
     *  owner=true → host (có nút Bắt đầu)
     *  owner=false → guest (có nút Sẵn sàng) */
    public void showReadyDialog(boolean ownerFlag, int digits) {
        this.isOwner   = ownerFlag;
        this.numDigits = digits;
        closeSecretDialog(); // đóng dialog cũ nếu có
        SwingUtilities.invokeLater(this::showSetSecretAndReadyDialog);
    }

    // ── Dialog đặt số + Sẵn sàng (hiện ngay khi vào phòng) ───────────────
    public void showSetSecretAndReadyDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog((Frame) owner, "Chuẩn bị", false);
        dlg.setUndecorated(true);
        dlg.setSize(340, isOwner ? 310 : 290);
        dlg.setLocationRelativeTo(this);
        dlg.setAlwaysOnTop(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PANEL_BG);
        root.setBorder(BorderFactory.createLineBorder(GOLD, 2));

        // ── Header ────────────────────────────────────────────────────────
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(0x25, 0x18, 0x00));
        hdr.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        hdr.add(lbl("Đặt số bí mật", 13, Font.BOLD, GOLD), BorderLayout.WEST);
        JLabel subHdr = isOwner
                ? lbl("Chờ tất cả sẵn sàng để bắt đầu", 10, Font.PLAIN, SUBTEXT)
                : lbl("Nhập số rồi nhấn Sẵn sàng", 10, Font.PLAIN, SUBTEXT);
        hdr.add(subHdr, BorderLayout.EAST);
        root.add(hdr, BorderLayout.NORTH);

        // ── Body ──────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(PANEL_BG);
        body.setBorder(BorderFactory.createEmptyBorder(14, 24, 8, 24));

        JLabel hint = lbl(numDigits + " chữ số (1–9), không trùng", 11, Font.PLAIN, SUBTEXT);
        hint.setAlignmentX(CENTER_ALIGNMENT);
        body.add(hint);
        body.add(Box.createVerticalStrut(10));

        JTextField secIn = new JTextField(10);
        secIn.setBackground(ENTRY_BG);
        secIn.setForeground(TEXT);
        secIn.setCaretColor(GOLD);
        secIn.setFont(new Font("Courier New", Font.BOLD, 26));
        secIn.setHorizontalAlignment(JTextField.CENTER);
        secIn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        secIn.setMaximumSize(new Dimension(250, 58));
        secIn.setAlignmentX(CENTER_ALIGNMENT);
        body.add(secIn);
        body.add(Box.createVerticalStrut(8));

        JLabel errLbl = lbl("", 10, Font.PLAIN, ACCENT);
        errLbl.setAlignmentX(CENTER_ALIGNMENT);
        body.add(errLbl);
        body.add(Box.createVerticalStrut(6));

        // Đếm sẵn sàng
        readyDialogCountLabel = lbl("0/0 người sẵn sàng", 11, Font.BOLD, SUBTEXT);
        readyDialogCountLabel.setAlignmentX(CENTER_ALIGNMENT);
        body.add(readyDialogCountLabel);

        root.add(body, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setBackground(new Color(0x10, 0x1A, 0x28));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        if (isOwner) {
            // Host: nút "Bắt đầu" (disabled cho đến khi tất cả sẵn sàng)
            dialogStartBtn = new JButton("[ Bắt đầu ]");
            dialogStartBtn.setFont(new Font("Courier New", Font.BOLD, 13));
            dialogStartBtn.setBackground(new Color(0x2A, 0x3A, 0x2A));
            dialogStartBtn.setForeground(Color.GRAY);
            dialogStartBtn.setFocusPainted(false);
            dialogStartBtn.setBorderPainted(false);
            dialogStartBtn.setEnabled(false);
            dialogStartBtn.setPreferredSize(new Dimension(0, 40));
            dialogStartBtn.addActionListener(e -> {
                // Chỉ gửi lệnh, dialog sẽ tự đóng khi nhận GAME_START từ server
                dialogStartBtn.setEnabled(false);
                dialogStartBtn.setText("Đang bắt đầu...");
                client.sendMsg("START_GAME|");
            });
            footer.add(dialogStartBtn, BorderLayout.CENTER);

            // Host cũng cần nút xác nhận số bí mật (riêng)
            JButton hostConfBtn = new JButton("Xác nhận số bí mật  ✔");
            hostConfBtn.setFont(new Font("Courier New", Font.BOLD, 12));
            hostConfBtn.setBackground(new Color(0x5E, 0x81, 0xAC));
            hostConfBtn.setForeground(Color.WHITE);
            hostConfBtn.setFocusPainted(false);
            hostConfBtn.setBorderPainted(false);
            hostConfBtn.setPreferredSize(new Dimension(0, 36));
            hostConfBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            footer.add(hostConfBtn, BorderLayout.NORTH);

            Runnable submitHost = () -> {
                String s = secIn.getText().trim();
                if (s.length() != numDigits || !s.matches("[1-9]+")) {
                    errLbl.setForeground(ACCENT); errLbl.setText("Số không hợp lệ!"); return; }
                Set<Character> seen = new HashSet<>();
                for (char c : s.toCharArray()) if (!seen.add(c)) {
                    errLbl.setForeground(ACCENT); errLbl.setText("Chữ số không được trùng!"); return; }
                client.sendMsg("SET_SECRET|" + s);
                client.sendMsg("PLAYER_READY|"); // host cũng gửi sẵn sàng để đếm
                mySecretDisplayLabel.setText("Số của bạn: " + s);
                secIn.setEditable(false);
                secIn.setForeground(GREEN_C);
                hostConfBtn.setEnabled(false);
                subHdr.setText("Chờ người chơi sẵn sàng...");
                errLbl.setForeground(GREEN_C); errLbl.setText("✔ Đã đặt số — chờ mọi người sẵn sàng");
                // KHÔNG đóng dialog — chờ host tự bấm Bắt đầu
            };
            hostConfBtn.addActionListener(e -> submitHost.run());
            secIn.addActionListener(e -> submitHost.run());

        } else {
            // Guest: nút "Sẵn sàng" + nút "Hủy sẵn sàng"
            JButton readyBtn = new JButton("✔  Sẵn sàng");
            readyBtn.setFont(new Font("Courier New", Font.BOLD, 13));
            readyBtn.setBackground(GREEN_C);
            readyBtn.setForeground(Color.WHITE);
            readyBtn.setFocusPainted(false);
            readyBtn.setBorderPainted(false);
            readyBtn.setPreferredSize(new Dimension(0, 40));
            readyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            dialogStartBtn = readyBtn; // ref để enable/disable

            JButton cancelBtn = mkBtn("Hủy sẵn sàng", ACCENT, Color.WHITE, 12);
            cancelBtn.setPreferredSize(new Dimension(0, 34));
            cancelBtn.setVisible(false);

            Runnable submitGuest = () -> {
                String s = secIn.getText().trim();
                if (s.length() != numDigits || !s.matches("[1-9]+")) {
                    errLbl.setForeground(ACCENT); errLbl.setText("Số không hợp lệ!"); return; }
                Set<Character> seen = new HashSet<>();
                for (char c : s.toCharArray()) if (!seen.add(c)) {
                    errLbl.setForeground(ACCENT); errLbl.setText("Chữ số không được trùng!"); return; }
                client.sendMsg("SET_SECRET|" + s);
                client.sendMsg("PLAYER_READY|");
                mySecretDisplayLabel.setText("Số của bạn: " + s);
                secIn.setEditable(false);
                secIn.setForeground(GREEN_C);
                readyBtn.setVisible(false);
                cancelBtn.setVisible(true);
                subHdr.setText("Đang chờ host bắt đầu...");
                subHdr.setForeground(GREEN_C);
                errLbl.setForeground(GREEN_C); errLbl.setText("✔ Đã sẵn sàng!");
                isReady = true;
            };
            readyBtn.addActionListener(e -> submitGuest.run());
            secIn.addActionListener(e -> submitGuest.run());

            cancelBtn.addActionListener(e -> {
                client.sendMsg("PLAYER_UNREADY|");
                isReady = false;
                secIn.setEditable(true);
                secIn.setForeground(TEXT);
                readyBtn.setVisible(true);
                cancelBtn.setVisible(false);
                subHdr.setText("Nhập số rồi nhấn Sẵn sàng");
                subHdr.setForeground(SUBTEXT);
                errLbl.setText("");
            });

            footer.add(readyBtn,  BorderLayout.CENTER);
            footer.add(cancelBtn, BorderLayout.SOUTH);
        }

        root.add(footer, BorderLayout.SOUTH);

        this.pendingSecretDialog = dlg;
        dlg.setContentPane(root);
        dlg.setVisible(true);
        secIn.requestFocusInWindow();
    }

    /** Hiện dialog đặt số bí mật đơn giản — gọi sau GAME_START */
    public void showSetSecretDialog() {
        SwingUtilities.invokeLater(() -> {
            closeSecretDialog(); // đóng dialog lobby nếu còn mở
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog dlg = new JDialog((Frame) owner, "Đặt số bí mật", false);
            dlg.setUndecorated(true);
            dlg.setSize(320, 240);
            dlg.setLocationRelativeTo(this);
            dlg.setAlwaysOnTop(true);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(PANEL_BG);
            root.setBorder(BorderFactory.createLineBorder(GOLD, 2));

            // Header
            JPanel hdr = new JPanel(new BorderLayout());
            hdr.setBackground(new Color(0x25, 0x18, 0x00));
            hdr.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            hdr.add(lbl("Đặt số bí mật", 13, Font.BOLD, GOLD), BorderLayout.WEST);
            JLabel subHdr = lbl("Nhập " + numDigits + " chữ số, không trùng, không có 0", 10, Font.PLAIN, SUBTEXT);
            hdr.add(subHdr, BorderLayout.SOUTH);
            root.add(hdr, BorderLayout.NORTH);

            // Body
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(PANEL_BG);
            body.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));

            JTextField secIn = new JTextField(10);
            secIn.setBackground(ENTRY_BG);
            secIn.setForeground(GOLD);
            secIn.setCaretColor(GOLD);
            secIn.setFont(new Font("Courier New", Font.BOLD, 28));
            secIn.setHorizontalAlignment(JTextField.CENTER);
            secIn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GOLD, 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            secIn.setMaximumSize(new Dimension(260, 62));
            secIn.setAlignmentX(CENTER_ALIGNMENT);
            body.add(secIn);
            body.add(Box.createVerticalStrut(10));

            JLabel errLbl = lbl("", 10, Font.PLAIN, ACCENT);
            errLbl.setAlignmentX(CENTER_ALIGNMENT);
            body.add(errLbl);
            root.add(body, BorderLayout.CENTER);

            // Footer: nút Xác nhận
            JButton confirmBtn = new JButton("Xác nhận  ✔");
            confirmBtn.setFont(new Font("Courier New", Font.BOLD, 13));
            confirmBtn.setBackground(GREEN_C);
            confirmBtn.setForeground(Color.WHITE);
            confirmBtn.setFocusPainted(false);
            confirmBtn.setBorderPainted(false);
            confirmBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            confirmBtn.setPreferredSize(new Dimension(0, 42));

            JPanel footer = new JPanel(new BorderLayout());
            footer.setBackground(new Color(0x10, 0x1A, 0x28));
            footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0, ENTRY_BG));
            footer.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            footer.add(confirmBtn, BorderLayout.CENTER);
            root.add(footer, BorderLayout.SOUTH);

            Runnable submit = () -> {
                String s = secIn.getText().trim();
                if (s.length() != numDigits) { errLbl.setText("Cần đúng " + numDigits + " chữ số!"); return; }
                if (!s.matches("[1-9]+"))    { errLbl.setText("Chỉ số 1-9, không có số 0!"); return; }
                Set<Character> seen = new HashSet<>();
                for (char c : s.toCharArray()) if (!seen.add(c)) { errLbl.setText("Chữ số không được trùng!"); return; }
                // Gửi lên server
                client.sendMsg("SET_SECRET|" + s);
                // Cập nhật UI ngay
                mySecretDisplayLabel.setText("Số của bạn: " + s);
                secIn.setEditable(false);
                secIn.setForeground(GREEN_C);
                errLbl.setForeground(GREEN_C);
                errLbl.setText("✔ Đã gửi, chờ người khác...");
                confirmBtn.setEnabled(false);
                // Dialog sẽ bị đóng khi nhận SECRET_SET từ server
            };

            confirmBtn.addActionListener(e -> submit.run());
            secIn.addActionListener(e -> submit.run());

            this.pendingSecretDialog = dlg;
            dlg.setContentPane(root);
            dlg.setVisible(true);
            secIn.requestFocusInWindow();
        });
    }
    // ══════════════════════════════════════════════════════════════════════
    //  CHAT
    // ══════════════════════════════════════════════════════════════════════
    void toggleChatDialog() {
        if (chatDialog == null) buildChatWindow();
        if (!chatDialog.isVisible()) {
            unreadCount = 0;
            chatBubble.setUnread(0);
            try {
                Point p = chatBubble.getLocationOnScreen();
                chatDialog.setLocation(p.x - chatDialog.getWidth() - 10,
                        p.y - chatDialog.getHeight());
            } catch (Exception ignored) {}
            chatDialog.setVisible(true);
        } else {
            chatDialog.setVisible(false);
        }
    }

    private void buildChatWindow() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        chatDialog = new JDialog((Frame) owner, false);
        chatDialog.setUndecorated(true);
        chatDialog.setSize(300, 360);

        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(PANEL_BG);
        c.setBorder(BorderFactory.createLineBorder(ACCENT, 2));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(ENTRY_BG);
        hdr.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        hdr.add(lbl("Chat phòng", 13, Font.BOLD, TEXT), BorderLayout.WEST);
        JButton x = mkBtn("[X]", ACCENT, Color.WHITE, 11);
        x.addActionListener(e -> chatDialog.setVisible(false));
        hdr.add(x, BorderLayout.EAST);
        c.add(hdr, BorderLayout.NORTH);

        // Bubble chat panel
        bubbleChatPanel = new JPanel();
        bubbleChatPanel.setLayout(new BoxLayout(bubbleChatPanel, BoxLayout.Y_AXIS));
        bubbleChatPanel.setBackground(BG);
        bubbleChatPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        bubbleChatScroll = new JScrollPane(bubbleChatPanel);
        bubbleChatScroll.setBorder(null);
        bubbleChatScroll.getViewport().setBackground(BG);
        bubbleChatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        c.add(bubbleChatScroll, BorderLayout.CENTER);

        JPanel inp = new JPanel(new BorderLayout(4, 0));
        inp.setBackground(ENTRY_BG);
        inp.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        chatField = new JTextField();
        chatField.setBackground(BG);
        chatField.setForeground(TEXT);
        chatField.setCaretColor(ACCENT);
        chatField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chatField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        chatField.addActionListener(e -> sendRoomChat());
        sendChatBtn = mkBtn("Gửi", ACCENT, Color.WHITE, 12);
        sendChatBtn.addActionListener(e -> sendRoomChat());
        inp.add(chatField,   BorderLayout.CENTER);
        inp.add(sendChatBtn, BorderLayout.EAST);
        c.add(inp, BorderLayout.SOUTH);

        chatDialog.setContentPane(c);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HISTORY DIALOG
    // ══════════════════════════════════════════════════════════════════════
    private void showHistoryDialog() {
        if (historyDialog == null || !historyDialog.isDisplayable()) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            historyDialog = new JDialog((Frame) owner, "Lịch sử đoán", false);
            historyDialog.setSize(420, 400);
            historyDialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(BG);
            JPanel hdr = new JPanel();
            hdr.setBackground(ENTRY_BG);
            hdr.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            hdr.add(lbl("--- Lịch sử đoán số ---", 14, Font.BOLD, ACCENT));
            panel.add(hdr, BorderLayout.NORTH);

            histTablePanel = new JPanel();
            histTablePanel.setLayout(new BoxLayout(histTablePanel, BoxLayout.Y_AXIS));
            histTablePanel.setBackground(PANEL_BG);
            histTablePanel.add(makeHistRow("#", "Số đoán", "Bò", "Bê", true));
            for (GuessEntry g : guessHistory)
                histTablePanel.add(makeHistRow(
                        String.valueOf(g.attempt), g.guess,
                        "B:" + g.bull, "C:" + g.cow, false));

            JScrollPane sp = new JScrollPane(histTablePanel);
            sp.setBorder(null);
            sp.getViewport().setBackground(PANEL_BG);
            panel.add(sp, BorderLayout.CENTER);
            historyDialog.setContentPane(panel);
        }
        historyDialog.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TRACKER DIALOG
    // ══════════════════════════════════════════════════════════════════════
    private void showTrackerDialog() {
        // trackerPanel đã được tạo sẵn trong setNumDigits
        if (trackerPanel == null) trackerPanel = new NumberTrackerPanel(numDigits);
        if (trackerDialog == null || !trackerDialog.isDisplayable()) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            trackerDialog = new JDialog((Frame) owner, "Bộ theo dõi số", false);
            Dimension ps  = trackerPanel.getPreferredSize();
            trackerDialog.setSize(ps.width + 24, ps.height + 46);
            trackerDialog.setLocationRelativeTo(this);
            trackerDialog.setContentPane(trackerPanel);
        }
        trackerDialog.setVisible(true);
    }

    private void showRulesDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog((Frame) owner, "Luật chơi", true);
        dlg.setUndecorated(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(0x2E, 0x34, 0x40));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        // Tiêu đề
        JLabel title = new JLabel("🎮  Luật chơi Bulls & Cows");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(new Color(0xEB, 0xCB, 0x8B));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(14));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x4C, 0x56, 0x6A));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(14));

        // Nội dung luật
        String[][] rules = {
                {"🐂  Bulls", "Số đúng VÀ đúng vị trí."},
                {"🐄  Cows",  "Số đúng nhưng SAI vị trí."},
        };
        for (String[] r : rules) {
            JPanel row = new JPanel(new BorderLayout(14, 0));
            row.setBackground(new Color(0x3B, 0x42, 0x52));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x4C,0x56,0x6A), 1),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            JLabel key = new JLabel(r[0]);
            key.setFont(new Font("SansSerif", Font.BOLD, 13));
            key.setForeground(new Color(0x88, 0xC0, 0xD0));
            key.setPreferredSize(new Dimension(90, 20));
            JLabel val = new JLabel(r[1]);
            val.setFont(new Font("SansSerif", Font.PLAIN, 13));
            val.setForeground(new Color(0xEC, 0xEF, 0xF4));
            row.add(key, BorderLayout.WEST);
            row.add(val, BorderLayout.CENTER);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(row);
            panel.add(Box.createVerticalStrut(7));
        }

        panel.add(Box.createVerticalStrut(8));

        // Ví dụ
        JLabel exTitle = new JLabel("Ví dụ: Số bí mật là  1234");
        exTitle.setFont(new Font("Courier New", Font.BOLD, 12));
        exTitle.setForeground(new Color(0xA3, 0xBE, 0x8C));
        exTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(exTitle);
        panel.add(Box.createVerticalStrut(8));

        String[][] examples = {
                {"Đoán: 1356", "→  1 Bull,  1 Cow"},
                {"Đoán: 5678", "→  0 Bulls, 0 Cows"},
                {"Đoán: 1243", "→  2 Bulls, 2 Cows"},
        };
        for (String[] ex : examples) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            row.setBackground(new Color(0x2E, 0x34, 0x40));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel guess = new JLabel(ex[0]);
            guess.setFont(new Font("Courier New", Font.PLAIN, 12));
            guess.setForeground(new Color(0xD0, 0x87, 0x70));
            JLabel result = new JLabel(ex[1]);
            result.setFont(new Font("Courier New", Font.PLAIN, 12));
            result.setForeground(new Color(0xE5, 0xE9, 0xF0));
            row.add(guess); row.add(result);
            panel.add(row);
        }

        panel.add(Box.createVerticalStrut(18));

        // Nút đóng
        JButton close = mkBtn("Đã hiểu  ✓", new Color(0x5E,0x81,0xAC), Color.WHITE, 13);
        close.setAlignmentX(Component.CENTER_ALIGNMENT);
        close.addActionListener(e -> dlg.dispose());
        panel.add(close);

        dlg.setContentPane(panel);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(340, dlg.getHeight()));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GAME-OVER DIALOG
    // ══════════════════════════════════════════════════════════════════════
    public void showGameOverDialog(List<String> rankings, Map<String, String> secrets) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        gameOverDialog = new JDialog((Frame) owner, "Kết quả", true);
        gameOverDialog.setSize(400, 340);
        gameOverDialog.setLocationRelativeTo(this);
        gameOverDialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(GOLD, 2));

        JPanel hdr = new JPanel();
        hdr.setBackground(new Color(0x30,0x20,0x00));
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));
        JLabel t = lbl("== GAME KẾT THÚC ==", 18, Font.BOLD, GOLD);
        t.setHorizontalAlignment(SwingConstants.CENTER);
        hdr.add(t);
        panel.add(hdr, BorderLayout.NORTH);

        JPanel ranks = new JPanel();
        ranks.setLayout(new BoxLayout(ranks, BoxLayout.Y_AXIS));
        ranks.setBackground(PANEL_BG);
        ranks.setBorder(BorderFactory.createEmptyBorder(10,18,10,18));

        String[] medals = {"#1 NHẤT","#2 NHÌ","#3 BA"};
        for (int i = 0; i < rankings.size(); i++) {
            String player = rankings.get(i);
            String medal  = i < medals.length ? medals[i] : "#"+(i+1);
            String sec    = secrets != null ? secrets.getOrDefault(player,"???") : "???";

            JPanel row = new JPanel(new BorderLayout(8,0));
            row.setBackground(i==0 ? new Color(0x2A,0x1A,0x00) : ENTRY_BG);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0,0,2,0,BG),
                    BorderFactory.createEmptyBorder(8,12,8,12)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

            row.add(lbl(medal+"  "+player, 14, Font.BOLD, i==0?GOLD:TEXT), BorderLayout.WEST);
            row.add(lbl("Số bí mật: "+sec, 12, Font.PLAIN, SUBTEXT),       BorderLayout.EAST);
            ranks.add(row);
            ranks.add(Box.createVerticalStrut(3));
        }

        JScrollPane sp = new JScrollPane(ranks);
        sp.setBorder(null);
        sp.getViewport().setBackground(PANEL_BG);
        panel.add(sp, BorderLayout.CENTER);

        JPanel btm = new JPanel();
        btm.setBackground(PANEL_BG);
        btm.setBorder(BorderFactory.createEmptyBorder(8,0,10,0));
        JButton close = mkBtn("  Dong  ", ACCENT, Color.WHITE, 13);
        close.addActionListener(e -> gameOverDialog.dispose());
        btm.add(close);
        panel.add(btm, BorderLayout.SOUTH);

        gameOverDialog.setContentPane(panel);
        gameOverDialog.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════════════════
    public void setNumDigits(int n) {
        numDigits = n; numPad.setNumDigits(n); updateGuessDisplay();
        // Tạo trackerPanel NGAY khi biết số chữ số — không chờ user mở dialog
        trackerPanel = new NumberTrackerPanel(n);
        // Reset dialog cũ nếu có
        if (trackerDialog != null) { trackerDialog.dispose(); trackerDialog = null; }
    }
    public void setRoomInfo(String t)       { roomInfoLabel.setText(t); }
    public void setTurnText(String t)       { turnLabel.setText(t); }
    public void setStartEnabled(boolean b)  { startBtn.setEnabled(b); }
    public void setSecretEnabled(boolean b) { setSecretBtn.setEnabled(b); }
    public void setSecretEditable(boolean b){ secretField.setEditable(b); }
    public void focusGuess()                { numPad.requestFocus(); }

    public void setGuessEnabled(boolean b) {
        if (b) switchCard("keypad");
        else   numPad.setEnabled(false);
    }
    public void setGuessEditable(boolean b) { numPad.setEnabled(b); }
    public void setChatEnabled(boolean b) {
        if (chatField   != null) chatField.setEditable(b);
        if (sendChatBtn != null) sendChatBtn.setEnabled(b);
    }

    public void setMyNickname(String nick)   { this.myNickname = nick; }

    public void clearChat() {
        if (bubbleChatPanel != null) {
            bubbleChatPanel.removeAll();
            bubbleChatPanel.revalidate();
            bubbleChatPanel.repaint();
        }
    }

    /** Tin nhắn chat thường (trái = người khác, phải = mình) */
    public void appendChatBubble(String sender, String text, boolean isMe) {
        if (bubbleChatPanel == null) buildChatWindow();
        addBubble(sender, text, isMe);
        scrollChatToBottom();
        if (chatDialog == null || !chatDialog.isVisible()) {
            unreadCount++;
            chatBubble.setUnread(unreadCount);
        }
    }

    /** Tin nhắn hệ thống — hiển thị giữa, màu mờ */
    public void appendChatSystem(String text) {
        if (bubbleChatPanel == null) buildChatWindow();
        JLabel sys = new JLabel(text, SwingConstants.CENTER);
        sys.setFont(new Font("SansSerif", Font.ITALIC, 10));
        sys.setForeground(SUBTEXT);
        sys.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        sys.setAlignmentX(Component.CENTER_ALIGNMENT);
        bubbleChatPanel.add(sys);
        bubbleChatPanel.add(Box.createVerticalStrut(2));
        scrollChatToBottom();
    }

    /** Giữ tương thích — GameClient cũ gọi appendChat */
    public void appendChat(String text) {
        appendChatBubble("", text.trim(), false);
    }

    private void addBubble(String sender, String text, boolean isMe) {
        Color bubbleBg  = isMe ? new Color(0x5E,0x81,0xAC) : new Color(0x0F,0x34,0x60);
        Color bubbleFg  = Color.WHITE;

        // Wrapper panel căn trái/phải
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Nội dung bubble
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(bubbleBg);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.EmptyBorder(0,0,0,0),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        // Bo góc bằng custom paint
        bubble = new RoundedPanel(bubbleBg, 12);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        if (!sender.isEmpty() && !isMe) {
            JLabel nameLabel = new JLabel(sender);
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
            nameLabel.setForeground(GOLD);
            bubble.add(nameLabel);
        }
        JLabel msgLabel = new JLabel("<html><body style='width:160px'>" +
                text.replace("<","&lt;") + "</body></html>");
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        msgLabel.setForeground(bubbleFg);
        bubble.add(msgLabel);

        if (isMe) {
            wrapper.add(Box.createHorizontalGlue());
            wrapper.add(bubble);
        } else {
            wrapper.add(bubble);
            wrapper.add(Box.createHorizontalGlue());
        }

        bubbleChatPanel.add(wrapper);
        bubbleChatPanel.add(Box.createVerticalStrut(2));
        bubbleChatPanel.revalidate();
        bubbleChatPanel.repaint();
    }

    private void scrollChatToBottom() {
        SwingUtilities.invokeLater(() -> {
            if (bubbleChatScroll != null) {
                JScrollBar sb = bubbleChatScroll.getVerticalScrollBar();
                sb.setValue(sb.getMaximum());
            }
        });
    }

    public void setMyTurnActive() {
        numPad.setAnimScale(1f);
        switchCard("keypad");
    }

    public void setOpponentTurn(String name) {
        opponentLabel.setText(name + " đang đoán...");
        numPad.setEnabled(false);
        switchCard("opponent");
    }

    public void addGuessResult(String g, int bull, int cow) {
        attemptCount++;
        GuessEntry e = new GuessEntry(attemptCount, g, bull, cow);
        guessHistory.add(e);
        refreshHistoryPanel();

        if (historyDialog != null && histTablePanel != null && historyDialog.isVisible()) {
            histTablePanel.add(makeHistRow(
                    String.valueOf(e.attempt), e.guess,
                    "B:"+e.bull, "C:"+e.cow, false));
            histTablePanel.revalidate();
        }
        attemptsLabel.setText("Lần thứ: " + attemptCount);
        if (trackerPanel != null) updateTracker(g, bull, cow);
    }

    public void addGuessToTracker(String g, int bull, int cow) {
        if (trackerPanel != null) updateTracker(g, bull, cow);
    }

    public void resetForNewGame() {
        guessHistory.clear(); recentGuesses.clear();
        attemptCount = 0; guessInput = "";
        refreshHistoryPanel();
        attemptsLabel.setText("Lần thứ: 0");
        updateGuessDisplay();
        if (trackerPanel != null) trackerPanel.resetAll();
        // Disable bàn phím khi chờ đặt số bí mật
        opponentLabel.setVisible(false);
        numPad.setEnabled(false);
        startGameTimer();
    }

    /** Gọi khi rời phòng/game kết thúc — reset hoàn toàn mọi trạng thái */
    public void fullReset() {
        // Reset dữ liệu ván chơi
        guessHistory.clear(); recentGuesses.clear();
        attemptCount = 0; guessInput = "";
        refreshHistoryPanel();
        attemptsLabel.setText("Lần thứ: 0");
        updateGuessDisplay();

        // Reset tracker
        if (trackerPanel != null) { trackerPanel.resetAll(); }
        if (trackerDialog != null) { trackerDialog.dispose(); trackerDialog = null; }
        trackerPanel = null;  // sẽ tạo lại khi GAME_START với numDigits mới

        // Reset chat bubble
        clearChat();

        // Reset UI state
        setTurnText("");
        setRoomInfo("Phòng: ---");
        setStartEnabled(true);
        setSecretEnabled(false);
        setGuessEnabled(false);
        setSecretEditable(true);
        secretField.setText("");
        if (mySecretDisplayLabel != null) mySecretDisplayLabel.setText("Số của bạn: ---");
        if (readyCountLabel != null) readyCountLabel.setText("");
        isReady = false;
        closeSecretDialog();

        // Reset label đối thủ
        opponentLabel.setText("Đang chờ đối thủ...");

        // Ẩn label đối thủ, disable bàn phím (nhưng vẫn hiển thị)
        opponentLabel.setVisible(false);
        numPad.setEnabled(false);

        // Dừng timer
        stopGameTimer();
    }

    public ChatBubbleButton getChatBubble() { return chatBubble; }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private void switchCard(String name) {
        // "keypad"  → bàn phím bật, label ẩn
        // "opponent"→ bàn phím tắt, label hiện
        // "idle"    → bàn phím tắt, label ẩn
        boolean showLabel  = name.equals("opponent");
        boolean enablePad  = name.equals("keypad");
        opponentLabel.setVisible(showLabel);
        numPad.setEnabled(enablePad);
    }

    private void animateKeypad(boolean show) {
        if (numPad == null) return;
        if (show) {
            switchCard("keypad");
            numPad.setAnimScale(0f);
            final int[] s = {0};
            javax.swing.Timer t = new javax.swing.Timer(14, null);
            t.addActionListener(e -> {
                s[0]++; numPad.setAnimScale(Math.min(1f, s[0]/10f));
                if (s[0] >= 10) { t.stop(); numPad.setAnimScale(1f); }
            });
            t.start();
        } else {
            final int[] s = {10};
            javax.swing.Timer t = new javax.swing.Timer(14, null);
            t.addActionListener(e -> {
                s[0]--; numPad.setAnimScale(Math.max(0f, s[0]/10f));
                if (s[0] <= 0) { t.stop(); switchCard("opponent"); numPad.setAnimScale(1f); }
            });
            t.start();
        }
    }

    private void updateGuessDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numDigits; i++) {
            sb.append(i < guessInput.length() ? guessInput.charAt(i) : '_');
            if (i < numDigits-1) sb.append(' ');
        }
        guessDisplay.setText(sb.toString());
    }

    private void refreshHistoryPanel() {
        while (historyPanel.getComponentCount() > 1) historyPanel.remove(1);
        for (GuessEntry e : guessHistory)
            historyPanel.add(makeHistRow(
                    String.valueOf(e.attempt), e.guess,
                    "B:"+e.bull, "C:"+e.cow, false));
        historyPanel.revalidate(); historyPanel.repaint();
        // Auto-scroll xuống dòng mới nhất
        if (histScrollPane != null) {
            SwingUtilities.invokeLater(() -> {
                JScrollBar sb = histScrollPane.getVerticalScrollBar();
                sb.setValue(sb.getMaximum());
            });
        }
    }

    private JPanel makeHistRow(String n, String g, String b, String c, boolean header) {
        JPanel row = new JPanel(new GridLayout(1,4,0,0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, header?28:30));
        row.setBackground(header ? ENTRY_BG : PANEL_BG);
        row.setBorder(BorderFactory.createMatteBorder(0,0,1,0, ENTRY_BG));

        String[] vals = {n, g, b, c};
        Color[] clrs  = {
                SUBTEXT,
                header ? SUBTEXT : TEXT,
                header ? SUBTEXT : (b.equals("B:0") ? SUBTEXT : GREEN_C),
                header ? SUBTEXT : (c.equals("C:0") ? SUBTEXT : GOLD)
        };
        Font f = new Font("Courier New", Font.BOLD, header?10:14);
        for (int i = 0; i < 4; i++) {
            JLabel l = new JLabel(vals[i], SwingConstants.CENTER);
            l.setFont(f); l.setForeground(clrs[i]);
            l.setBorder(BorderFactory.createMatteBorder(0,0,0,1,ENTRY_BG));
            row.add(l);
        }
        return row;
    }

    private void updateTracker(String guess, int bull, int cow) {
        if (trackerPanel == null) return;
        int total = bull + cow;

        // ── CASE 1: Bò+Bê = 0 ───────────────────────────────────────────────
        // Không có chữ số nào trong guess tồn tại trong số bí mật
        // → Gạch TOÀN BỘ các chữ số đó ở MỌI vị trí
        if (total == 0) {
            for (char ch : guess.toCharArray())
                trackerPanel.crossAllPositions(ch);
            trackerPanel.repaint();
            return;
        }

        // ── CASE 2: Bò+Bê = numDigits ────────────────────────────────────────
        // TẤT CẢ chữ số trong guess đều có trong số bí mật, chỉ khác vị trí
        // → Các chữ số NGOÀI guess chắc chắn KHÔNG có trong số bí mật
        //   (vì số bí mật chỉ có numDigits chữ số, tất cả đã nằm trong guess)
        if (total == numDigits) {
            // Gạch mọi chữ số 1-9 mà KHÔNG có trong guess, ở TẤT CẢ vị trí
            Set<Character> inGuess = new HashSet<>();
            for (char c : guess.toCharArray()) inGuess.add(c);
            for (char c = '1'; c <= '9'; c++) {
                if (!inGuess.contains(c))
                    trackerPanel.crossAllPositions(c);
            }
            // Nếu bull = 0 → TẤT CẢ là Bê → mỗi chữ số sai vị trí hiện tại
            // → Gạch guess[i] tại cột i (sai vị trí i chắc chắn)
            if (bull == 0) {
                for (int i = 0; i < guess.length() && i < numDigits; i++)
                    trackerPanel.markTried(i, guess.charAt(i), false);
            }
            trackerPanel.repaint();
            return;
        }

        // ── CASE 3: 0 < Bò+Bê < numDigits ───────────────────────────────────
        // Một phần chữ số trong guess có trong số bí mật, một phần không
        // Ta KHÔNG biết chữ nào là "có" hay "không" → không thể gạch chắc chắn
        // NGOẠI LỆ: nếu có chữ số xuất hiện nhiều lần trong guess (ví dụ "1123")
        // có thể suy luận thêm — nhưng với game này guess thường không trùng chữ số
        // → Không gạch gì để tránh thông tin sai
        trackerPanel.repaint();
    }

    private void sendRoomChat() {
        if (chatField == null) return;
        String msg = chatField.getText().trim();
        if (!msg.isEmpty()) { client.sendMsg("ROOM_CHAT|"+msg); chatField.setText(""); }
    }

    private void startGameTimer() {
        startTime = System.currentTimeMillis();
        if (swingTimer != null) swingTimer.stop();
        swingTimer = new javax.swing.Timer(1000, e -> {
            long el = (System.currentTimeMillis()-startTime)/1000;
            timerLabel.setText(String.format("%02d:%02d", el/60, el%60));
        });
        swingTimer.start();
    }

    private void stopGameTimer() {
        if (swingTimer != null) { swingTimer.stop(); swingTimer = null; }
        timerLabel.setText("00:00");
    }

    // ── widget factories ───────────────────────────────────────────────────
    private JLabel lbl(String text, int size, int style, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Courier New", style, size));
        l.setForeground(fg);
        return l;
    }

    private JButton mkBtn(String text, Color bg, Color fg, int size) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Courier New", Font.BOLD, size));
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(5,10,5,10));
        return b;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NUMPAD
    // ══════════════════════════════════════════════════════════════════════
    class NumPad extends JPanel {
        // 9 số + 1 Del + 1 Enter = 11 buttons tổng, nhưng Enter dùng GridBagLayout span 2 cols
        private final List<JButton> allKeys = new ArrayList<>();
        private boolean padEnabled = false;
        private float   animScale  = 1f;

        NumPad() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            setPreferredSize(new Dimension(240, 220));
            setMaximumSize (new Dimension(240, 220));

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.BOTH;
            gc.insets = new Insets(4,4,4,4);
            gc.weightx = 1; gc.weighty = 1;

            // Hàng 1-3: chữ số 1-9
            String[] digits = {"1","2","3","4","5","6","7","8","9"};
            for (int i = 0; i < 9; i++) {
                gc.gridx = i % 3; gc.gridy = i / 3;
                gc.gridwidth = 1;
                JButton btn = createKey(digits[i], PANEL_BG, TEXT);
                add(btn, gc); allKeys.add(btn);
            }

            // Hàng 4: [⌫ Xóa] chiếm 1 cột + [↵ Enter] chiếm 2 cột
            gc.gridy = 3;
            gc.gridx = 0; gc.gridwidth = 1;
            JButton delBtn = createKey("⌫", new Color(0x6B,0x2D,0x2D), new Color(0xFF,0xAA,0xAA));
            add(delBtn, gc); allKeys.add(delBtn);

            gc.gridx = 1; gc.gridwidth = 2;
            JButton enterBtn = createKey("↵  Enter", new Color(0x1A,0x4A,0x2A), new Color(0xA3,0xBE,0x8C));
            add(enterBtn, gc); allKeys.add(enterBtn);
        }

        private JButton createKey(String lbl, Color bg, Color fg) {
            Color pressedBg = bg.brighter();
            JButton btn = new JButton(lbl) {
                private boolean pressed = false;
                { addMouseListener(new MouseAdapter(){
                    public void mousePressed(MouseEvent e)  { pressed=true;  repaint(); }
                    public void mouseReleased(MouseEvent e) { pressed=false; repaint(); }
                }); }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w=getWidth(), h=getHeight(), depth=pressed?0:3;
                    g2.setColor(new Color(0,0,0,80));
                    g2.fillRoundRect(1,depth+1,w-2,h-depth-1,10,10);
                    Color activeBg = isEnabled()?(pressed?pressedBg:bg):new Color(0x10,0x10,0x20);
                    g2.setColor(activeBg);
                    g2.fillRoundRect(1,pressed?3:0,w-2,h-3,10,10);
                    g2.setColor(pressed?ACCENT:activeBg.brighter());
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawRoundRect(1,pressed?3:0,w-2,h-3,10,10);
                    g2.setColor(isEnabled()?fg:SUBTEXT);
                    g2.setFont(getFont());
                    FontMetrics fm=g2.getFontMetrics();
                    int tx=(w-fm.stringWidth(getText()))/2;
                    int ty=(pressed?3:0)+(h-3+fm.getAscent()-fm.getDescent())/2;
                    g2.drawString(getText(),tx,ty);
                    g2.dispose();
                }
            };
            btn.setFont(new Font("SansSerif", Font.BOLD, lbl.length() > 2 ? 14 : 18));
            btn.setOpaque(false); btn.setContentAreaFilled(false);
            btn.setBorderPainted(false); btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> handleKey(lbl));
            return btn;
        }

        private void handleKey(String lbl) {
            if (!padEnabled) return;
            if (lbl.equals("⌫")) {
                if (!guessInput.isEmpty())
                    guessInput = guessInput.substring(0, guessInput.length()-1);
                updateGuessDisplay();
            } else if (lbl.startsWith("↵")) {
                // Enter: gửi nếu đủ chữ số và không trùng
                if (guessInput.length() == numDigits) {
                    Set<Character> seen = new HashSet<>();
                    boolean unique = true;
                    for (char c : guessInput.toCharArray())
                        if (!seen.add(c)) { unique = false; break; }
                    if (unique) {
                        final String fin = guessInput;
                        client.sendMsg("GUESS|" + fin);
                        guessInput = ""; updateGuessDisplay();
                    } else {
                        // Flash đỏ: chữ số trùng
                        guessInput = ""; updateGuessDisplay();
                    }
                }
            } else {
                // Chữ số: chỉ thêm nếu chưa có trong chuỗi hiện tại
                if (guessInput.length() < numDigits && !guessInput.contains(lbl)) {
                    guessInput += lbl;
                }
                updateGuessDisplay();
            }
        }

        public void setNumDigits(int n) { /* outer field handles this */ }
        public void setAnimScale(float s) { animScale=s; repaint(); }

        @Override public void paintComponent(Graphics g) {
            if (animScale >= 1f) { super.paintComponent(g); return; }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g2.translate(w/2f*(1-animScale), h/2f*(1-animScale));
            g2.scale(animScale, animScale);
            super.paintComponent(g2); g2.dispose();
        }

        @Override public void setEnabled(boolean b) {
            super.setEnabled(b); padEnabled=b;
            for (JButton k : allKeys) if (k!=null) k.setEnabled(b);
            repaint();
        }
    }

    // ══════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════
    //  CHAT BUBBLE  — draggable, fixed default position, custom image support
    //
    //  HOW TO USE A CUSTOM IMAGE:
    //  1. Put your image file (e.g. "chat_icon.png") in the same folder as
    //     the compiled .class files.
    //  2. In GameClient.java, after  chatBubble = roomPanel.getChatBubble();
    //     add this line:
    //       chatBubble.setCustomIcon(new ImageIcon("chat_icon.png").getImage());
    //  The image will be clipped to the circle and scaled to fit.
    // ══════════════════════════════════════════════════════════════════════
    public class ChatBubbleButton extends JButton {
        private int     unread = 0;
        private int     dsx, dsy;
        private boolean everDragged = false;   // true once user has moved it
        private boolean dragActive  = false;
        private Image   customIcon  = null;

        ChatBubbleButton() {
            setPreferredSize(new Dimension(54,54));
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e)  {
                    dsx = e.getX(); dsy = e.getY(); dragActive = false;
                }
                public void mouseDragged(MouseEvent e)  {
                    // Only register as a real drag after 4px threshold
                    if (!dragActive && Math.abs(e.getX()-dsx) + Math.abs(e.getY()-dsy) < 4) return;
                    dragActive   = true;
                    everDragged  = true;
                    Container par = getParent(); if (par == null) return;
                    int nx = Math.max(0, Math.min(getX()+e.getX()-dsx, par.getWidth() -getWidth()));
                    int ny = Math.max(0, Math.min(getY()+e.getY()-dsy, par.getHeight()-getHeight()));
                    setLocation(nx, ny);
                    // move chat dialog with bubble
                    if (chatDialog != null && chatDialog.isVisible()) {
                        try {
                            Point p = getLocationOnScreen();
                            chatDialog.setLocation(p.x - chatDialog.getWidth()-10,
                                    p.y - chatDialog.getHeight());
                        } catch (Exception ignored) {}
                    }
                }
                public void mouseReleased(MouseEvent e) {
                    if (!dragActive) toggleChatDialog();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        /** Returns true if the user has manually dragged this bubble */
        public boolean hasBeenDragged()  { return everDragged; }
        /** Reset so the bubble snaps to default position next time it's shown */
        public void    resetDragFlag()   { everDragged = false; }

        public void setUnread(int n)     { unread = n; repaint(); }
        public void setCustomIcon(Image img) { customIcon = img; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = w - 6;

            // shadow
            g2.setColor(new Color(0,0,0,60));
            g2.fillOval(4, 6, r, r);

            // circle
            g2.setColor(ACCENT);
            g2.fillOval(2, 2, r, r);

            if (customIcon != null) {
                // clip to circle, draw custom image
                g2.setClip(new java.awt.geom.Ellipse2D.Float(2, 2, r, r));
                g2.drawImage(customIcon, 2, 2, r, r, null);
                g2.setClip(null);
            } else {
                drawBubbleIcon(g2, w, h, r);
            }

            // unread badge
            if (unread > 0) {
                g2.setColor(GOLD);
                g2.fillOval(w-20, 0, 18, 18);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                String t = unread > 9 ? "9+" : String.valueOf(unread);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(t, w-20+(18-fm.stringWidth(t))/2, 13);
            }
            g2.dispose();
        }

        /** Vector speech-bubble icon — no emoji, no font dependency */
        private void drawBubbleIcon(Graphics2D g2, int w, int h, int r) {
            int pad = 9;
            int bx = pad, by = pad + 1;
            int bw = r - pad - 2, bh = (int)((r - pad) * 0.65f);
            int br = 5;

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(bx, by, bw, bh, br, br);

            // tail
            int tx = bx + 8;
            int ty = by + bh - 1;
            g2.fillPolygon(new int[]{tx, tx+9, tx+2}, new int[]{ty, ty, ty+8}, 3);

            // three dots
            g2.setColor(ACCENT);
            int dotY = by + bh/2 - 2;
            int dotGap = bw / 4;
            for (int i = 1; i <= 3; i++)
                g2.fillOval(bx + i*dotGap - 3, dotY, 6, 6);
        }
    }

    //  NUMBER TRACKER PANEL
    //  Column per position: big letter top, 3x3 mini-grid of digits 1-9
    // ══════════════════════════════════════════════════════════════════════
    class NumberTrackerPanel extends JPanel {
        private static final Color TB    = new Color(0x1A,0x1A,0x2E);
        private static final Color TCELL = new Color(0x0F,0x34,0x60);
        private static final Color TCR   = new Color(0xE9,0x45,0x60);
        private static final Color TLET  = new Color(0x88,0xC0,0xD0);
        private static final Color TNUM  = new Color(0xEA,0xEA,0xEA);
        private static final Color TDIM  = new Color(0x44,0x20,0x20);

        private boolean[][] crossed;
        private int nd;

        NumberTrackerPanel(int nd) {
            this.nd=nd; crossed=new boolean[nd][9];
            setBackground(TB); setOpaque(true);
        }

        public void setNumDigits(int n) { nd=n; crossed=new boolean[n][9]; repaint(); }
        public void crossAllPositions(char d) {
            int di=d-'1'; if(di<0||di>8) return;
            for (int p=0;p<nd;p++) crossed[p][di]=true; repaint();
        }
        public void markTried(int pos, char d, boolean ok) {
            int di=d-'1'; if(di>=0&&di<9&&pos<nd&&!ok) crossed[pos][di]=true; repaint();
        }
        public void resetAll() { crossed=new boolean[nd][9]; repaint(); }

        private static final int PAD=14, CGAP=10, LETH=44, MINI=36, MGAP=4;
        private int colW() { return 3*MINI+2*MGAP; }

        @Override public Dimension getPreferredSize() {
            return new Dimension(
                    Math.max(200, 2*PAD+nd*colW()+Math.max(0,nd-1)*CGAP),
                    Math.max(200, 2*PAD+LETH+3*MINI+2*MGAP));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int cw=colW();
            int totalW=nd*cw+Math.max(0,nd-1)*CGAP;
            int ox=(getWidth()-totalW)/2;
            char[] ltrs={'A','B','C','D','E','F'};

            for (int p=0;p<nd;p++) {
                int cx0=ox+p*(cw+CGAP);
                g2.setFont(new Font("Courier New",Font.BOLD,24));
                g2.setColor(TLET);
                String lb=String.valueOf(ltrs[p]);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(lb, cx0+(cw-fm.stringWidth(lb))/2, PAD+fm.getAscent());

                int gy=PAD+LETH;
                for (int row=0;row<3;row++) for (int col=0;col<3;col++) {
                    int di=row*3+col;
                    int mx=cx0+col*(MINI+MGAP), my=gy+row*(MINI+MGAP);
                    boolean x=crossed[p][di];
                    g2.setColor(x?new Color(0x20,0x08,0x0C):TCELL);
                    g2.fillRoundRect(mx,my,MINI,MINI,8,8);
                    g2.setFont(new Font("Courier New",Font.BOLD,14));
                    FontMetrics mf=g2.getFontMetrics();
                    String num=String.valueOf(di+1);
                    g2.setColor(x?TDIM:TNUM);
                    g2.drawString(num, mx+(MINI-mf.stringWidth(num))/2,
                            my+(MINI+mf.getAscent()-mf.getDescent())/2);
                    if (x) {
                        g2.setColor(TCR);
                        g2.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                        g2.drawLine(mx+5,my+5,mx+MINI-5,my+MINI-5);
                        g2.drawLine(mx+MINI-5,my+5,mx+5,my+MINI-5);
                    }
                }
            }
            g2.dispose();
        }
    }

    static class GuessEntry {
        int attempt,bull,cow; String guess;
        GuessEntry(int a,String g,int b,int c){attempt=a;guess=g;bull=b;cow=c;}
    }

    /** Panel với góc bo tròn cho chat bubble */
    static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int radius;
        RoundedPanel(Color bg, int radius) {
            this.bg = bg; this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}