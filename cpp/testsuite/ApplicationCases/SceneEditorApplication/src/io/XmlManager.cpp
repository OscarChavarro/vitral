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
                           const java::String& outputFilename,
                           const java::String& dtdFilename)
{
    try {
        //- 1. Create a new empty Document --------------------------------
        org::w3c::dom::Document* document =
            javax::xml::parsers::DocumentBuilder::newDocument();

        //- 2. Create an Element from the specified object ----------------
        org::w3c::dom::Element* xmlElement = nullptr;

        ParametricCurve* curve = dynamic_cast<ParametricCurve*>(object);
        ParametricBiCubicPatch* patch =
            dynamic_cast<ParametricBiCubicPatch*>(object);
        if ( curve != nullptr ) {
            xmlElement = ParametricCurvePersistence::toElement(curve,
                                                              document);
        }
        else if ( patch != nullptr ) {
            xmlElement = ParametricBiCubicPatchPersistence::toElement(patch,
                                                                     document);
        }

        //- 3. Add Element to Document ------------------------------------
        if ( xmlElement != nullptr ) {
            document->appendChild(xmlElement);
        }

        document->normalizeDocument();

        //- 4. Export Document to File ------------------------------------
        javax::xml::transform::Transformer xformer;
        xformer.setOutputProperty(
            javax::xml::transform::Transformer::DOCTYPE_SYSTEM, dtdFilename);
        // Make resulting XML file more human-readable
        xformer.setOutputProperty(
            javax::xml::transform::Transformer::INDENT, "yes");
        // Warning: what encoding to use?
        xformer.setOutputProperty(
            javax::xml::transform::Transformer::ENCODING, "UTF-8");
        if ( !xformer.transform(document, java::File(outputFilename)) ) {
            Logger::reportMessage(java::String(), Logger::FATAL_ERROR,
                "exportXml", java::String("Can not write ") + outputFilename);
        }
        delete document;
    }
    catch ( const XmlException& ex ) {
        Logger::reportMessage(java::String(), Logger::FATAL_ERROR,
            "exportXml", ex.getMessage());
    }
}

Geometry* XmlManager::importXml(const java::String& inputFilename)
{
    //- 1. Create a Document from the XML input file ------------------
    org::w3c::dom::Document* document =
        javax::xml::parsers::DocumentBuilder::parse(java::File(inputFilename));

    if ( document == nullptr ) {
        Logger::reportMessage(java::String(), Logger::WARNING, "importXml",
            java::String("Can not parse ") + inputFilename);
        return nullptr;
    }

    Geometry* result = nullptr;
    try {
        //- 2. Extract objects from Document ------------------------------
        org::w3c::dom::Node* rootNode = document->getDocumentElement();
        org::w3c::dom::NodeList nodeList;

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
        Logger::reportMessage(java::String(), Logger::WARNING, "importXml",
                              ex.getMessage());
    }
    delete document;
    return result;
}
