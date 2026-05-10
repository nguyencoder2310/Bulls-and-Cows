import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class GameClient extends JFrame {

    private static final int TCP_PORT = 9876;
    private static final int UDP_PORT = 9877;
    private static final String DISCOVER_REQ = "GAME_DISCOVER";

    private String  nickname;
    private String  currentRoom;
    private boolean isOwner   = false;
    private int     numDigits = 4;
    private boolean myTurn    = false;

    private CardLayout cardLayout;
    private JPanel     mainPanel;
    private LoginPanel loginPanel;
    private LobbyPanel lobbyPanel;
    private RoomPanel  roomPanel;

    private JLayeredPane layeredPane;
    private RoomPanel.ChatBubbleButton chatBubble;

    private Map<String,String> revealedSecrets = new HashMap<>();
    private List<String>       finalRankings   = new ArrayList<>();

    private Socket      link;
    private PrintWriter output;
    private Scanner     input;
    private volatile boolean connected = false;

    public GameClient() { setupGUI(); }

    private void setupGUI() {
        setTitle("Bulls & Cows");
        setSize(430, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        layeredPane = new JLayeredPane();

        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setBackground(RoomPanel.BG);

        loginPanel = new LoginPanel(this);
        lobbyPanel = new LobbyPanel(this);
        roomPanel  = new RoomPanel(this);

        mainPanel.add(loginPanel, "login");
        mainPanel.add(lobbyPanel, "lobby");
        mainPanel.add(roomPanel,  "room");

        // mainPanel fills the entire layeredPane
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                mainPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                // Only snap bubble to default if it has NOT been dragged yet
                if (chatBubble != null && !chatBubble.hasBeenDragged()) {
                    placeBubbleDefault();
                }
            }
        });
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);

        chatBubble = roomPanel.getChatBubble();
        chatBubble.setCustomIcon(new ImageIcon("my_icon.gif").getImage());
        chatBubble.setVisible(false);
        layeredPane.add(chatBubble, JLayeredPane.POPUP_LAYER);

        setContentPane(layeredPane);
        cardLayout.show(mainPanel, "login");
    }

    /** Bottom-right corner */
    private void placeBubbleDefault() {
        int x = layeredPane.getWidth()  - 68;
        int y = layeredPane.getHeight() - 68;
        if (x > 0 && y > 0) chatBubble.setBounds(x, y, 54, 54);
    }

    private void showChatBubble(boolean show) {
        if (show) {
            if (!chatBubble.hasBeenDragged()) placeBubbleDefault();
            chatBubble.setVisible(true);
        } else {
            chatBubble.setVisible(false);
            chatBubble.resetDragFlag();
        }
        layeredPane.repaint();
    }

    public String  getCurrentRoom() { return currentRoom; }
    public boolean isMyTurn()       { return myTurn; }
    public void    sendMsg(String m){ if (output != null) output.println(m); }

    public void discoverServer() {
        new Thread(() -> {
            try {
                DatagramSocket ds = new DatagramSocket();
                ds.setBroadcast(true);
                ds.setSoTimeout(3000);
                byte[] data = DISCOVER_REQ.getBytes();
                ds.send(new DatagramPacket(data, data.length,
                        InetAddress.getByName("255.255.255.255"), UDP_PORT));
                byte[] buf = new byte[256];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);
                ds.receive(resp);
                String msg = new String(resp.getData(), 0, resp.getLength());
                String[] p = msg.split("\\|");
                if (p[0].equals("GAME_SERVER") && p.length >= 3) {
                    final String ip = p[2];
                    SwingUtilities.invokeLater(() -> {
                        loginPanel.setServerIp(ip);
                        JOptionPane.showMessageDialog(this, "Tim thay: " + ip);
                    });
                }
                ds.close();
            } catch (SocketTimeoutException e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Khong tim thay server LAN!"));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Loi: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Kết nối qua Radmin VPN.
     * mode = "CREATE" → host tạo phòng (server phải chạy trên máy host Radmin)
     * mode = "JOIN"   → join bằng IP Radmin của host + mã phòng
     */
    public void connectOnline(String mode) {
        String nick = loginPanel.getNickname();
        if (nick.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập nickname trước nhé!"); return;
        }

        String ip = loginPanel.getRadminIp();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nhập IP Radmin của host!\n\n" +
                            "Hướng dẫn:\n" +
                            "1. Cả hai máy cùng vào một mạng Radmin VPN\n" +
                            "2. Host mở Radmin → xem IP (thường 26.x.x.x)\n" +
                            "3. Điền IP đó vào ô 'Radmin IP'\n" +
                            "4. Host phải chạy GameServer trước",
                    "Thiếu IP Radmin", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String roomCode = mode.equals("JOIN") ? loginPanel.getRoomCode() : "";
        if (mode.equals("JOIN") && roomCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập mã phòng trước nhé!"); return;
        }

        final String finalNick = nick;
        final String finalCode = roomCode;
        new Thread(() -> {
            try {
                link   = new Socket(InetAddress.getByName(ip), TCP_PORT);
                input  = new Scanner(link.getInputStream());
                output = new PrintWriter(link.getOutputStream(), true);
                connected = true;
                nickname  = finalNick;
                sendMsg("NICK|" + finalNick);

                if (mode.equals("JOIN")) {
                    pendingJoinCode = finalCode;
                }

                new Thread(this::listenServer, "Listener").start();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Không kết nối được!\n" +
                                        "Kiểm tra:\n" +
                                        "• IP Radmin đúng chưa? (" + ip + ")\n" +
                                        "• Host đã chạy GameServer chưa?\n" +
                                        "• Cùng mạng Radmin VPN chưa?\n\n" +
                                        "Chi tiết: " + e.getMessage()));
            }
        }).start();
    }

    private volatile String pendingJoinCode = null;

    public void connectToServer() {
        String nick = loginPanel.getNickname();
        if (nick.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhap nickname di!"); return;
        }
        String ip = loginPanel.getServerIp();
        if (ip.equals("auto") || ip.isEmpty()) { discoverServer(); return; }
        new Thread(() -> {
            try {
                link   = new Socket(InetAddress.getByName(ip), TCP_PORT);
                input  = new Scanner(link.getInputStream());
                output = new PrintWriter(link.getOutputStream(), true);
                connected = true;
                nickname  = nick;
                sendMsg("NICK|" + nick);
                new Thread(this::listenServer, "Listener").start();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Khong ket noi duoc: " + e.getMessage()));
            }
        }).start();
    }

    private void listenServer() {
        try {
            while (connected && input.hasNextLine())
                handleServerMessage(input.nextLine());
        } catch (Exception e) {
            if (connected) {
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Mat ket noi!");
                    showChatBubble(false);
                    cardLayout.show(mainPanel, "login");
                });
            }
        }
    }

    private void handleServerMessage(String line) {
        String[] parts = line.split("\\|", 3);
        String   cmd   = parts[0];

        SwingUtilities.invokeLater(() -> {
            switch (cmd) {
                case "WELCOME": break;

                case "NICK_OK":
                    cardLayout.show(mainPanel, "lobby");
                    setTitle("Bulls & Cows - " + nickname);
                    roomPanel.setMyNickname(nickname);
                    showChatBubble(false);
                    // Auto-join nếu có mã phòng đang chờ (từ "Join by Code")
                    if (pendingJoinCode != null && !pendingJoinCode.isEmpty()) {
                        final String code = pendingJoinCode;
                        pendingJoinCode = null;
                        sendMsg("JOIN_ROOM|" + code);
                    }
                    break;

                case "CHAT_MSG":
                    if (parts.length >= 3) {
                        String sender = parts[1], msg = parts[2];
                        if (!sender.contains("He thong") && !sender.equals("\uD83C\uDFAE"))
                            lobbyPanel.appendText(sender + ": " + msg + "\n", new Color(0xEC,0xEF,0xF4));
                        else
                            lobbyPanel.appendText("  " + msg + "\n", new Color(0x6B,0x74,0x8A));
                    }
                    break;

                case "ROOM_ANNOUNCE":
                    if (parts.length >= 3) {
                        String[] ap = line.split("\\|", 5);
                        if (ap.length >= 4) {
                            lobbyPanel.appendText(ap[2] + " tạo phòng [" + ap[1] + "] - " + ap[3] + " chữ số  ",
                                    new Color(0x88,0xC0,0xD0));
                            lobbyPanel.appendJoinButton(ap[1]);
                            lobbyPanel.appendText("\n", new Color(0xEC,0xEF,0xF4));
                        }
                    }
                    break;

                case "ROOM_CREATED":
                    currentRoom = parts.length > 1 ? parts[1] : "";
                    isOwner = true;
                    revealedSecrets.clear(); finalRankings.clear();
                    roomPanel.setRoomInfo("Phòng: " + currentRoom + " (Chủ phòng)");
                    roomPanel.clearChat();
                    roomPanel.resetForNewGame();
                    roomPanel.setStartEnabled(true);
                    roomPanel.setSecretEnabled(false);
                    roomPanel.setGuessEnabled(false);
                    myTurn = false;
                    roomPanel.setTurnText("Chờ người chơi...");
                    showChatBubble(true);
                    cardLayout.show(mainPanel, "room");
                    break;

                case "ROOM_JOINED":
                    if (parts.length >= 3) {
                        currentRoom = parts[1];
                        if (!isOwner) {
                            revealedSecrets.clear(); finalRankings.clear();
                            roomPanel.setRoomInfo("Phòng: " + currentRoom);
                            roomPanel.clearChat();
                            roomPanel.resetForNewGame();
                            roomPanel.setStartEnabled(false);
                            roomPanel.setSecretEnabled(false);
                            roomPanel.setGuessEnabled(false);
                            myTurn = false;
                            roomPanel.setTurnText("Chờ bắt đầu...");
                            showChatBubble(true);
                            cardLayout.show(mainPanel, "room");
                        }
                    }
                    break;

                case "ROOM_PLAYER_JOINED":
                    if (parts.length >= 2)
                        roomPanel.setTurnText(parts[1] + " đã vào phòng");
                    break;

                case "ROOM_PLAYER_LEFT":
                    if (parts.length >= 2)
                        roomPanel.setTurnText(parts[1] + " đã rời phòng");
                    break;

                case "ROOM_CHAT_MSG":
                    if (parts.length >= 3) {
                        String sender2 = parts[1], msg2 = parts[2];
                        boolean isSys = sender2.equals("\uD83C\uDFAE") || sender2.equals("\uD83C\uDFAE H\u1ec7 th\u1ed1ng")
                                || sender2.equals("\uD83C\uDFAE") || sender2.startsWith("\uD83C\uDFAE")
                                || sender2.startsWith("\uD83C\uDFF0") || sender2.startsWith("\uD83C\uDFC6");
                        if (!isSys) {
                            boolean isMe = sender2.equals(nickname);
                            roomPanel.appendChatBubble(sender2, msg2, isMe);
                        } else {
                            roomPanel.appendChatSystem(msg2);
                        }
                    }
                    break;

                case "ROOM_LEFT":
                    currentRoom = null; isOwner = false; myTurn = false;
                    numDigits = 4;
                    roomPanel.fullReset();   // xóa toàn bộ state ván cũ
                    showChatBubble(false);
                    cardLayout.show(mainPanel, "lobby");
                    break;

                case "GAME_START":
                    if (parts.length >= 2) {
                        numDigits = Integer.parseInt(parts[1].trim());
                        roomPanel.setNumDigits(numDigits);
                    }
                    roomPanel.resetForNewGame();
                    roomPanel.setStartEnabled(false);
                    roomPanel.setSecretEnabled(true);
                    roomPanel.setSecretEditable(true);   // mở lại field bị lock từ ván trước
                    roomPanel.setGuessEnabled(false);
                    roomPanel.setChatEnabled(true);
                    roomPanel.setTurnText("Đặt số bí mật!");
                    break;

                case "SECRET_SET":
                    roomPanel.setSecretEnabled(false);
                    roomPanel.setSecretEditable(false);
                    roomPanel.setTurnText("Chờ người khác đặt số...");
                    break;

                case "YOUR_TURN":
                    myTurn = true;
                    String target = parts.length > 1 ? parts[1] : "?";
                    roomPanel.setTurnText("Lượt bạn! Đoán số của " + target);
                    roomPanel.setMyTurnActive();  // enable numPad + switchCard("keypad") + revalidate
                    roomPanel.focusGuess();
                    break;

                // Simple result — không dùng nữa, server mới không gửi
                case "GUESS_RESULT":
                    break;

                // Rich result — sent only to the guesser
                case "GUESS_RESULT_DETAIL":
                    String[] d = line.split("\\|");
                    if (d.length >= 4) {
                        try {
                            String guessStr = d[1];
                            int bull = Integer.parseInt(d[2]);
                            int cow  = Integer.parseInt(d[3]);
                            roomPanel.addGuessResult(guessStr, bull, cow);
                            if (bull != numDigits) {
                                myTurn = false;
                                // Tắt bàn phím và hiện "chờ" — YOUR_TURN sẽ bật lại
                                roomPanel.setTurnText("Chờ lượt...");
                                roomPanel.setOpponentTurn("đối thủ");
                            }
                        } catch (Exception ignored) {}
                    }
                    break;

                // Server mới gửi để báo ai đang giữ lượt (chỉ người không phải guesser)
                case "OPPONENT_TURN":
                    if (parts.length >= 2 && !myTurn) {
                        roomPanel.setOpponentTurn(parts[1]);
                        roomPanel.setTurnText(parts[1] + " đang đoán...");
                    }
                    break;

                // Broadcast to whole room — also arrives for our OWN guess
                case "OPPONENT_GUESS":
                    String[] og = line.split("\\|");
                    if (og.length >= 5) {
                        String opName  = og[1];
                        String opGuess = og[2];
                        int opB = 0, opC = 0;
                        try { opB = Integer.parseInt(og[3]); opC = Integer.parseInt(og[4]); } catch (Exception ignored) {}
                        boolean isMyGuess = opName.equals(nickname);
                        roomPanel.appendChatBubble(opName,
                                "[" + opGuess + "] Bò:" + opB + " Bê:" + opC, isMyGuess);
                        // Người chờ nhận tin này → hiện "X đang đoán"
                        // (người vừa đoán đã bị GUESS_RESULT_DETAIL xử lý rồi)
                        if (!isMyGuess && !myTurn) {
                            roomPanel.setOpponentTurn(opName);
                        }
                    }
                    break;

                case "PLAYER_WON":
                    if (parts.length >= 3) {
                        finalRankings.add(parts[1]);
                        roomPanel.setTurnText(parts[1] + " thắng! Hạng #" + parts[2]);
                    }
                    break;

                case "GAME_OVER":
                    myTurn = false;
                    roomPanel.setGuessEnabled(false);
                    roomPanel.setTurnText("Game kết thúc!");
                    List<String> rankings = new ArrayList<>();
                    // Server sends "player1:secret1,player2:secret2"
                    if (parts.length >= 2 && !parts[1].isEmpty()) {
                        for (String entry : parts[1].split(",")) {
                            String[] kv = entry.split(":");
                            rankings.add(kv[0]);
                            if (kv.length >= 2) revealedSecrets.put(kv[0], kv[1]);
                        }
                    }
                    final List<String> fr  = rankings;
                    final Map<String,String> sec = new HashMap<>(revealedSecrets);
                    SwingUtilities.invokeLater(() -> roomPanel.showGameOverDialog(fr, sec));
                    break;

                case "ERROR":
                    String errMsg = parts.length > 1 ? parts[1] : "Loi";
                    JOptionPane.showMessageDialog(this, errMsg, "Loi", JOptionPane.WARNING_MESSAGE);
                    break;

                case "SERVER_SHUTDOWN":
                    connected = false;
                    JOptionPane.showMessageDialog(this, "Server da tat!");
                    showChatBubble(false);
                    cardLayout.show(mainPanel, "login");
                    break;
            }
        });
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new GameClient().setVisible(true));
    }
}