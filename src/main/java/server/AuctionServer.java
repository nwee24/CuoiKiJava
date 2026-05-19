package server;

import shared.AppConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private int port;
    private ExecutorService threadPool;

    public AuctionServer() {
        AppConfig config = AppConfig.getInstance();
        this.port = config.getServerPort();
        this.threadPool = Executors.newFixedThreadPool(config.getThreadPoolSize());
    }

    public void start() {
        try {
            AppConfig config = AppConfig.getInstance();
            
            // Cài đặt SSL từ keystore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream ksIs = getClass().getClassLoader().getResourceAsStream(config.getKeystore());
            
            if (ksIs == null) {
                throw new RuntimeException("Lỗi: Không tìm thấy file " + config.getKeystore() + " trong thư mục resources!");
            }
            
            keyStore.load(ksIs, config.getKeystorePassword().toCharArray());
            ksIs.close();
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, config.getKeystorePassword().toCharArray());
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);
            
            System.out.println("[Hệ thống] Auction SSL Server (Bảo mật TLS) đang chạy trên port " + port);

            // Vòng lặp nhận kết nối từ Client
            while (true) {
                java.net.Socket clientSocket = serverSocket.accept();
                System.out.println("[Hệ thống] Client mới kết nối bảo mật từ: " + clientSocket.getInetAddress());
                
                // Submit handler vào Thread Pool để xử lý đa luồng
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (Exception e) {
            System.err.println("[Lỗi Server] Không thể khởi động SSL Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}
