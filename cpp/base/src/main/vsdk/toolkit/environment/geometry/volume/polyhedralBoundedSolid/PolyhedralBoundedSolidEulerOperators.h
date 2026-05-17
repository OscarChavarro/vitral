#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_POLYHEDRALBOUNDEDSOLID_POLYHEDRALBOUNDEDSOLIDEULEROPERATORS_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_POLYHEDRALBOUNDEDSOLID_POLYHEDRALBOUNDEDSOLIDEULEROPERATORS_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class PolyhedralBoundedSolid;

class PolyhedralBoundedSolidEulerOperators {
public:
    static void mvfs(PolyhedralBoundedSolid* solid, const Vector3Dd& pos, int vertexId, int faceId);
    static void smev(PolyhedralBoundedSolid* solid, int seedSolidId, int fromVertexId, int toVertexId, const Vector3Dd& pos);
    static void mef(PolyhedralBoundedSolid* solid, int seedSolidId, int seedFaceId,
                    int startHalfEdge1, int endHalfEdge1,
                    int startHalfEdge2, int endHalfEdge2,
                    int newFaceId);
    static void mef(PolyhedralBoundedSolid* solid, int seedSolidId, int seedFaceId,
                    int startHalfEdge1, int endHalfEdge1,
                    int startHalfEdge2, int endHalfEdge2);
    static void smef(PolyhedralBoundedSolid* solid, int seedFaceId,
                     int startVertexId, int endVertexId,
                     int newFaceId);
};

#endif
