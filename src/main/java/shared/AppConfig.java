package shared;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

public class AppConfig {
    private static AppConfig instance;
    
    // Server config
    private String serverHost;
    private int serverPort;
    private String keystore;
    private String keystorePassword;
    private int threadPoolSize;
    
    // Database config
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    
    // Auction config
    private int defaultDurationSeconds;
    private long minBidIncrement;
    private int extensionSeconds;
    private int extensionThresholdSeconds;
    private double commissionPercent;
    private long fixedFee;
    private double penaltyPercent;

    private AppConfig() {
        loadConfig();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadConfig() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("config.xml");
            if (is == null) {
                throw new RuntimeException("Cannot find config.xml in classpath");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);
            document.getDocumentElement().normalize();

            // Load Server Config
            Element serverElement = (Element) document.getElementsByTagName("server").item(0);
            serverHost = getTagValue("host", serverElement);
            serverPort = Integer.parseInt(getTagValue("port", serverElement));
            keystore = getTagValue("keystore", serverElement);
            keystorePassword = getTagValue("keystorePassword", serverElement);
            threadPoolSize = Integer.parseInt(getTagValue("threadPoolSize", serverElement));

            // Load Database Config
            Element dbElement = (Element) document.getElementsByTagName("database").item(0);
            dbUrl = getTagValue("url", dbElement);
            dbUsername = getTagValue("username", dbElement);
            dbPassword = getTagValue("password", dbElement);

            // Load Auction Config
            Element auctionElement = (Element) document.getElementsByTagName("auction").item(0);
            defaultDurationSeconds = Integer.parseInt(getTagValue("defaultDurationSeconds", auctionElement));
            minBidIncrement = Long.parseLong(getTagValue("minBidIncrement", auctionElement));
            extensionSeconds = Integer.parseInt(getTagValue("extensionSeconds", auctionElement));
            extensionThresholdSeconds = Integer.parseInt(getTagValue("extensionThresholdSeconds", auctionElement));
            commissionPercent = Double.parseDouble(getTagValue("commissionPercent", auctionElement));
            fixedFee = Long.parseLong(getTagValue("fixedFee", auctionElement));
            penaltyPercent = Double.parseDouble(getTagValue("penaltyPercent", auctionElement));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load config.xml", e);
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getNodeValue();
        }
        return "";
    }

    // Getters for all properties
    public String getServerHost() { return serverHost; }
    public int getServerPort() { return serverPort; }
    public String getKeystore() { return keystore; }
    public String getKeystorePassword() { return keystorePassword; }
    public int getThreadPoolSize() { return threadPoolSize; }

    public String getDbUrl() { return dbUrl; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }

    public int getDefaultDurationSeconds() { return defaultDurationSeconds; }
    public long getMinBidIncrement() { return minBidIncrement; }
    public int getExtensionSeconds() { return extensionSeconds; }
    public int getExtensionThresholdSeconds() { return extensionThresholdSeconds; }
    public double getCommissionPercent() { return commissionPercent; }
    public long getFixedFee() { return fixedFee; }
    public double getPenaltyPercent() { return penaltyPercent; }
}
