#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "java/util/StringTokenizer.h"
#include "org/w3c/dom/Document.h"
#include "org/w3c/dom/Element.h"
#include "org/w3c/dom/NamedNodeMap.h"
#include "org/w3c/dom/NodeList.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/io/XmlException.h"
#include "vsdk/toolkit/io/geometry/ParametricCurvePersistence.h"

const char* const ParametricCurvePersistence::rootName = "ParametricCurve";

static const char* const nodesNames[] = {
    "Point",
    "Vector3Dd",
};
static const char* const curveAttributesNames[] = {
    "approximationSteps"};
static const char* const pointAttributesNames[] = {
    "type"};

// Full precision, so that exported coordinates read back exactly
static String formatCoordinate(double value)
{
    char buffer[64];
    snprintf(buffer, sizeof(buffer), "%.17g", value);
    return String(buffer);
}

ParametricCurve* ParametricCurvePersistence::nodeToParametricCurve(
    const Node* nodeRoot)
{
    if ( nodeRoot == NULL || !nodeRoot->getNodeName().equals(rootName) ) {
        throw XmlException("The node no is a curve ");
    }
    ParametricCurve* curve = new ParametricCurve();
    const NamedNodeMap* atts = nodeRoot->getAttributes();
    if ( atts != NULL ) {
        Node* atributo;
        atributo = atts->getNamedItem(curveAttributesNames[0]);
        if ( atributo != NULL ) {
            String approximationSteps = atributo->getNodeValue();
            if ( !approximationSteps.equals("") ) {
                curve->setApproximationSteps(
                    atoi(approximationSteps.c_str()));
            }
        }
    }

    NodeList nodeList = nodeRoot->getChildNodes();
    for ( int i = 0; i < nodeList.getLength(); i++ ) {
        Node* nodo;
        nodo = nodeList.item(i);

        NodeList subNodos = nodo->getChildNodes();
        ArrayList<Vector3Dd> v3;
        for ( int j = 0; j < subNodos.getLength(); j++ ) {
            Node* subNodo = subNodos.item(j);
            if ( subNodo->getLocalName() != NULL &&
                 subNodo->getLocalName()->equals(nodesNames[1]) ) {
                StringTokenizer coors(subNodo->getTextContent());
                double coordinates[3] = {0.0, 0.0, 0.0};
                int k;
                for ( k = 0; k < 3 && coors.hasMoreTokens(); k++ ) {
                    coordinates[k] = atof(coors.nextToken().c_str());
                }
                v3.add(Vector3Dd(coordinates[0], coordinates[1],
                                 coordinates[2]));
            }
        }

        atts = nodo->getAttributes();
        if ( atts != NULL ) {
            Node* atributo =
                atts->getNamedItem(pointAttributesNames[0]);
            if ( atributo == NULL ) {
                continue;
            }
            String type = atributo->getNodeValue();
            if ( !type.equals("") ) {
                curve->addPoint(v3, atoi(type.c_str()));
            }
        }
    }

    return curve;
}

Element* ParametricCurvePersistence::toElement(
    ParametricCurve* curve, Document* document)
{
    Element* nodeCurve = document->createElement(rootName);

    nodeCurve->setAttribute(curveAttributesNames[0],
        String::valueOf(curve->getApproximationSteps()));
    for ( int i = 0; i < curve->getPointSize(); i++ ) {
        Element* ePoint =
            document->createElement(nodesNames[0]);
        ePoint->setAttribute(pointAttributesNames[0],
            String::valueOf(curve->getPointType(i)));
        const ArrayList<Vector3Dd>& v3 = curve->getPointVector(i);
        for ( int j = 0; j < v3.size(); j++ ) {
            Element* eVector =
                document->createElement(nodesNames[1]);
            const Vector3Dd& v = v3[j];
            eVector->setTextContent(formatCoordinate(v.x()) + " " +
                formatCoordinate(v.y()) + " " + formatCoordinate(v.z()));
            ePoint->appendChild(eVector);
        }
        nodeCurve->appendChild(ePoint);
    }

    return nodeCurve;
}
