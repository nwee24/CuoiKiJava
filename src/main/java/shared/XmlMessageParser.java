package shared;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.InputSource;

public class XmlMessageParser {
    public static final String DELIMITER = "##END##";

    public static String serialize(MessageType type, Map<String, String> fields) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("message");
            doc.appendChild(rootElement);

            Element typeElement = doc.createElement("type");
            typeElement.appendChild(doc.createTextNode(type.name()));
            rootElement.appendChild(typeElement);

            if (fields != null) {
                for (Map.Entry<String, String> entry : fields.entrySet()) {
                    Element el = doc.createElement(entry.getKey());
                    String value = entry.getValue() != null ? entry.getValue() : "";
                    // Dữ liệu văn bản dài tự động được DOM xử lý escaping
                    el.appendChild(doc.createTextNode(value));
                    rootElement.appendChild(el);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            
            // Không xóa ký tự xuống dòng vì description sản phẩm có thể chứa xuống dòng
            return writer.getBuffer().toString() + DELIMITER;
        } catch (Exception e) {
            e.printStackTrace();
            return "<message><type>ERROR</type><content>Serialize Error</content></message>" + DELIMITER;
        }
    }

    public static Map<String, String> deserialize(String xmlStr) {
        Map<String, String> map = new HashMap<>();
        try {
            // Loại bỏ DELIMITER nếu có
            if (xmlStr.endsWith(DELIMITER)) {
                xmlStr = xmlStr.substring(0, xmlStr.length() - DELIMITER.length());
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            NodeList nodeList = root.getChildNodes();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    map.put(node.getNodeName(), node.getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("type", MessageType.ERROR.name());
            map.put("content", "Deserialize Error: " + e.getMessage());
        }
        return map;
    }
}
