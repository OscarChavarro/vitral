#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_POLYHEDRALBOUNDEDSOLID_POLYHEDRALBOUNDEDSOLIDEULEROPERATORS_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_POLYHEDRALBOUNDEDSOLID_POLYHEDRALBOUNDEDSOLIDEULEROPERATORS_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidLoop;
class _PolyhedralBoundedSolidHalfEdge;

class PolyhedralBoundedSolidEulerOperators {
public:
    static void mvfs(PolyhedralBoundedSolid* solid, const Vector3Dd& pos, int vertexId, int faceId);
    static void kvfs(PolyhedralBoundedSolid* solid);

    static void lmev(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidHalfEdge* he1, _PolyhedralBoundedSolidHalfEdge* he2, int vertexId, const Vector3Dd& p);
    static void lkev(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidHalfEdge* he1, _PolyhedralBoundedSolidHalfEdge* he2);
    static void lkef(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidHalfEdge* he1, _PolyhedralBoundedSolidHalfEdge* he2);
    static _PolyhedralBoundedSolidFace* lmef(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidHalfEdge* he1, _PolyhedralBoundedSolidHalfEdge* he2, int newFaceId);

    static bool lringmv(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidLoop* l, _PolyhedralBoundedSolidFace* toFace, bool setAsOuterLoop);

    static bool mev(PolyhedralBoundedSolid* solid, int f1, int f2, int v1, int v2, int newVertexId, const Vector3Dd& p);
    static bool kemr(PolyhedralBoundedSolid* solid, int f1, int f2, int v1, int v2, int v3, int v4);
    static bool kfmrh(PolyhedralBoundedSolid* solid, int f1, int f2);

    // compatibility overloads already used by existing C++ code
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
