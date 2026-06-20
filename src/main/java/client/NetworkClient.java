package client;

import shared.AppConfig;
import shared.MessageType;
import shared.XmlMessageParser;

import javax.swing.*;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter writer;
    private InputStreamReader reader;

    private String sessionToken;
    private String currentUserRole;
    private String currentUsername;

    public interface MessageListener {
        void onMessage(MessageType type, Map<String, String> data);
    }

    private final List<MessageListener> listeners = new ArrayList<>();

    private NetworkClient() {}

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public boolean connect() {
        if (socket != null && !socket.isClosed()) return true;
        try {
            AppConfig config = AppConfig.getInstance();
            
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            
            socket = sc.getSocketFactory().createSocket(config.getServerHost(), config.getServerPort());
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);

            // Bắt đầu luồng lắng nghe liên tục
            Thread listenerThread = new Thread(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[1024];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, read);
                        int idx;
                        while ((idx = sb.indexOf(XmlMessageParser.DELIMITER)) != -1) {
                            String xml = sb.substring(0, idx);
                            sb.delete(0, idx + XmlMessageParser.DELIMITER.length());
                            
                            Map<String, String> data = XmlMessageParser.deserialize(xml);
                            String typeStr = data.get("type");
                            if (typeStr != null) {
                                try {
                                    MessageType type = MessageType.valueOf(typeStr);
                                    notifyListeners(type, data);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Mất kết nối tới Server.");
                    handleReconnect();
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();
            
            // Luồng Heartbeat
            Thread heartbeatThread = new Thread(() -> {
                while (!socket.isClosed()) {
                    try {
                        Thread.sleep(30000); // 30s
                        sendMessage(MessageType.HEARTBEAT, null);
                    } catch (Exception e) { break; }
                }
            });
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void handleReconnect() {
        int attempts = 0;
        boolean connected = false;
        while (attempts < 3 && !connected) {
            try {
                Thread.sleep(5000);
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Đang thử kết nối lại...");
                });
                if (connect()) {
                    connected = true;
                    // Re-auth ngầm nếu có sessionToken (Tuỳ chọn bổ sung)
                    if (sessionToken != null) {
                        // Demo: Ta có thể bỏ qua bước re-auth nếu server validate token linh hoạt
                    }
                }
            } catch (Exception e) {}
            attempts++;
        }
        if (!connected) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Không thể kết nối lại sau 3 lần thử. Vui lòng khởi động lại ứng dụng.", "Lỗi Mạng", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            });
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket = null;
        writer = null;
        reader = null;
        sessionToken = null;
        currentUserRole = null;
        currentUsername = null;
    }

    public void sendMessage(MessageType type, Map<String, String> fields) {
        if (writer != null) {
            if (fields == null) fields = new java.util.HashMap<>();
            if (sessionToken != null) fields.put("sessionToken", sessionToken);
            String xml = XmlMessageParser.serialize(type, fields);
            writer.print(xml);
            writer.flush();
        }
    }

    public synchronized void addListener(MessageListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public synchronized void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            List<MessageListener> copy;
            synchronized (this) {
                copy = new ArrayList<>(listeners);
            }
            for (MessageListener listener : copy) {
                listener.onMessage(type, data);
            }
        });
    }

    public void setSessionInfo(String token, String role, String username) {
        this.sessionToken = token;
        this.currentUserRole = role;
        this.currentUsername = username;
    }

    public String getSessionToken() { return sessionToken; }
    public String getCurrentUserRole() { return currentUserRole; }
    public String getCurrentUsername() { return currentUsername; }
}
