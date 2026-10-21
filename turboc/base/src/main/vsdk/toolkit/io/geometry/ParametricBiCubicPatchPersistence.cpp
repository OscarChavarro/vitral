#include <stdlib.h>

#include "org/w3c/dom/Document.h"
#include "org/w3c/dom/Element.h"
#include "org/w3c/dom/NamedNodeMap.h"
#include "org/w3c/dom/NodeList.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/io/XmlException.h"
#include "vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.h"
#include "vsdk/toolkit/io/geometry/ParametricCurvePersistence.h"

const char* const ParametricBiCubicPatchPersistence::rootName =
    "ParametricBiCubicPatch";

static const char* const patchAttributesNames[] = {
    "type", "approximationSteps"};

ParametricBiCubicPatch*
ParametricBiCubicPatchPersistence::nodeToParametricBiCubicPatch(
    const Node* nodeRoot)
{
    if ( nodeRoot == NULL || !nodeRoot->getNodeName().equals(rootName) ) {
        throw XmlException("The node no is a patch ");
    }
    ParametricBiCubicPatch* patch = NULL;
    const NamedNodeMap* atts = nodeRoot->getAttributes();
    if ( atts != NULL ) {
        Node* atributo;
        atributo = atts->getNamedItem(patchAttributesNames[0]);
        if ( atributo != NULL ) {
            String type = atributo->getNodeValue();
            if ( !type.equals("") ) {
                patch = new ParametricBiCubicPatch();
                patch->setType(atoi(type.c_str()));
                atributo = atts->getNamedItem(patchAttributesNames[1]);
                if ( atributo != NULL ) {
                    String approximationSteps = atributo->getNodeValue();
                    if ( !approximationSteps.equals("") ) {
                        patch->setApproximationSteps(
                            atoi(approximationSteps.c_str()));
                    }
                }

                NodeList nodeList = nodeRoot->getChildNodes();
                // patch.contourCurve = the first curve node
                for ( int i = 0; i < nodeList.getLength(); i++ ) {
                    Node* nodeCurve = nodeList.item(i);
                    patch->contourCurve = ParametricCurvePersistence::
                        nodeToParametricCurve(nodeCurve);
                    if ( patch->contourCurve != NULL ) {
                        break;
                    }
                }
            }
        }
    }

    return patch;
}

Element* ParametricBiCubicPatchPersistence::toElement(
    ParametricBiCubicPatch* patch, Document* document)
{
    Element* nodeRoot = document->createElement(rootName);

    nodeRoot->setAttribute(patchAttributesNames[1],
        String::valueOf(patch->getApproximationSteps()));
    nodeRoot->setAttribute(patchAttributesNames[0],
        String::valueOf(patch->getType()));

    if ( patch->contourCurve != NULL ) {
        Element* eCurve;
        eCurve = ParametricCurvePersistence::toElement(patch->contourCurve,
                                                       document);
        nodeRoot->appendChild(eCurve);
    }
    return nodeRoot;
}
