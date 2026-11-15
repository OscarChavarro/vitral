#ifndef __PRMTRC_B_CBC_PTCH_PRSSTNC__
#define __PRMTRC_B_CBC_PTCH_PRSSTNC__

class Document;
class Element;
class Node;

class ParametricBiCubicPatch;

/**
XML persistence for `ParametricBiCubicPatch`.
*/
class ParametricBiCubicPatchPersistence {
public:
    static const char* const rootName;

    /**
    @param nodeRoot XML node with a `ParametricBiCubicPatch` element
    @return a new patch owned by the caller, or null if it has no type
    @throws XmlException if the node is not a patch
    */
    static ParametricBiCubicPatch* nodeToParametricBiCubicPatch(
        const Node* nodeRoot);

    /**
    @param patch patch to export
    @param document document used to create the nodes
    @return a new element owned by the caller (until appended to the tree)
    */
    static Element* toElement(ParametricBiCubicPatch* patch,
                                            Document* document);
};

#endif
