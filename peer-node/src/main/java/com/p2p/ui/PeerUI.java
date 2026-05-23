package com.p2p.ui;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.chunk.Bitfield;
import com.p2p.network.PeerClient;
import com.p2p.dto.file.FileMetadata;
import com.p2p.dto.peer.PeerInfo;
import com.p2p.dto.tracker.RegisterRequest;
import com.p2p.service.ChunkService;
import com.p2p.service.DowloadService;
import com.p2p.service.FileService;
import com.p2p.tracker.TrackerClient;
import com.p2p.network.PeerServer;
import com.p2p.message.Message;
import com.p2p.message.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PeerUI {

    private static final Color APP_BG = new Color(7, 15, 29);
    private static final Color SIDEBAR_BG = new Color(10, 25, 45);
    private static final Color CARD_BG = new Color(18, 32, 50);
    private static final Color CARD_BG_2 = new Color(13, 26, 42);
    private static final Color BORDER = new Color(47, 66, 91);
    private static final Color FIELD_BG = new Color(8, 18, 32);

    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color PURPLE = new Color(109, 40, 217);
    private static final Color GREEN = new Color(34, 197, 94);

    private static final Color TEXT = new Color(241, 245, 249);
    private static final Color MUTED = new Color(148, 163, 184);

    private static final Color LOG_GREEN = new Color(75, 255, 111);
    private static final Color LOG_BLUE = new Color(56, 189, 248);
    private static final Color LOG_ORANGE = new Color(251, 191, 36);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            new JoinFrame();
        });
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
            UIManager.put("TextField.caretForeground", TEXT);
            UIManager.put("TextField.selectionBackground", BLUE);
        } catch (Exception ignored) {}
    }

    // ================= JOIN SCREEN =================
    static class JoinFrame extends JFrame {
        private final JTextField ipField = new JTextField("127.0.0.1");
        private final JTextField portField = new JTextField("5000");

        JoinFrame() {
            setTitle("P2P File Sharing Dashboard");
            setSize(960, 580);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);

            JPanel root = new GradientPanel(new Color(15, 23, 42), new Color(30, 64, 175));
            root.setLayout(new GridBagLayout());
            root.setBorder(new EmptyBorder(40, 40, 40, 40));
            setContentPane(root);

            RoundedPanel card = new RoundedPanel(32, new Color(15, 23, 42, 235), new Color(59, 130, 246, 120));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new EmptyBorder(38, 42, 38, 42));
            card.setPreferredSize(new Dimension(460, 430));

            JLabel logo = new JLabel("☍");
            logo.setFont(new Font("Segoe UI Symbol", Font.BOLD, 42));
            logo.setForeground(new Color(96, 165, 250));
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel title = new JLabel("P2P File Sharing");
            title.setFont(new Font("Segoe UI", Font.BOLD, 34));
            title.setForeground(TEXT);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel sub = new JLabel("Join Tracker để bắt đầu chia sẻ file");
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            sub.setForeground(MUTED);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton join = gradientButton("Kết nối Tracker");
            join.addActionListener(e -> join());

            card.add(logo);
            card.add(Box.createVerticalStrut(10));
            card.add(title);
            card.add(Box.createVerticalStrut(8));
            card.add(sub);
            card.add(Box.createVerticalStrut(28));
            card.add(label("Peer IP"));
            card.add(Box.createVerticalStrut(8));
            card.add(input(ipField));
            card.add(Box.createVerticalStrut(16));
            card.add(label("Peer Port"));
            card.add(Box.createVerticalStrut(8));
            card.add(input(portField));
            card.add(Box.createVerticalStrut(28));
            card.add(join);
            card.add(Box.createVerticalStrut(18));
            card.add(note("Tracker endpoint: http://localhost:8080/tracker/message"));

            root.add(card);
            setVisible(true);
        }

        private void join() {
            String ip = ipField.getText().trim();
            int port;

            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (Exception e) {
                showError(this, "Port không hợp lệ");
                return;
            }

            try {
                PeerInfo me = new PeerInfo();
                me.setIp(ip);
                me.setPort(port);

                RegisterRequest req = new RegisterRequest();
                req.setPeer(me);
                req.setFileName("");
                req.setChunks(new ArrayList<>());

                new TrackerClient().register(req);

                new DashboardFrame(ip, port);
                dispose();

            } catch (Exception ex) {
                showError(this, "Không kết nối được Tracker. Hãy kiểm tra tracker-server đã chạy chưa.");
            }
        }
    }

    // ================= DASHBOARD =================
    static class DashboardFrame extends JFrame {
        private final String ip;
        private final int port;

        private final TrackerClient tracker = new TrackerClient();
        private final DefaultListModel<PeerInfo> peerModel = new DefaultListModel<>();
        private final DefaultListModel<PeerChunkInfo> searchPeerModel = new DefaultListModel<>();
        private final DefaultListModel<Integer> chunkModel = new DefaultListModel<>();

        private final JList<PeerChunkInfo> searchPeerList = new JList<>(searchPeerModel);
        private final JList<Integer> chunkList = new JList<>(chunkModel);
        private final JLabel searchResultLabel = new JLabel("Chưa search file");
        private final JButton downloadAllButton = gradientButton("⇩  Download All Missing");

        private final PeerClient peerClient = new PeerClient();
        private final ObjectMapper mapper = new ObjectMapper();
        private List<PeerInfo> currentSearchPeers = new ArrayList<>();
        private String currentSearchFile = "";

        private final ChunkService chunkService;
        private Timer heartbeatTimer;
        private Timer peerRefreshTimer;
        private Thread peerServerThread;

        private final JTextPane logPane = new JTextPane();
        private final JLabel peerCountLabel = new JLabel("0");
        private final JLabel statusLabel = new JLabel("ONLINE");

        private final JTextField searchField = new JTextField("test.pdf");
        private final JTextField chunkField = new JTextField("0");

        private final JLabel selectedFileLabel = new JLabel("Chưa chọn file");
        private File selectedFile;

        DashboardFrame(String ip, int port) {
            this.ip = ip;
            this.port = port;
            this.chunkService = new ChunkService(port);

            setTitle("P2P File Sharing Dashboard");
            setSize(1440, 780);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    shutdownPeer();
                }
            });
            setMinimumSize(new Dimension(1180, 720));

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(APP_BG);
            setContentPane(root);

            root.add(sidebar(), BorderLayout.WEST);
            root.add(mainPanel(), BorderLayout.CENTER);
            root.add(footer(), BorderLayout.SOUTH);

            startPeerServer();
            loadPeers();
            startHeartbeat();

            peerRefreshTimer = new Timer(4000, e -> loadPeers());
            peerRefreshTimer.start();

            setVisible(true);
        }

        private JPanel sidebar() {
            JPanel side = new JPanel(new BorderLayout());
            side.setBackground(SIDEBAR_BG);
            side.setPreferredSize(new Dimension(190, 0));
            side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.setBorder(new EmptyBorder(22, 14, 14, 14));

            JLabel logo = new JLabel("☍", SwingConstants.CENTER);
            logo.setOpaque(true);
            logo.setBackground(BLUE);
            logo.setForeground(Color.WHITE);
            logo.setFont(new Font("Segoe UI Symbol", Font.BOLD, 32));
            logo.setPreferredSize(new Dimension(52, 52));
            logo.setMaximumSize(new Dimension(52, 52));
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);

            top.add(logo);
            top.add(Box.createVerticalStrut(28));
            top.add(navItem("⌂", "Dashboard", true));
            top.add(navItem("⇧", "Upload File", false));
            top.add(navItem("⇩", "Download Chunk", false));
            top.add(navItem("♧", "Peers", false));
            top.add(navItem("▤", "File List", false));
            top.add(navItem("⚙", "Settings", false));
            top.add(Box.createVerticalStrut(12));
            top.add(separator());
            top.add(navItem("▣", "System Logs", false));

            RoundedPanel info = new RoundedPanel(16, new Color(12, 27, 48), new Color(0, 132, 255));
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setBorder(new EmptyBorder(14, 14, 14, 14));
            info.setPreferredSize(new Dimension(160, 210));
            info.setMaximumSize(new Dimension(160, 210));

            JLabel t = new JLabel("● Peer Information");
            t.setForeground(TEXT);
            t.setFont(new Font("Segoe UI", Font.BOLD, 14));

            info.add(t);
            info.add(Box.createVerticalStrut(16));
            info.add(infoLine("IP Address", ip));
            info.add(infoLine("Port", String.valueOf(port)));
            info.add(infoLine("Status", "● Online"));
            info.add(infoLine("Tracker", "127.0.0.1:8080"));

            JPanel bottom = new JPanel(new GridBagLayout());
            bottom.setOpaque(false);
            bottom.setBorder(new EmptyBorder(14, 14, 14, 14));
            bottom.add(info);

            side.add(top, BorderLayout.NORTH);
            side.add(bottom, BorderLayout.SOUTH);

            return side;
        }

        private JPanel mainPanel() {
            JPanel main = new JPanel(new BorderLayout());
            main.setBackground(APP_BG);
            main.add(header(), BorderLayout.NORTH);

            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(APP_BG);
            content.setBorder(new EmptyBorder(16, 16, 12, 16));

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(7, 7, 7, 7);
            c.fill = GridBagConstraints.BOTH;

            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0.34;
            c.weighty = 0.55;
            content.add(uploadCard(), c);

            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 0.66;
            c.weighty = 0.55;
            content.add(downloadCard(), c);

            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 0.34;
            c.weighty = 0.45;
            content.add(peerCard(), c);

            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 0.66;
            c.weighty = 0.45;
            content.add(logCard(), c);

            main.add(content, BorderLayout.CENTER);
            return main;
        }

        private JPanel header() {
            JPanel header = new GradientPanel(new Color(37, 99, 235), new Color(109, 40, 217));
            header.setLayout(new BorderLayout());
            header.setPreferredSize(new Dimension(0, 94));
            header.setBorder(new EmptyBorder(18, 20, 16, 20));

            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("P2P File Sharing Dashboard");
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 25));

            JLabel sub = new JLabel("Peer " + ip + ":" + port + " đang hoạt động với Tracker");
            sub.setForeground(new Color(219, 234, 254));
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            left.add(title);
            left.add(Box.createVerticalStrut(8));
            left.add(sub);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
            right.setOpaque(false);
            right.add(statBox("● STATUS", statusLabel));
            right.add(statBox("♧ PEERS", peerCountLabel));
            right.add(peerBadge(ip + ":" + port));

            header.add(left, BorderLayout.WEST);
            header.add(right, BorderLayout.EAST);

            return header;
        }

        private JPanel uploadCard() {
            RoundedPanel card = darkCard();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new EmptyBorder(18, 18, 14, 18));

            card.add(sectionTitle("☁", "Upload / Register File", "Chọn file từ máy, split chunk và register với Tracker"));
            card.add(Box.createVerticalStrut(16));
            card.add(dropZone());
            card.add(Box.createVerticalStrut(10));
            card.add(fileInfoBox());
            card.add(Box.createVerticalStrut(12));

            JButton upload = gradientButton("♙  Upload / Register File");
            upload.addActionListener(e -> uploadFile());
            card.add(upload);

            return card;
        }

        private JPanel dropZone() {
            DashedPanel p = new DashedPanel();
            p.setLayout(new GridBagLayout());
            p.setPreferredSize(new Dimension(0, 150));
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            p.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chooseFile();
                }
            });

            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

            JLabel icon = new JLabel("☁", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Symbol", Font.BOLD, 36));
            icon.setForeground(new Color(59, 130, 246));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel t1 = new JLabel("Kéo thả file vào đây");
            t1.setForeground(TEXT);
            t1.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            t1.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel t2 = new JLabel("hoặc");
            t2.setForeground(MUTED);
            t2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            t2.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton choose = gradientButton("Chọn file từ máy");
            choose.setPreferredSize(new Dimension(150, 36));
            choose.setMaximumSize(new Dimension(150, 36));
            choose.addActionListener(e -> chooseFile());

            inner.add(icon);
            inner.add(Box.createVerticalStrut(6));
            inner.add(t1);
            inner.add(Box.createVerticalStrut(8));
            inner.add(t2);
            inner.add(Box.createVerticalStrut(8));
            inner.add(choose);

            p.add(inner);
            return p;
        }

        private JPanel fileInfoBox() {
            RoundedPanel box = new RoundedPanel(12, new Color(16, 31, 49), BORDER);
            box.setLayout(new BorderLayout(12, 0));
            box.setBorder(new EmptyBorder(12, 14, 12, 14));
            box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

            JLabel icon = new JLabel("▱");
            icon.setFont(new Font("Segoe UI Symbol", Font.BOLD, 30));
            icon.setForeground(new Color(203, 213, 225));

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

            selectedFileLabel.setForeground(TEXT);
            selectedFileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

            JLabel hint = new JLabel("Chunk size: 100KB theo FileService");
            hint.setForeground(MUTED);
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            text.add(selectedFileLabel);
            text.add(Box.createVerticalStrut(3));
            text.add(hint);

            box.add(icon, BorderLayout.WEST);
            box.add(text, BorderLayout.CENTER);

            return box;
        }

        private JPanel downloadCard() {
            RoundedPanel card = darkCard();
            card.setLayout(new BorderLayout(0, 12));
            card.setBorder(new EmptyBorder(18, 18, 14, 18));

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(sectionTitle("⌕", "Search & Download File", "Search tên file → hiện peer chứa file và chunk đang có"));
            top.add(Box.createVerticalStrut(14));

            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            row.add(input(searchField), BorderLayout.CENTER);

            JButton search = darkButton("⌕  Search File");
            search.setPreferredSize(new Dimension(145, 44));
            search.addActionListener(e -> searchFile());
            row.add(search, BorderLayout.EAST);
            top.add(row);

            searchResultLabel.setForeground(MUTED);
            searchResultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            top.add(Box.createVerticalStrut(8));
            top.add(searchResultLabel);

            JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
            center.setOpaque(false);

            searchPeerList.setCellRenderer(new SearchPeerRenderer());
            searchPeerList.setBackground(CARD_BG_2);
            searchPeerList.setFixedCellHeight(64);
            searchPeerList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) showChunksOfSelectedPeer();
            });

            chunkList.setCellRenderer(new ChunkRenderer());
            chunkList.setBackground(CARD_BG_2);
            chunkList.setFixedCellHeight(44);
            chunkList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) downloadSelectedChunk();
                }
            });

            center.add(listPanel("Peers chứa file", searchPeerList));
            center.add(listPanel("Chunks của peer đang chọn", chunkList));

            JPanel bottom = new JPanel(new BorderLayout(12, 0));
            bottom.setOpaque(false);
            downloadAllButton.addActionListener(e -> downloadAllMissingChunks());
            bottom.add(downloadAllButton, BorderLayout.CENTER);
            bottom.add(infoNote("ℹ", "Click peer để xem chunk. Click chunk để tải chunk đó. Download All tải các chunk còn thiếu."), BorderLayout.SOUTH);

            card.add(top, BorderLayout.NORTH);
            card.add(center, BorderLayout.CENTER);
            card.add(bottom, BorderLayout.SOUTH);

            return card;
        }

        private JPanel peerCard() {
            RoundedPanel card = darkCard();
            card.setLayout(new BorderLayout(0, 12));
            card.setBorder(new EmptyBorder(18, 18, 14, 18));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(sectionTitle("♧", "Online Peers", "Danh sách peer lấy từ Tracker"), BorderLayout.CENTER);

            JButton refreshSmall = iconButton("⟳");
            refreshSmall.addActionListener(e -> loadPeers());
            top.add(refreshSmall, BorderLayout.EAST);

            JList<PeerInfo> list = new JList<>(peerModel);
            list.setCellRenderer(new PeerRenderer());
            list.setBackground(CARD_BG_2);
            list.setFixedCellHeight(54);
            list.setBorder(null);

            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createLineBorder(BORDER));
            scroll.getViewport().setBackground(CARD_BG_2);

            JButton refresh = darkButton("⟳  Refresh Peers");
            refresh.addActionListener(e -> loadPeers());

            card.add(top, BorderLayout.NORTH);
            card.add(scroll, BorderLayout.CENTER);
            card.add(refresh, BorderLayout.SOUTH);

            return card;
        }

        private JPanel logCard() {
            RoundedPanel card = darkCard();
            card.setLayout(new BorderLayout(0, 12));
            card.setBorder(new EmptyBorder(18, 18, 14, 18));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(sectionTitle("☷", "System Logs", "Theo dõi quá trình register, search và download"), BorderLayout.WEST);

            JButton clear = darkButton("⌫  Clear Logs");
            clear.setPreferredSize(new Dimension(130, 38));
            clear.addActionListener(e -> logPane.setText(""));
            top.add(clear, BorderLayout.EAST);

            logPane.setEditable(false);
            logPane.setFont(new Font("Consolas", Font.PLAIN, 14));
            logPane.setForeground(LOG_GREEN);
            logPane.setBackground(new Color(5, 15, 30));
            logPane.setBorder(new EmptyBorder(14, 16, 14, 16));

            log("Dashboard started for peer " + ip + ":" + port, LOG_GREEN);
            log("Connected to tracker 127.0.0.1:8080", LOG_GREEN);

            JScrollPane scroll = new JScrollPane(logPane);
            scroll.setBorder(BorderFactory.createLineBorder(BORDER));
            scroll.getViewport().setBackground(new Color(5, 15, 30));

            card.add(top, BorderLayout.NORTH);
            card.add(scroll, BorderLayout.CENTER);

            return card;
        }

        private JPanel footer() {
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
            footer.setBackground(new Color(8, 19, 34));
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0,0, BORDER));

            JLabel a = new JLabel("🛡  P2P File Sharing System");
            JLabel b = new JLabel("☕  Built with Java Swing");

            a.setForeground(MUTED);
            b.setForeground(MUTED);

            footer.add(a);
            footer.add(new JLabel("|"));
            footer.add(b);

            return footer;
        }


        private void startPeerServer() {
            peerServerThread = new Thread(() -> {
                try {
                    PeerServer server = new PeerServer(port, chunkService);
                    server.start();
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            log("Peer server failed: " + e.getMessage(), Color.RED)
                    );
                }
            });

            peerServerThread.setDaemon(true);
            peerServerThread.start();

            log("Peer server started on port " + port, LOG_GREEN);
        }

        private void startHeartbeat() {
            heartbeatTimer = new Timer(3000, e -> {
                try {
                    PeerInfo me = new PeerInfo();
                    me.setIp(ip);
                    me.setPort(port);

                    Message<PeerInfo> message =
                            new Message<>(MessageType.HEARTBEAT, me);

                    tracker.send(message);
                    statusLabel.setText("ONLINE");

                } catch (Exception ex) {
                    statusLabel.setText("OFFLINE");
                    log("Heartbeat failed: " + ex.getMessage(), Color.RED);
                }
            });

            heartbeatTimer.start();
            log("Heartbeat started every 3 seconds", LOG_GREEN);
        }

        private void shutdownPeer() {
            if (heartbeatTimer != null) {
                heartbeatTimer.stop();
            }

            if (peerRefreshTimer != null) {
                peerRefreshTimer.stop();
            }

            try {
                PeerInfo me = new PeerInfo();
                me.setIp(ip);
                me.setPort(port);

                Message<PeerInfo> message =
                        new Message<>(MessageType.UNREGISTER, me);

                tracker.send(message);
                log("Peer unregistered from tracker", LOG_ORANGE);

            } catch (Exception ignored) {
            }
        }

        private void chooseFile() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn file để upload");

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                long totalChunks = (long) Math.ceil(selectedFile.length() * 1.0 / (100 * 1024));

                selectedFileLabel.setText(
                        "<html><span style='color:white'>" + selectedFile.getName() +
                                "</span><br/><span style='color:#94a3b8'>" +
                                formatSize(selectedFile.length()) + " • " + totalChunks +
                                " chunks</span></html>"
                );

                log("Selected file: " + selectedFile.getName(), LOG_BLUE);
            }
        }

        private void uploadFile() {
            if (selectedFile == null) {
                showError(this, "Chưa chọn file để upload");
                return;
            }

            try {
                FileService fileService = new FileService(chunkService);
                FileMetadata metadata = fileService.splitFile(selectedFile.getAbsolutePath());

                List<Integer> chunks = new ArrayList<>();
                for (int i = 0; i < metadata.getTotalChunks(); i++) {
                    chunks.add(i);
                }

                PeerInfo peer = new PeerInfo();
                peer.setIp(ip);
                peer.setPort(port);

                RegisterRequest request = new RegisterRequest();
                request.setFileName(metadata.getFileName());
                request.setPeer(peer);
                request.setChunks(chunks);

                tracker.register(request);

                statusLabel.setText("SEEDING");
                log("Registered file: " + metadata.getFileName() + " with " + metadata.getTotalChunks() + " chunks", LOG_GREEN);
                loadPeers();

                showSuccess(this, "Upload/Register file thành công");

            } catch (Exception e) {
                log("Upload failed: " + e.getMessage(), Color.RED);
                showError(this, "Upload thất bại. Kiểm tra file hoặc tracker.");
            }
        }

        private void searchFile() {
            String fileName = searchField.getText().trim();

            if (fileName.isEmpty()) {
                showError(this, "Nhập tên file cần tìm");
                return;
            }

            try {
                currentSearchFile = fileName;
                currentSearchPeers = tracker.getPeer(fileName);

                searchPeerModel.clear();
                chunkModel.clear();
                renderPeers(currentSearchPeers);

                for (PeerInfo peer : currentSearchPeers) {
                    if (peer.getPort() == port) continue;
                    List<Integer> chunks = getBitfield(fileName, peer);
                    if (!chunks.isEmpty()) {
                        searchPeerModel.addElement(new PeerChunkInfo(peer, chunks));
                    }
                }

                statusLabel.setText("SEARCHED");
                searchResultLabel.setText("Found " + searchPeerModel.size() + " peer(s) có file: " + fileName);
                log("Search file: " + fileName, LOG_BLUE);
                log("Found " + searchPeerModel.size() + " peer(s) with chunks", LOG_GREEN);

                if (searchPeerModel.isEmpty()) {
                    showError(this, "Không tìm thấy peer nào có chunk của file này");
                } else {
                    searchPeerList.setSelectedIndex(0);
                }

            } catch (Exception e) {
                log("Search failed: " + e.getMessage(), Color.RED);
                showError(this, "Tìm file thất bại");
            }
        }

        private void showChunksOfSelectedPeer() {
            chunkModel.clear();
            PeerChunkInfo selected = searchPeerList.getSelectedValue();
            if (selected == null) return;

            for (Integer c : selected.chunks) {
                chunkModel.addElement(c);
            }
            log("Selected peer " + selected.peer.getPort() + " has chunks " + selected.chunks, LOG_BLUE);
        }

        private void downloadSelectedChunk() {
            PeerChunkInfo selectedPeer = searchPeerList.getSelectedValue();
            Integer chunkIndex = chunkList.getSelectedValue();

            if (selectedPeer == null || chunkIndex == null) return;
            if (currentSearchFile == null || currentSearchFile.isEmpty()) {
                showError(this, "Chưa search file");
                return;
            }

            if (chunkService.getChunk(currentSearchFile, chunkIndex) != null) {
                log("Chunk " + chunkIndex + " already exists", LOG_ORANGE);
                showSuccess(this, "Chunk " + chunkIndex + " đã tồn tại");
                return;
            }

            try {
                List<Integer> selectedChunks = new ArrayList<>();
                selectedChunks.add(chunkIndex);

                List<PeerInfo> onePeer = new ArrayList<>();
                onePeer.add(selectedPeer.peer);

                DowloadService service = new DowloadService(chunkService, port);
                service.dowload(currentSearchFile, onePeer, selectedChunks);

                statusLabel.setText("DOWNLOADED");
                log("Downloaded chunk " + chunkIndex + " from peer " + selectedPeer.peer.getPort(), LOG_GREEN);
                showSuccess(this, "Download chunk " + chunkIndex + " hoàn tất");

            } catch (Exception e) {
                log("Download chunk failed: " + e.getMessage(), Color.RED);
                showError(this, "Download chunk thất bại");
            }
        }

        private void downloadAllMissingChunks() {
            if (currentSearchFile == null || currentSearchFile.isEmpty()) {
                showError(this, "Hãy search file trước");
                return;
            }

            if (currentSearchPeers == null || currentSearchPeers.isEmpty()) {
                showError(this, "Không có peer nào giữ file này");
                return;
            }

            try {
                List<Integer> missingChunks = new ArrayList<>();

                for (int i = 0; i < searchPeerModel.size(); i++) {
                    PeerChunkInfo info = searchPeerModel.get(i);
                    for (Integer c : info.chunks) {
                        if (!missingChunks.contains(c) && chunkService.getChunk(currentSearchFile, c) == null) {
                            missingChunks.add(c);
                        }
                    }
                }

                if (missingChunks.isEmpty()) {
                    log("All chunks already exist for file " + currentSearchFile, LOG_ORANGE);
                    showSuccess(this, "Tất cả chunk đã tồn tại, không cần download");
                    return;
                }

                DowloadService service = new DowloadService(chunkService, port);
                service.dowload(currentSearchFile, currentSearchPeers, missingChunks);

                statusLabel.setText("DOWNLOADED");
                log("Download all missing chunks: " + missingChunks, LOG_GREEN);
                showSuccess(this, "Download all chunk còn thiếu hoàn tất");

            } catch (Exception e) {
                log("Download all failed: " + e.getMessage(), Color.RED);
                showError(this, "Download all thất bại");
            }
        }

        private List<Integer> getBitfield(String fileName, PeerInfo peer) {
            try {
                Message<String> bitReq = new Message<>(MessageType.BITFIELD, fileName);
                Message<?> bitRes = peerClient.send(peer.getIp(), peer.getPort(), bitReq);

                if (bitRes == null || bitRes.getType() != MessageType.BITFIELD) {
                    log("Cannot get BITFIELD from peer " + peer.getPort(), Color.RED);
                    return new ArrayList<>();
                }

                Bitfield bf = mapper.convertValue(bitRes.getPayload(), Bitfield.class);
                return bf.getAvailableChunks() == null ? new ArrayList<>() : bf.getAvailableChunks();

            } catch (Exception e) {
                log("Error BITFIELD peer " + peer.getPort() + ": " + e.getMessage(), Color.RED);
                return new ArrayList<>();
            }
        }

        private void downloadOneChunk() {
            downloadSelectedChunk();
        }

        private void loadPeers() {
            try {
                List<PeerInfo> peers = tracker.getPeer("");
                renderPeers(peers);
            } catch (Exception e) {
                log("Cannot load peers: " + e.getMessage(), Color.RED);
            }
        }

        private void renderPeers(List<PeerInfo> peers) {
            peerModel.clear();
            for (PeerInfo p : peers) {
                peerModel.addElement(p);
            }
            peerCountLabel.setText(String.valueOf(peers.size()));
        }

        private void log(String text, Color color) {
            try {
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                StyledDocument doc = logPane.getStyledDocument();

                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, color);

                doc.insertString(doc.getLength(), "[" + time + "]  " + text + "\n", attrs);
                logPane.setCaretPosition(doc.getLength());

            } catch (Exception ignored) {}
        }
    }

    // ================= COMPONENTS =================

    static JPanel listPanel(String title, JList<?> list) {
        RoundedPanel p = new RoundedPanel(10, CARD_BG_2, BORDER);
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel t = new JLabel(title);
        t.setForeground(TEXT);
        t.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(CARD_BG_2);

        p.add(t, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    static JPanel sectionTitle(String icon, String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JLabel ic = new JLabel(icon, SwingConstants.CENTER);
        ic.setOpaque(true);
        ic.setBackground(new Color(13, 48, 82));
        ic.setForeground(new Color(0, 132, 255));
        ic.setFont(new Font("Segoe UI Symbol", Font.BOLD, 22));
        ic.setPreferredSize(new Dimension(38, 38));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setForeground(TEXT);
        t.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel s = new JLabel(subtitle);
        s.setForeground(MUTED);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        text.add(t);
        text.add(Box.createVerticalStrut(4));
        text.add(s);

        panel.add(ic, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);

        return panel;
    }

    static JPanel fieldBlock(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label(label));
        p.add(Box.createVerticalStrut(8));
        p.add(input(field));
        return p;
    }

    static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JComponent input(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBackground(FIELD_BG);
        field.setBorder(new EmptyBorder(0, 14, 0, 14));
        field.setOpaque(false);

        RoundedPanel wrap = new RoundedPanel(8, FIELD_BG, BORDER);
        wrap.setLayout(new BorderLayout());
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrap.setPreferredSize(new Dimension(100, 44));
        wrap.add(field, BorderLayout.CENTER);

        return wrap;
    }

    static JLabel note(String text) {
        JLabel note = new JLabel("<html><div style='width:350px;line-height:1.5'>" + text + "</div></html>");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        note.setForeground(MUTED);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        return note;
    }

    static JPanel infoNote(String icon, String text) {
        RoundedPanel p = new RoundedPanel(10, new Color(19, 45, 70), new Color(43, 80, 120));
        p.setLayout(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel ic = new JLabel(icon);
        ic.setForeground(new Color(0, 132, 255));
        ic.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));

        JLabel tx = new JLabel(text);
        tx.setForeground(new Color(191, 219, 254));
        tx.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        p.add(ic, BorderLayout.WEST);
        p.add(tx, BorderLayout.CENTER);

        return p;
    }

    static JPanel navItem(String icon, String text, boolean active) {
        RoundedPanel p = new RoundedPanel(
                8,
                active ? BLUE : new Color(0, 0, 0, 0),
                active ? BLUE : new Color(0, 0, 0, 0)
        );

        p.setLayout(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel ic = new JLabel(icon);
        ic.setForeground(Color.WHITE);
        ic.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));

        JLabel tx = new JLabel(text);
        tx.setForeground(Color.WHITE);
        tx.setFont(new Font("Segoe UI", Font.BOLD, active ? 14 : 13));

        p.add(ic, BorderLayout.WEST);
        p.add(tx, BorderLayout.CENTER);

        return p;
    }

    static JComponent separator() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        return s;
    }

    static JPanel infoLine(String label, String value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel v = new JLabel(value);
        v.setForeground(value.contains("Online") ? GREEN : TEXT);
        v.setFont(new Font("Segoe UI", Font.BOLD, 13));

        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        p.add(Box.createVerticalStrut(12));

        return p;
    }

    static RoundedPanel darkCard() {
        return new RoundedPanel(14, CARD_BG, BORDER);
    }

    static JPanel statBox(String label, JLabel value) {
        RoundedPanel p = new RoundedPanel(10, new Color(255, 255, 255, 25), new Color(255, 255, 255, 0));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setPreferredSize(new Dimension(140, 58));

        JLabel l = new JLabel(label);
        l.setForeground(label.contains("STATUS") ? GREEN : new Color(219, 234, 254));
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));

        value.setForeground(Color.WHITE);
        value.setFont(new Font("Segoe UI", Font.BOLD, 18));

        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(value);

        return p;
    }

    static JPanel peerBadge(String text) {
        RoundedPanel p = new RoundedPanel(10, new Color(15, 23, 80), new Color(0, 0, 0, 0));
        p.setLayout(new GridBagLayout());
        p.setPreferredSize(new Dimension(170, 58));

        JLabel t = new JLabel("▰  " + text);
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));

        p.add(t);
        return p;
    }

    static JButton gradientButton(String text) {
        return new GradientButton(text, BLUE, PURPLE);
    }

    static JButton darkButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(new Color(12, 25, 42));
        b.setForeground(new Color(203, 213, 225));
        b.setBorder(BorderFactory.createLineBorder(BORDER));
        return b;
    }

    static JButton iconButton(String text) {
        JButton b = darkButton(text);
        b.setPreferredSize(new Dimension(38, 38));
        return b;
    }

    static JButton baseButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setPreferredSize(new Dimension(120, 42));
        return b;
    }

    static void showError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    static void showSuccess(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    // ================= RENDERER =================

    static class PeerRenderer extends JPanel implements ListCellRenderer<PeerInfo> {
        private final JLabel left = new JLabel();
        private final JLabel right = new JLabel("ONLINE");

        PeerRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 12, 10, 12));
            setOpaque(true);

            left.setFont(new Font("Segoe UI", Font.BOLD, 15));

            right.setFont(new Font("Segoe UI", Font.BOLD, 11));
            right.setOpaque(true);
            right.setForeground(GREEN);
            right.setBackground(new Color(20, 83, 45));
            right.setBorder(new EmptyBorder(4, 8, 4, 8));

            add(left, BorderLayout.WEST);
            add(right, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends PeerInfo> list,
                PeerInfo value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            setBackground(isSelected ? new Color(20, 45, 70) : CARD_BG_2);
            left.setForeground(TEXT);
            left.setText("●   " + value.getIp() + ":" + value.getPort());
            return this;
        }
    }

    static class PeerChunkInfo {
        PeerInfo peer;
        List<Integer> chunks;

        PeerChunkInfo(PeerInfo peer, List<Integer> chunks) {
            this.peer = peer;
            this.chunks = chunks;
        }
    }

    static class SearchPeerRenderer extends JPanel implements ListCellRenderer<PeerChunkInfo> {
        private final JLabel left = new JLabel();
        private final JLabel right = new JLabel();

        SearchPeerRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(8, 10, 8, 10));
            setOpaque(true);
            left.setFont(new Font("Segoe UI", Font.BOLD, 14));
            right.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            right.setForeground(MUTED);
            add(left, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PeerChunkInfo> list, PeerChunkInfo value, int index, boolean isSelected, boolean cellHasFocus) {
            setBackground(isSelected ? new Color(20, 45, 70) : CARD_BG_2);
            left.setForeground(TEXT);
            left.setText("● " + value.peer.getIp() + ":" + value.peer.getPort());
            right.setText(value.chunks.size() + " chunk(s)");
            return this;
        }
    }

    static class ChunkRenderer extends JPanel implements ListCellRenderer<Integer> {
        private final JLabel left = new JLabel();
        private final JLabel right = new JLabel("click download");

        ChunkRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(8, 10, 8, 10));
            setOpaque(true);
            left.setFont(new Font("Segoe UI", Font.BOLD, 14));
            right.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            right.setForeground(MUTED);
            add(left, BorderLayout.WEST);
            add(right, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Integer> list, Integer value, int index, boolean isSelected, boolean cellHasFocus) {
            setBackground(isSelected ? new Color(20, 45, 70) : CARD_BG_2);
            left.setForeground(TEXT);
            left.setText("Chunk " + value);
            return this;
        }
    }

    // ================= CUSTOM PAINT =================

    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private final Color border;

        RoundedPanel(int radius, Color fill, Color border) {
            this.radius = radius;
            this.fill = fill;
            this.border = border;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

            if (border.getAlpha() > 0) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class GradientPanel extends JPanel {
        private final Color a;
        private final Color b;

        GradientPanel(Color a, Color b) {
            this.a = a;
            this.b = b;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2.setPaint(new GradientPaint(0, 0, a, getWidth(), 0, b));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(getWidth() / 2 - 40, -120, 360, 260);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class DashedPanel extends JPanel {
        DashedPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(16, 31, 49));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            float[] dash = {6f, 5f};
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0));
            g2.setColor(new Color(82, 102, 130));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class GradientButton extends JButton {
        private final Color a;
        private final Color b;

        GradientButton(String text, Color a, Color b) {
            super(text);
            this.a = a;
            this.b = b;

            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setPreferredSize(new Dimension(120, 42));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(new GradientPaint(0, 0, a, getWidth(), 0, b));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}