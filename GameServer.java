import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class GameServer extends JFrame {

    private static final int TCP_PORT = 9876;
    private static final int UDP_PORT = 9877;
    private static final String DISCOVER_REQ = "GAME_DISCOVER";
    private static final String DISCOVER_RES = "GAME_SERVER";


    private JTextArea logArea;
    private JTextArea chatArea;
    private JTextField chatField;
    private JLabel statusLabel, clientCountLabel;


    private ServerSocket serverSocket;
    private DatagramSocket udpSocket;
    private volatile boolean running = true;


    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final List<String> chatHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    public GameServer() {
        setupGUI();
        startServer();
    }


    private void setupGUI() {
        setTitle("🎮 Game Đoán Số - Server");
        setSize(750, 600);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(NordTheme.BG);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { shutdown(); }
        });

        // Top status bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(NordTheme.FROST3);
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        statusLabel = NordTheme.label("⏳ Đang khởi động...", Color.WHITE);
        clientCountLabel = NordTheme.label("Clients: 0", NordTheme.SNOW0);
        topPanel.add(statusLabel, BorderLayout.WEST);
        topPanel.add(clientCountLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center: log + chat
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(NordTheme.BG);
        splitPane.setDividerLocation(370);
        splitPane.setBorder(null);

        // Log panel
        JPanel logPanel = createPanel("📋 Server Log");
        logArea = NordTheme.textArea();
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logPanel.add(logScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(logPanel);

        // Chat panel
        JPanel chatPanel = createPanel("💬 Lobby Chat");
        chatArea = NordTheme.textArea();
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(null);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        chatInputPanel.setBackground(NordTheme.BG_SEC);
        chatInputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        chatField = NordTheme.textField("");
        chatField.addActionListener(e -> sendServerChat());
        JButton sendBtn = NordTheme.button("Gửi", NordTheme.FROST3);
        sendBtn.addActionListener(e -> sendServerChat());
        chatInputPanel.add(chatField, BorderLayout.CENTER);
        chatInputPanel.add(sendBtn, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(chatPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createPanel(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NordTheme.BG_SEC);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(NordTheme.BORDER),
                        title, TitledBorder.LEFT, TitledBorder.TOP,
                        null, NordTheme.FROST3
                )
        ));
        return p;
    }


    private void startServer() {
        new Thread(this::runTCPServer, "TCP-Server").start();
        new Thread(this::runUDPDiscovery, "UDP-Discovery").start();
    }

    private void runTCPServer() {
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            InetAddress addr = InetAddress.getLocalHost();
            log("✅ TCP Server đã khởi động tại port " + TCP_PORT);
            log("📡 Server IP: " + addr.getHostAddress());
            SwingUtilities.invokeLater(() ->
                    statusLabel.setText("🟢 Đang chạy - " + addr.getHostAddress() + ":" + TCP_PORT));

            while (running) {
                Socket sock = serverSocket.accept();
                ClientHandler ch = new ClientHandler(sock);
                clients.add(ch);
                ch.start();
                log("🔗 Kết nối mới: " + sock.getInetAddress().getHostAddress());
                updateClientCount();
            }
        } catch (IOException e) {
            if (running) log("❌ TCP Error: " + e.getMessage());
        }
    }

    private void runUDPDiscovery() {
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
            udpSocket.setBroadcast(true);
            log("✅ UDP Discovery đang lắng nghe tại port " + UDP_PORT);
            byte[] buf = new byte[256];

            while (running) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                udpSocket.receive(pkt);
                String msg = new String(pkt.getData(), 0, pkt.getLength());

                if (msg.equals(DISCOVER_REQ)) {
                    InetAddress clientAddr = pkt.getAddress();
                    int clientPort = pkt.getPort();
                    String resp = DISCOVER_RES + "|" + TCP_PORT + "|" +
                            InetAddress.getLocalHost().getHostAddress();
                    byte[] respData = resp.getBytes();
                    udpSocket.send(new DatagramPacket(respData, respData.length,
                            clientAddr, clientPort));
                    log("📡 Discovery từ " + clientAddr.getHostAddress());
                }
            }
        } catch (IOException e) {
            if (running) log("❌ UDP Error: " + e.getMessage());
        }
    }


    private void sendServerChat() {
        String msg = chatField.getText().trim();
        if (msg.isEmpty()) return;
        chatField.setText("");
        broadcastToAll("CHAT_MSG|🖥️ Server|" + msg);
        appendChat("🖥️ Server: " + msg);
    }

    private void broadcastToAll(String msg) {

        synchronized (chatHistory) {
            chatHistory.add(msg);
            if (chatHistory.size() > MAX_HISTORY) {
                chatHistory.remove(0);
            }
        }
        for (ClientHandler ch : clients) {
            if (ch.nickname != null) ch.send(msg);
        }
    }

    private void sendChatHistory(ClientHandler client) {
        synchronized (chatHistory) {
            for (String msg : chatHistory) {
                client.send(msg);
            }
        }

        for (Map.Entry<String, Room> entry : rooms.entrySet()) {
            Room room = entry.getValue();
            if (!room.gameStarted) {
                client.send("ROOM_ANNOUNCE|" + room.code + "|" + room.owner +
                        "|" + room.numDigits);
            }
        }
    }

    private void broadcastToRoom(String roomCode, String msg) {
        Room room = rooms.get(roomCode);
        if (room == null) return;
        for (String player : room.players) {
            ClientHandler ch = findClient(player);
            if (ch != null) ch.send(msg);
        }
    }

    private ClientHandler findClient(String nickname) {
        for (ClientHandler ch : clients) {
            if (nickname.equals(ch.nickname)) return ch;
        }
        return null;
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rng = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        String code = sb.toString();
        return rooms.containsKey(code) ? generateRoomCode() : code;
    }

    private synchronized void handleCreateRoom(ClientHandler client, int numDigits) {
        if (client.currentRoom != null) {
            client.send("ERROR|Bạn đang ở trong phòng rồi!");
            return;
        }
        if (numDigits < 2 || numDigits > 6) {
            client.send("ERROR|Số chữ số phải từ 2 đến 6!");
            return;
        }
        String code = generateRoomCode();
        Room room = new Room(code, client.nickname, numDigits);
        room.players.add(client.nickname);
        rooms.put(code, room);
        client.currentRoom = code;

        client.send("ROOM_CREATED|" + code);
        client.send("ROOM_JOINED|" + code + "|" + client.nickname);
        broadcastToAll("ROOM_ANNOUNCE|" + code + "|" + client.nickname + "|" + numDigits);
        appendChat("🏠 " + client.nickname + " tạo phòng " + code);
        log("🏠 Phòng " + code + " được tạo bởi " + client.nickname);
    }

    private synchronized void handleJoinRoom(ClientHandler client, String code) {
        if (client.currentRoom != null) {
            client.send("ERROR|Bạn đang ở trong phòng rồi!");
            return;
        }
        Room room = rooms.get(code);
        if (room == null) {
            client.send("ERROR|Không tìm thấy phòng " + code + "!");
            return;
        }
        if (room.gameStarted) {
            client.send("ERROR|Phòng đang chơi, không thể vào!");
            return;
        }
        if (room.players.size() >= 6) {
            client.send("ERROR|Phòng đã đầy (6/6)!");
            return;
        }

        room.players.add(client.nickname);
        client.currentRoom = code;

        String playerList = String.join(",", room.players);
        client.send("ROOM_JOINED|" + code + "|" + playerList);
        broadcastToRoom(code, "ROOM_PLAYER_JOINED|" + client.nickname +
                "|" + room.players.size());
        broadcastToAll("CHAT_MSG|🎮 Hệ thống|" + client.nickname +
                " đã vào phòng [" + code + "]");
        log("👤 " + client.nickname + " vào phòng " + code);
    }

    private synchronized void handleLeaveRoom(ClientHandler client) {
        if (client.currentRoom == null) return;
        Room room = rooms.get(client.currentRoom);
        if (room == null) { client.currentRoom = null; return; }

        room.players.remove(client.nickname);
        if (room.gameStarted) {
            room.activePlayers.remove(client.nickname);
            room.secrets.remove(client.nickname);
            room.rankings.add(client.nickname); // auto-lose
        }

        broadcastToRoom(client.currentRoom, "ROOM_PLAYER_LEFT|" + client.nickname);
        log("👤 " + client.nickname + " rời phòng " + client.currentRoom);


        if (client.nickname.equals(room.owner)) {
            if (room.players.isEmpty()) {
                rooms.remove(client.currentRoom);
                log("🗑️ Phòng " + client.currentRoom + " đã bị xóa");
            } else {
                room.owner = room.players.get(0);
                broadcastToRoom(client.currentRoom,
                        "ROOM_CHAT_MSG|🎮 Hệ thống|" + room.owner + " là chủ phòng mới");
            }
        }

        client.send("ROOM_LEFT");
        String prevRoom = client.currentRoom;
        client.currentRoom = null;


        if (room.gameStarted && room.activePlayers.size() <= 1) {
            endGame(room, prevRoom);
        }
    }


    private synchronized void handlePlayerReady(ClientHandler client) {
        if (client.currentRoom == null || client.nickname == null) return;
        Room room = rooms.get(client.currentRoom);
        if (room == null || room.gameStarted) return;
        room.readyPlayers.add(client.nickname);
        broadcastToRoom(client.currentRoom, "PLAYER_READY_NOTIFY|" + client.nickname);
        // Đếm tất cả người (kể cả host)
        int total = room.players.size();
        int readyCount = room.readyPlayers.size();
        broadcastToRoom(client.currentRoom, "READY_UPDATE|" + readyCount + "|" + total);
    }

    private synchronized void handlePlayerUnready(ClientHandler client) {
        if (client.currentRoom == null || client.nickname == null) return;
        Room room = rooms.get(client.currentRoom);
        if (room == null || room.gameStarted) return;
        room.readyPlayers.remove(client.nickname);
        int total = room.players.size();
        int readyCount = room.readyPlayers.size();
        broadcastToRoom(client.currentRoom, "READY_UPDATE|" + readyCount + "|" + total);
    }

    private synchronized void handleStartGame(ClientHandler client) {
        if (client.currentRoom == null) return;
        Room room = rooms.get(client.currentRoom);
        if (room == null) return;

        if (!client.nickname.equals(room.owner)) {
            client.send("ERROR|Chỉ chủ phòng mới có thể bắt đầu!"); return;
        }
        if (room.players.size() < 2) {
            client.send("ERROR|Cần ít nhất 2 người chơi!"); return;
        }
        if (room.gameStarted) {
            client.send("ERROR|Game đã bắt đầu rồi!"); return;
        }

        // Kiểm tra tất cả đã sẵn sàng (đặt số bí mật)
        if (room.readyPlayers.size() < room.players.size()) {
            int missing = room.players.size() - room.readyPlayers.size();
            client.send("ERROR|Còn " + missing + " người chưa sẵn sàng!");
            return;
        }

        room.gameStarted = true;
        room.activePlayers = new ArrayList<>(room.players);
        room.rankings = new ArrayList<>();
        room.currentTurnIndex = 0;
        room.secretsReady = room.players.size(); // đã đặt hết
        room.readyPlayers = new HashSet<>();

        broadcastToRoom(client.currentRoom, "GAME_START|" + room.numDigits);
        broadcastToRoom(client.currentRoom,
                "ROOM_CHAT_MSG|🎮 Hệ thống|Game bắt đầu! Chúc mọi người may mắn!");
        log("🎮 Game bắt đầu tại phòng " + client.currentRoom);

        // Bắt đầu lượt ngay — secrets đã có sẵn từ lobby
        startTurn(room, client.currentRoom);
    }

    private synchronized void handleSetSecret(ClientHandler client, String secret) {
        if (client.currentRoom == null) return;
        Room room = rooms.get(client.currentRoom);
        if (room == null) return;

        if (secret.length() != room.numDigits || !secret.matches("\\d+")) {
            client.send("ERROR|Số bí mật phải có đúng " + room.numDigits + " chữ số!");
            return;
        }
        if (!isValidSecret(secret)) {
            client.send("ERROR|Số không được có số 0 và không được trùng chữ số!");
            return;
        }

        // Lưu số bí mật — cho phép cả trước khi game bắt đầu (giai đoạn lobby)
        room.secrets.put(client.nickname, secret);
        client.send("SECRET_SET|" + secret);

        if (room.gameStarted) {
            // Đang trong game: đếm secretsReady, bắt đầu lượt khi đủ
            room.secretsReady = (int) room.activePlayers.stream()
                    .filter(p -> room.secrets.containsKey(p)).count();
            broadcastToRoom(client.currentRoom,
                    "ROOM_CHAT_MSG|🎮 Hệ thống|" + client.nickname + " đã chọn số bí mật ✅");
            if (room.secretsReady == room.activePlayers.size()) {
                broadcastToRoom(client.currentRoom,
                        "ROOM_CHAT_MSG|🎮 Hệ thống|Tất cả đã sẵn sàng! Bắt đầu đoán số!");
                startTurn(room, client.currentRoom);
            }
        }
        log("🔒 " + client.nickname + " đặt số bí mật tại phòng " + client.currentRoom);
    }
    private boolean isValidSecret(String secret) {
        if (secret == null || secret.contains("0")) return false;
        Set<Character> set = new HashSet<>();
        for (char c : secret.toCharArray()) {
            if (!set.add(c)) return false;
        }
        return true;
    }
    private void startTurn(Room room, String roomCode) {
        if (room.activePlayers.size() <= 1) {
            endGame(room, roomCode);
            return;
        }

        int idx = room.currentTurnIndex % room.activePlayers.size();
        String guesser = room.activePlayers.get(idx);

        int targetIdx = (idx + 1) % room.activePlayers.size();
        String target = room.activePlayers.get(targetIdx);

        // Thông báo cả phòng biết lượt của ai
        broadcastToRoom(roomCode,
                "ROOM_CHAT_MSG|\uD83C\uDFAE H\u1ec7 th\u1ed1ng|L\u01b0\u1ee3t c\u1ee7a " +
                        guesser + " \u2192 \u0111o\u00e1n s\u1ed1 c\u1ee7a " + target);

        // Chỉ gửi YOUR_TURN cho người được đoán
        ClientHandler guesserClient = findClient(guesser);
        if (guesserClient != null) {
            guesserClient.send("YOUR_TURN|" + target);
        }
    }
    private synchronized void handleGuess(ClientHandler client, String guess) {
        if (client.currentRoom == null) return;
        Room room = rooms.get(client.currentRoom);
        if (room == null || !room.gameStarted) return;

        // Validate độ dài, chỉ gồm chữ số, không có 0, không trùng
        if (guess.length() != room.numDigits || !guess.matches("[1-9]+")) {
            client.send("ERROR|Số đoán phải có đúng " + room.numDigits + " chữ số từ 1-9!");
            return;
        }
        Set<Character> guessSet = new HashSet<>();
        for (char c : guess.toCharArray()) {
            if (!guessSet.add(c)) {
                client.send("ERROR|Số đoán không được có chữ số trùng!");
                return;
            }
        }

        int idx = room.currentTurnIndex % room.activePlayers.size();
        if (!client.nickname.equals(room.activePlayers.get(idx))) {
            client.send("ERROR|Chưa đến lượt!");
            return;
        }

        int targetIdx = (idx + 1) % room.activePlayers.size();
        String target = room.activePlayers.get(targetIdx);
        String secret = room.secrets.get(target);

        int bulls = 0, cows = 0;
        // Đếm Bulls trước (đúng vị trí)
        boolean[] secretUsed = new boolean[secret.length()];
        boolean[] guessUsed  = new boolean[guess.length()];
        for (int i = 0; i < secret.length(); i++) {
            if (secret.charAt(i) == guess.charAt(i)) {
                bulls++;
                secretUsed[i] = true;
                guessUsed[i]  = true;
            }
        }
        // Đếm Cows (đúng số, sai vị trí) — không đếm lại ô đã dùng
        for (int i = 0; i < guess.length(); i++) {
            if (guessUsed[i]) continue;
            for (int j = 0; j < secret.length(); j++) {
                if (!secretUsed[j] && guess.charAt(i) == secret.charAt(j)) {
                    cows++;
                    secretUsed[j] = true;
                    break;
                }
            }
        }

        // 1. Gửi kết quả riêng cho người đoán (để cập nhật bảng lịch sử của họ)
        client.send("GUESS_RESULT_DETAIL|" + guess + "|" + bulls + "|" + cows);

        // 2. Broadcast cho cả phòng biết kết quả lượt này
        broadcastToRoom(client.currentRoom,
                "OPPONENT_GUESS|" + client.nickname + "|" + guess + "|" + bulls + "|" + cows);
        broadcastToRoom(client.currentRoom,
                "ROOM_CHAT_MSG|\uD83C\uDFAE|" + client.nickname +
                        " đoán [" + guess + "] → Bò:" + bulls + " Bê:" + cows);

        if (bulls == room.numDigits) {
            // Thắng: xóa NGƯỜI ĐÃ THẮNG (client.nickname) khỏi activePlayers
            int rank = room.rankings.size() + 1;
            room.rankings.add(client.nickname);
            broadcastToRoom(client.currentRoom,
                    "PLAYER_WON|" + client.nickname + "|" + rank);
            broadcastToRoom(client.currentRoom,
                    "ROOM_CHAT_MSG|\uD83C\uDFC6|" + client.nickname +
                            " đoán đúng số của " + target + " [" + secret + "]! Hạng #" + rank);

            int winnerIdx = room.activePlayers.indexOf(client.nickname);
            room.activePlayers.remove(client.nickname);

            if (room.activePlayers.size() <= 1) {
                if (!room.activePlayers.isEmpty())
                    room.rankings.add(room.activePlayers.get(0));
                endGame(room, client.currentRoom);
            } else {
                // Điều chỉnh index sau khi xóa
                if (winnerIdx < room.currentTurnIndex ||
                        winnerIdx == room.currentTurnIndex) {
                    room.currentTurnIndex = winnerIdx % room.activePlayers.size();
                }
                startTurn(room, client.currentRoom);
            }
        } else {
            // Chưa thắng: chuyển lượt sang người tiếp theo
            room.currentTurnIndex = (idx + 1) % room.activePlayers.size();
            startTurn(room, client.currentRoom);
        }
    }
    private void endGame(Room room, String roomCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n══════ 🏆 BẢNG XẾP HẠNG 🏆 ══════\n");
        for (int i = 0; i < room.rankings.size(); i++) {
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "  ";
            String playerName = room.rankings.get(i);
            String playerSecret = room.secrets.getOrDefault(playerName, "???");
            sb.append(medal).append(" #").append(i + 1).append(" - ")
                    .append(playerName).append(" (số bí mật: ").append(playerSecret).append(")\n");
        }
        sb.append("══════════════════════════════\n");

        broadcastToRoom(roomCode, "ROOM_CHAT_MSG|🎮 Hệ thống|" + sb.toString());
        // Build rankings with secrets: "player1:secret1,player2:secret2"
        StringBuilder rankSecrets = new StringBuilder();
        for (int i = 0; i < room.rankings.size(); i++) {
            if (i > 0) rankSecrets.append(",");
            String p = room.rankings.get(i);
            rankSecrets.append(p).append(":").append(room.secrets.getOrDefault(p, "???"));
        }
        broadcastToRoom(roomCode, "GAME_OVER|" + rankSecrets.toString());
        broadcastToAll("CHAT_MSG|🎮 Hệ thống|Phòng [" + roomCode +
                "] đã kết thúc game! 🏆 " + room.rankings.get(0));
        appendChat("🏆 Phòng " + roomCode + " kết thúc - Thắng: " + room.rankings.get(0));
        log("🏆 Game kết thúc tại phòng " + roomCode);


        for (String player : new ArrayList<>(room.players)) {
            ClientHandler ch = findClient(player);
            if (ch != null) {
                ch.send("ROOM_LEFT");
                ch.currentRoom = null;
            }
        }
        rooms.remove(roomCode);
    }


    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) +
                    "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void appendChat(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateClientCount() {
        long count = clients.stream().filter(c -> c.nickname != null).count();
        SwingUtilities.invokeLater(() ->
                clientCountLabel.setText("Clients: " + count));
    }

    private void shutdown() {
        running = false;
        broadcastToAll("SERVER_SHUTDOWN");
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        try { if (udpSocket != null) udpSocket.close(); } catch (Exception ignored) {}
        for (ClientHandler ch : clients) ch.close();
        System.exit(0);
    }


    class ClientHandler extends Thread {
        Socket link;
        PrintWriter output;
        Scanner input;
        String nickname;
        String currentRoom;

        ClientHandler(Socket link) {
            this.link = link;
            setDaemon(true);
        }

        public void run() {
            try {
                input = new Scanner(link.getInputStream());
                output = new PrintWriter(link.getOutputStream(), true);
                send("WELCOME|Ch\u00e0o m\u1eebng \u0111\u1ebfn Game \u0110o\u00e1n S\u1ed1! H\u00e3y \u0111\u1eb7t nickname.");

                while (running && input.hasNextLine()) {
                    String line = input.nextLine();
                    processMessage(line);
                }
            } catch (IOException e) {

            } finally {
                handleDisconnect();
            }
        }

        private void processMessage(String line) {
            String[] parts = line.split("\\|", 2);
            String cmd = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (cmd) {
                case "NICK":
                    handleNick(data);
                    break;
                case "CHAT":
                    if (nickname != null) {


                        broadcastToAll("CHAT_MSG|" + nickname + "|" + data);
                        appendChat(nickname + ": " + data);
                    }
                    break;
                case "CREATE_ROOM":
                    try {
                        handleCreateRoom(this, Integer.parseInt(data.trim()));
                    } catch (NumberFormatException e) {
                        send("ERROR|S\u1ed1 ch\u1eef s\u1ed1 kh\u00f4ng h\u1ee3p l\u1ec7!");
                    }
                    break;
                case "JOIN_ROOM":
                    handleJoinRoom(this, data.trim().toUpperCase());
                    break;
                case "LEAVE_ROOM":
                    handleLeaveRoom(this);
                    break;
                case "ROOM_CHAT":
                    if (currentRoom != null && nickname != null) {
                        broadcastToRoom(currentRoom, "ROOM_CHAT_MSG|" + nickname + "|" + data);
                    }
                    break;
                case "PLAYER_READY":
                    handlePlayerReady(this);
                    break;
                case "PLAYER_UNREADY":
                    handlePlayerUnready(this);
                    break;
                case "START_GAME":
                    handleStartGame(this);
                    break;
                case "SET_SECRET":
                    handleSetSecret(this, data.trim());
                    break;
                case "GUESS":
                    handleGuess(this, data.trim());
                    break;
            }
        }

        private void handleNick(String name) {
            name = name.trim();
            if (name.isEmpty() || name.length() > 20) {
                send("ERROR|Nickname kh\u00f4ng h\u1ee3p l\u1ec7 (1-20 k\u00fd t\u1ef1)!");
                return;
            }
            for (ClientHandler ch : clients) {
                if (name.equals(ch.nickname)) {
                    send("ERROR|Nickname \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng!");
                    return;
                }
            }
            this.nickname = name;
            send("NICK_OK|" + name);

            sendChatHistory(this);
            broadcastToAll("CHAT_MSG|\uD83C\uDFAE H\u1ec7 th\u1ed1ng|" + name + " \u0111\u00e3 tham gia lobby!");
            appendChat("\u27A1\uFE0F " + name + " \u0111\u00e3 tham gia");
            log("\uD83D\uDC64 " + name + " \u0111\u00e3 k\u1ebft n\u1ed1i (" + link.getInetAddress().getHostAddress() + ")");
            updateClientCount();
        }

        private void handleDisconnect() {
            if (currentRoom != null) handleLeaveRoom(this);
            clients.remove(this);
            if (nickname != null) {
                broadcastToAll("CHAT_MSG|\uD83C\uDFAE H\u1ec7 th\u1ed1ng|" + nickname + " \u0111\u00e3 r\u1eddi \u0111i.");
                appendChat("\u2B05\uFE0F " + nickname + " \u0111\u00e3 r\u1eddi \u0111i");
                log("\uD83D\uDC64 " + nickname + " \u0111\u00e3 ng\u1eaft k\u1ebft n\u1ed1i");
            }
            updateClientCount();
            close();
        }

        void send(String msg) {
            if (output != null) output.println(msg);
        }

        void close() {
            try { if (link != null) link.close(); } catch (Exception ignored) {}
        }
    }


    static class Room {
        String code, owner;
        int numDigits;
        List<String> players = new ArrayList<>();
        boolean gameStarted = false;
        Map<String, String> secrets = new HashMap<>();
        List<String> activePlayers = new ArrayList<>();
        List<String> rankings = new ArrayList<>();
        int currentTurnIndex = 0;
        int secretsReady = 0;
        Set<String> readyPlayers = new HashSet<>(); // lobby ready tracking

        Room(String code, String owner, int numDigits) {
            this.code = code;
            this.owner = owner;
            this.numDigits = numDigits;
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameServer().setVisible(true));
    }
}