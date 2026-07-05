#ifndef __POLYHEDRALBOUNDEDSOLIDTOPOLOGYEDITING__
#define __POLYHEDRALBOUNDEDSOLIDTOPOLOGYEDITING__

class PolyhedralBoundedSolid;

class PolyhedralBoundedSolidTopologyEditing {
public:
    static void loopGlue(PolyhedralBoundedSolid* solid, int faceId);
    static void compactIds(PolyhedralBoundedSolid* solid);
    static void maximizeFaces(PolyhedralBoundedSolid* solid);
    static int weldCoincidentVertices(PolyhedralBoundedSolid* solid);
};

#endif
