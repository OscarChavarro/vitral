#include "java/io/File.h"
#include "javax/xml/parsers/DocumentBuilder.h"
#include "javax/xml/transform/Transformer.h"
#include "org/w3c/dom/Document.h"
#include "org/w3c/dom/Element.h"
#include "org/w3c/dom/NodeList.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/io/XmlException.h"
#include "vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.h"
#include "vsdk/toolkit/io/geometry/ParametricCurvePersistence.h"
#include "io/XmlManager.h"

void XmlManager::exportXml(Geometry* object,
                           const String& outputFilename,
                           const String& dtdFilename)
{
    try {
        //- 1. Create a new empty Document --------------------------------
        Document* document =
            DocumentBuilder::newDocument();

        //- 2. Create an Element from the specified object ----------------
        Element* xmlElement = NULL;

        ParametricCurve* curve = dynamic_cast<ParametricCurve*>(object);
        ParametricBiCubicPatch* patch =
            dynamic_cast<ParametricBiCubicPatch*>(object);
        if ( curve != NULL ) {
            xmlElement = ParametricCurvePersistence::toElement(curve,
                                                              document);
        }
        else if ( patch != NULL ) {
            xmlElement = ParametricBiCubicPatchPersistence::toElement(patch,
                                                                     document);
        }

        //- 3. Add Element to Document ------------------------------------
        if ( xmlElement != NULL ) {
            document->appendChild(xmlElement);
        }

        document->normalizeDocument();

        //- 4. Export Document to File ------------------------------------
        Transformer xformer;
        xformer.setOutputProperty(
            Transformer::DOCTYPE_SYSTEM, dtdFilename);
        // Make resulting XML file more human-readable
        xformer.setOutputProperty(
            Transformer::INDENT, "yes");
        // Warning: what encoding to use?
        xformer.setOutputProperty(
            Transformer::ENCODING, "UTF-8");
        if ( !xformer.transform(document, File(outputFilename)) ) {
            Logger::reportMessage(String(), Logger::FATAL_ERROR,
                "exportXml", String("Can not write ") + outputFilename);
        }
        delete document;
    }
    catch ( const XmlException& ex ) {
        Logger::reportMessage(String(), Logger::FATAL_ERROR,
            "exportXml", ex.getMessage());
    }
}

Geometry* XmlManager::importXml(const String& inputFilename)
{
    //- 1. Create a Document from the XML input file ------------------
    Document* document =
        DocumentBuilder::parse(File(inputFilename));

    if ( document == NULL ) {
        Logger::reportMessage(String(), Logger::WARNING, "importXml",
            String("Can not parse ") + inputFilename);
        return NULL;
    }

    Geometry* result = NULL;
    try {
        //- 2. Extract objects from Document ------------------------------
        Node* rootNode = document->getDocumentElement();
        NodeList nodeList;

        if ( rootNode->getNodeName().equals(
                 ParametricCurvePersistence::rootName) ) {
            nodeList = document->getElementsByTagName(
                ParametricCurvePersistence::rootName);
            result = ParametricCurvePersistence::nodeToParametricCurve(
                nodeList.item(0));
        }
        else if ( rootNode->getNodeName().equals(
                      ParametricBiCubicPatchPersistence::rootName) ) {
            nodeList = document->getElementsByTagName(
                ParametricBiCubicPatchPersistence::rootName);
            result = ParametricBiCubicPatchPersistence::
                nodeToParametricBiCubicPatch(nodeList.item(0));
        }
        //-----------------------------------------------------------------
    }
    catch ( const XmlException& ex ) {
        Logger::reportMessage(String(), Logger::WARNING, "importXml",
                              ex.getMessage());
    }
    delete document;
    return result;
}
