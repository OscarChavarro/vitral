#ifndef __PARAMETRIC_CURVE_PERSISTENCE__
#define __PARAMETRIC_CURVE_PERSISTENCE__

#include "java/lang/String.h"

class Document;
class Element;
class Node;

class ParametricCurve;

/**
XML persistence for `ParametricCurve`.
*/
class ParametricCurvePersistence {
public:
    static const char* const rootName;

    /**
    @param nodeRoot XML node with a `ParametricCurve` element
    @return a new curve, owned by the caller
    @throws XmlException if the node is not a curve
    */
    static ParametricCurve* nodeToParametricCurve(
        const Node* nodeRoot);

    /**
    @param curve curve to export
    @param document document used to create the nodes
    @return a new element owned by the caller (until appended to the tree)
    */
    static Element* toElement(ParametricCurve* curve,
                                            Document* document);
};

#endif
