import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.io.XmlException;
import vsdk.toolkit.io.geometry.ParametricCurvePersistence;
import vsdk.toolkit.io.geometry.ParametricBiCubicPatchPersistence;

public class XmlManager {
  public XmlManager() {
  }

  public static Element exportXml(Object object,
                                  String fileName,
                                  String dtdFilename) throws XmlException {
    Document document;

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(true);
      factory.setNamespaceAware(true);
      DocumentBuilder builder = null;
      builder = factory.newDocumentBuilder();
      document = builder.newDocument();
      Element nodeRoot = null;
      if (object.getClass().getName().equals(
          "vsdk.toolkit.environment.geometry.ParametricCurve")) {
        ParametricCurve curve = (ParametricCurve) object;
/*
        System.out.print("curve to export with: " + curve.getPointSize() +
                         " points");
*/
        nodeRoot = ParametricCurvePersistence.toElement(curve, document);
      }
      else if (object.getClass().getName().equals(
          "vsdk.toolkit.environment.geometry.ParametricBiCubicPatch")) {
        ParametricBiCubicPatch patch = (ParametricBiCubicPatch) object;
/*
        System.out.print("type patch to export: " + patch.getType());
*/
        nodeRoot = ParametricBiCubicPatchPersistence.toElement(patch, document);
      }

      document.appendChild(nodeRoot);
      document.normalizeDocument();

      // Prepare the output file
      Source source = new DOMSource(document);
      File file = new File(fileName);
      Result result = new StreamResult(file);

      // Write the DOM document to the file
      Transformer xformer = null;
      xformer = TransformerFactory.newInstance().newTransformer();
      xformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                                dtdFilename);
      xformer.transform(source, result);
      document = builder.parse(file);
      return nodeRoot;
    }
    catch (ParserConfigurationException ex) {
      new XmlException("Exception: " + ex);
    }
    catch (TransformerFactoryConfigurationError ex1) {
      new XmlException("Exception: " + ex1);
    }
    catch (TransformerConfigurationException ex1) {
      new XmlException("Exception: " + ex1);
    }
    catch (TransformerException ex2) {
      new XmlException("Exception: " + ex2);
    }
    catch (IOException ex3) {
      new XmlException("Exception: " + ex3);
    }
    catch (SAXException ex3) {
      new XmlException("Exception: " + ex3);
    }
    return null;
  }

  public static Object importXml(String fileNameIn) throws
      XmlException {
    Document document;
    NodeList nodeList;

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(true);
    factory.setNamespaceAware(true);

    DocumentBuilder builder = null;

    try {

      builder = factory.newDocumentBuilder();
      document = builder.parse(new File(fileNameIn));

      Node rootNode = document.getFirstChild();

      if (rootNode.getNodeName() == ParametricCurvePersistence.rootName) {
        nodeList = document.getElementsByTagName(
            ParametricCurvePersistence.rootName);
        Node firstCuve = nodeList.item(0);
        ParametricCurve curve = null;

        curve = ParametricCurvePersistence.nodeToParametricCurve(
            firstCuve);
/*
        System.out.println("curva con: " + curve.getPointSize() + " puntos");
*/
        return curve;
      }
      else if (rootNode.getNodeName() ==
               ParametricBiCubicPatchPersistence.rootName) {
        nodeList = document.getElementsByTagName(
            ParametricBiCubicPatchPersistence.rootName);
        Node firstCuve = nodeList.item(0);
        ParametricBiCubicPatch patch = null;

        patch = ParametricBiCubicPatchPersistence.nodeToParametricBiCubicPatch(
            firstCuve);

/*
        System.out.println("type patch: " + patch.getType());
*/
        return patch;
      }

    }

    catch (ParserConfigurationException ex) {
    }
    catch (XmlException ex1) {
      new XmlException(ex1);

    }

    catch (IOException ex2) {
    }
    catch (SAXException ex2) {
    }
    return null;
  }

  public static void main(String argv[]) throws IOException, SAXException {
    String dtdFileName = "\\etc\\xml\\vsdk.dtd";
    String absolutePath = new File("").getAbsolutePath().
        replace("\\JBuilder2005", "").replace("\\_ide", "");

    String fileNameIn = absolutePath + "\\etc\\xml\\patch.xml";
    Object obj = null;
    try {
      obj = importXml(fileNameIn);
      if (obj.getClass().getName().equals(
          "vsdk.toolkit.environment.geometry.ParametricCurve")) {
        ParametricCurve curve = (ParametricCurve) obj;
/*
        System.out.print("curve with: " + curve.getPointSize() + " points");
*/
      }
      else if (obj.getClass().getName().equals(
          "vsdk.toolkit.environment.geometry.ParametricBiCubicPatch")) {
        ParametricBiCubicPatch patch = (ParametricBiCubicPatch) obj;
/*
        System.out.print("type patch: " + patch.getType());
*/
      }
    }
    catch (XmlException ex) {
    }

    if (obj != null) {
      try {
        exportXml(obj, absolutePath + "\\etc\\xml\\Out.xml",
                  absolutePath + dtdFileName);
      }
      catch (XmlException ex1) {
      }
    }

  } // main

}
