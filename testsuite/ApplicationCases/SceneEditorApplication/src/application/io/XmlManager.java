//===========================================================================

package application.io;

// JDK basic classes
import java.io.File;
import java.io.IOException;

// XML classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.io.XmlException;
import vsdk.toolkit.io.geometry.ParametricCurvePersistence;
import vsdk.toolkit.io.geometry.ParametricBiCubicPatchPersistence;

public class XmlManager {
    public static void exportXml(Object object, String outputFilename,
                                 String dtdFilename) throws XmlException {
        try {
            //- 1. Create a new empty Document --------------------------------
            DocumentBuilderFactory factory;
            DocumentBuilder builder;
            Document document;

            factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false); // Warning: check what it means
            factory.setNamespaceAware(true); // Warning: check what it means
            builder = factory.newDocumentBuilder();
            document = builder.newDocument();

            //- 2. Create an Element from the specified object ----------------
            Element xmlElement;
            xmlElement = null;

            if ( object instanceof ParametricCurve)  {
                ParametricCurve curve = (ParametricCurve) object;
                xmlElement = ParametricCurvePersistence.toElement(curve, 
                                                                  document);
            }
            else if ( object instanceof ParametricBiCubicPatch ) {
                ParametricBiCubicPatch patch = (ParametricBiCubicPatch) object;
                xmlElement = ParametricBiCubicPatchPersistence.toElement(patch,
                                                                     document);
            }

            //- 3. Add Element to Document ------------------------------------
            if ( xmlElement != null ) {
                document.appendChild(xmlElement);
            }

            document.normalizeDocument();

            //- 4. Export Document to File ------------------------------------
            Source source = new DOMSource(document);
            Result result = new StreamResult(new File(outputFilename));

            Transformer xformer;
            xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                                      dtdFilename);
            // Make resulting XML file more human-readable
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // Warning: what encoding to use?
            xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            xformer.transform(source, result);
        }
        catch ( ParserConfigurationException | XmlException | DOMException | 
                TransformerFactoryConfigurationError | 
                IllegalArgumentException | TransformerException ex ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, "exportXml", "" + ex);
        }
    }

    public static Object importXml(String inputFilename) throws
        XmlException {
        try {
            //- 1. Create a Document from the XML input file ------------------
            DocumentBuilderFactory factory;
            DocumentBuilder builder;
            Document document;

            factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false); // Warning: check what it means
            factory.setNamespaceAware(true); // Warning: check what it means
            builder = factory.newDocumentBuilder();
            document = builder.parse(new File(inputFilename));

            //- 2. Extract objects from Document ------------------------------
            Node rootNode = document.getFirstChild();
            NodeList nodeList;

            if ( rootNode.getNodeName().equals( 
                 ParametricCurvePersistence.rootName) ) {
                nodeList = document.getElementsByTagName(
                    ParametricCurvePersistence.rootName);
                Node firstNode = nodeList.item(0);
                ParametricCurve curve;
                curve = ParametricCurvePersistence.nodeToParametricCurve(
                    firstNode);
                return curve;
            }
            else if ( rootNode.getNodeName().equals(
                      ParametricBiCubicPatchPersistence.rootName) ) {
                nodeList = document.getElementsByTagName(
                    ParametricBiCubicPatchPersistence.rootName);
                Node firstNode = nodeList.item(0);
                ParametricBiCubicPatch patch;
                patch = ParametricBiCubicPatchPersistence.
                    nodeToParametricBiCubicPatch(firstNode);
                return patch;
            }
            //-----------------------------------------------------------------
        }
        catch ( ParserConfigurationException | SAXException | IOException | 
                XmlException ex ) {
            VSDK.reportMessage(null, VSDK.WARNING, "importXml", "" + ex);
        }
        return null;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
