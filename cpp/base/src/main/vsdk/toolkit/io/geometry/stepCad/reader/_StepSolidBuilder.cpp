#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepSolidBuilder.h"

#include "java/lang/Double.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/System.h"
#include "java/util/HashMapIterationOrder.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepTokenizer.h"

_StepSolidBuilder::_StepSolidBuilder(const _StepEntityMap& inEntities)
    : entities(inEntities)
{
    this->unitToMetre = detectLengthUnitScale();
}

PolyhedralBoundedSolid* _StepSolidBuilder::build(const _StepEntityMap& entities)
{
    _StepSolidBuilder builder(entities);
    return builder.buildInternal();
}

//=================================================================

PolyhedralBoundedSolid* _StepSolidBuilder::buildInternal()
{
    int closedShellId = findClosedShellId();
    java::ArrayList<int> faceIds = collectFaceIds(closedShellId);

    collectEdgeCurveEndpoints(faceIds);

    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();

    // C++ port: Java leaves a half-built solid to the garbage collector
    // when a pass throws
    try {
        pass1CreateVertices(solid);
        pass2CreateFacesAndHalfEdges(solid, faceIds);
        pass3WireEdges(solid);
        pass4SetEmanatingHalfEdges();
    }
    catch ( ... ) {
        delete solid;
        throw;
    }

    updateMaxIds(solid);
    return solid;
}

//=================================================================
//= TRAVERSAL =====================================================

int _StepSolidBuilder::findClosedShellId()
{
    java::ArrayList<int> ids = entities.keysInJavaOrder();
    long int i;
    for ( i = 0; i < ids.size(); i++ ) {
        _StepEntity* e = entities.get(ids.get(i));
        if ( e->name == "MANIFOLD_SOLID_BREP" ) {
            return _StepTokenizer::parseRef(e->params.get(1));
        }
    }
    throw java::IllegalStateException(
        "STEP file contains no MANIFOLD_SOLID_BREP entity.");
}

java::ArrayList<int> _StepSolidBuilder::collectFaceIds(int closedShellId)
{
    _StepEntity* shell = require(closedShellId, "CLOSED_SHELL");
    java::ArrayList<java::String> faceAgg =
        _StepTokenizer::parseAggregate(shell->params.get(1));
    java::ArrayList<int> ids;
    long int i;
    for ( i = 0; i < faceAgg.size(); i++ ) {
        ids.add(_StepTokenizer::parseRef(faceAgg.get(i)));
    }
    return ids;
}

/**
Pre-collects the start/end VERTEX_POINT ids for every EDGE_CURVE
reachable from the given ADVANCED_FACE list so that Pass 2 can
resolve vertex references without re-traversal.

Vertex positions are taken exclusively from EDGE_CURVE params[1]
(start VERTEX_POINT) and params[2] (end VERTEX_POINT).  The
geometry entity in params[3] (LINE, SURFACE_CURVE,
B_SPLINE_CURVE_WITH_KNOTS, etc.) is inspected only for diagnostic
logging and does not affect topology reconstruction.
*/
void _StepSolidBuilder::collectEdgeCurveEndpoints(
    const java::ArrayList<int>& faceIds)
{
    long int i;
    long int j;
    long int k;
    for ( i = 0; i < faceIds.size(); i++ ) {
        int faceId = faceIds.get(i);
        _StepEntity* face = require(faceId, "ADVANCED_FACE");
        java::ArrayList<java::String> boundsAgg =
            _StepTokenizer::parseAggregate(face->params.get(1));
        for ( j = 0; j < boundsAgg.size(); j++ ) {
            int boundId = _StepTokenizer::parseRef(boundsAgg.get(j));
            _StepEntity* bound = entities.get(boundId);
            if ( bound == nullptr ) {
                continue;
            }
            int loopId = _StepTokenizer::parseRef(bound->params.get(1));
            _StepEntity* loop = require(loopId, "EDGE_LOOP");
            java::ArrayList<java::String> oeAgg =
                _StepTokenizer::parseAggregate(loop->params.get(1));
            for ( k = 0; k < oeAgg.size(); k++ ) {
                int oeId = _StepTokenizer::parseRef(oeAgg.get(k));
                _StepEntity* oe = require(oeId, "ORIENTED_EDGE");
                int ecId = _StepTokenizer::parseRef(oe->params.get(3));
                if ( ecStartVpId.containsKey(ecId) ) {
                    continue;
                }
                _StepEntity* ec = require(ecId, "EDGE_CURVE");
                ecStartVpId.put(ecId, _StepTokenizer::parseRef(ec->params.get(1)));
                ecEndVpId.put(ecId, _StepTokenizer::parseRef(ec->params.get(2)));
                ecIdsInInsertionOrder.add(ecId);
                logEdgeGeometryType(ecId, ec);
            }
        }
    }
}

/**
Resolves and logs the effective 3D geometry type for the given
EDGE_CURVE.  SURFACE_CURVE wrappers are unwrapped one level to
expose the underlying curve (LINE or B_SPLINE_CURVE_WITH_KNOTS).
B_SPLINE edges are accepted and treated as straight-line segments
because the faces are assumed to be planarised.
*/
void _StepSolidBuilder::logEdgeGeometryType(int ecId, _StepEntity* ec)
{
    if ( ec->params.size() < 4 ) {
        return;
    }
    java::String geomTok = ec->params.get(3).trim();
    if ( geomTok.isEmpty() || geomTok.charAt(0) != '#' ) {
        return;
    }
    int geomId;
    try {
        geomId = _StepTokenizer::parseRef(geomTok);
    }
    catch ( const java::IllegalArgumentException& ) {
        return;
    }
    _StepEntity* geomEntity = entities.get(geomId);
    if ( geomEntity == nullptr ) {
        return;
    }
    java::String geomName = geomEntity->name;
    if ( geomName == "SURFACE_CURVE" && geomEntity->params.size() >= 2 ) {
        java::String innerTok = geomEntity->params.get(1).trim();
        if ( !innerTok.isEmpty() && innerTok.charAt(0) == '#' ) {
            try {
                int innerGeomId = _StepTokenizer::parseRef(innerTok);
                _StepEntity* innerGeom = entities.get(innerGeomId);
                if ( innerGeom != nullptr ) {
                    geomName = java::String("SURFACE_CURVE->") + innerGeom->name;
                    if ( innerGeom->name == "B_SPLINE_CURVE_WITH_KNOTS" ) {
                        java::System::err.println((
                            java::String("[StepReader] EDGE_CURVE #") +
                            java::String::valueOf(ecId) +
                            ": geometry is " + geomName +
                            " — treating as straight-line segment" +
                            " (planarised solid assumed).").c_str());
                    }
                }
            }
            catch ( const java::IllegalArgumentException& ) {
                // inner param is not a reference; keep geomName as-is
            }
        }
    }
    else if ( geomName == "B_SPLINE_CURVE_WITH_KNOTS" ) {
        java::System::err.println((
            java::String("[StepReader] EDGE_CURVE #") +
            java::String::valueOf(ecId) +
            ": geometry is B_SPLINE_CURVE_WITH_KNOTS" +
            " — treating as straight-line segment" +
            " (planarised solid assumed).").c_str());
    }
}

//=================================================================
//= PASS 1: VERTICES ==============================================

void _StepSolidBuilder::pass1CreateVertices(PolyhedralBoundedSolid* solid)
{
    int pbsId = 1;
    java::ArrayList<int> vpIds = collectAllVertexPointIds();
    long int i;
    for ( i = 0; i < vpIds.size(); i++ ) {
        int vpId = vpIds.get(i);
        Vector3Dd pos = resolveVertexPosition(vpId);
        // C++ port: the vertex does not list itself in its solid
        _PolyhedralBoundedSolidVertex* v =
            new _PolyhedralBoundedSolidVertex(pos, pbsId);
        solid->getVerticesList().add(v);
        vertexByVpId.put(vpId, v);
        pbsId++;
    }
}

java::ArrayList<int> _StepSolidBuilder::collectAllVertexPointIds()
{
    java::ArrayList<int> vpIds;
    // Java iterates `ecStartVpId.entrySet()` and then `ecEndVpId.entrySet()`:
    // both maps have the same keys, put in the same order
    java::ArrayList<int> ecIds =
        java::HashMapIterationOrder::integerKeys(ecIdsInInsertionOrder);
    long int i;
    for ( i = 0; i < ecIds.size(); i++ ) {
        int value = *ecStartVpId.get(ecIds.get(i));
        if ( !vpIds.contains(value) ) {
            vpIds.add(value);
        }
    }
    for ( i = 0; i < ecIds.size(); i++ ) {
        int value = *ecEndVpId.get(ecIds.get(i));
        if ( !vpIds.contains(value) ) {
            vpIds.add(value);
        }
    }
    return vpIds;
}

Vector3Dd _StepSolidBuilder::resolveVertexPosition(int vpId)
{
    _StepEntity* vp = require(vpId, "VERTEX_POINT");
    int cpId = _StepTokenizer::parseRef(vp->params.get(1));
    _StepEntity* cp = require(cpId, "CARTESIAN_POINT");
    java::ArrayList<java::String> coords =
        _StepTokenizer::parseAggregate(cp->params.get(1));
    double x = _StepTokenizer::parseDouble(coords.get(0)) * unitToMetre;
    double y = _StepTokenizer::parseDouble(coords.get(1)) * unitToMetre;
    double z = _StepTokenizer::parseDouble(coords.get(2)) * unitToMetre;
    return Vector3Dd(x, y, z);
}

/**
Scans the entity map for a compound entity that declares both
LENGTH_UNIT and SI_UNIT, extracts the SI prefix, and returns the
corresponding scale factor to convert to metres.

Recognised SI prefixes and their scale factors:
  .MILLI.  → 0.001    .CENTI.  → 0.01
  .DECI.   → 0.1      (none/$) → 1.0
  .DECA.   → 10       .HECTO.  → 100
  .KILO.   → 1000

Returns 1.0 (metres assumed) when no matching entity is found or the
prefix is unrecognised.
*/
double _StepSolidBuilder::detectLengthUnitScale()
{
    java::ArrayList<int> ids = entities.keysInJavaOrder();
    long int i;
    for ( i = 0; i < ids.size(); i++ ) {
        _StepEntity* e = entities.get(ids.get(i));
        if ( !(e->name == _StepEntity::COMPLEX_NAME) ) {
            continue;
        }
        java::String body = e->params.get(0);
        if ( !body.contains("LENGTH_UNIT") || !body.contains("SI_UNIT") ) {
            continue;
        }
        // Extract the SI_UNIT prefix from the compound body.
        // The SI_UNIT sub-entity has the form: SI_UNIT(prefix,name)
        // where prefix may be $ (none) or an enum like .MILLI.
        int siPos = body.indexOf(java::String("SI_UNIT("));
        if ( siPos < 0 ) {
            continue;
        }
        int open = siPos + java::String("SI_UNIT(").length();
        int close = body.indexOf(')', open);
        if ( close < 0 ) {
            continue;
        }
        java::String siParams = body.substring(open, close).trim();
        // siParams = "prefix,name" e.g. ".MILLI.,.METRE." or "$,.METRE."
        int comma = siParams.indexOf(',');
        java::String prefix = comma >= 0
            ? siParams.substring(0, comma).trim()
            : siParams.trim();
        double scale = siPrefixToScale(prefix);
        if ( scale != 1.0 ) {
            java::System::err.println((
                java::String("[StepReader] Length unit prefix ") + prefix +
                " detected — scaling all vertex coordinates by " +
                java::Double::toString(scale) + " to convert to metres.").c_str());
        }
        return scale;
    }
    return 1.0;
}

double _StepSolidBuilder::siPrefixToScale(const java::String& prefix)
{
    if ( prefix == ".ATTO." ) return 1e-18;
    if ( prefix == ".FEMTO." ) return 1e-15;
    if ( prefix == ".PICO." ) return 1e-12;
    if ( prefix == ".NANO." ) return 1e-9;
    if ( prefix == ".MICRO." ) return 1e-6;
    if ( prefix == ".MILLI." ) return 1e-3;
    if ( prefix == ".CENTI." ) return 1e-2;
    if ( prefix == ".DECI." ) return 1e-1;
    if ( prefix == ".DECA." ) return 1e1;
    if ( prefix == ".HECTO." ) return 1e2;
    if ( prefix == ".KILO." ) return 1e3;
    if ( prefix == ".MEGA." ) return 1e6;
    if ( prefix == ".GIGA." ) return 1e9;
    if ( prefix == ".TERA." ) return 1e12;
    return 1.0;   // $ or unrecognised = metres
}

//=================================================================
//= PASS 2: FACES, LOOPS, HALF-EDGES ==============================

void _StepSolidBuilder::pass2CreateFacesAndHalfEdges(
    PolyhedralBoundedSolid* solid,
    const java::ArrayList<int>& faceIds)
{
    int faceSeqId = 1;
    long int i;
    for ( i = 0; i < faceIds.size(); i++ ) {
        int faceId = faceIds.get(i);
        _StepEntity* faceEntity = require(faceId, "ADVANCED_FACE");
        // C++ port: the face does not list itself in its solid
        _PolyhedralBoundedSolidFace* face =
            new _PolyhedralBoundedSolidFace(solid, faceSeqId);
        solid->getPolygonsList().add(face);
        faceSeqId++;
        bool faceSameSense = parseFaceSameSense(faceEntity);
        buildLoopsForFace(face, faceEntity, faceSameSense);
    }
}

/**
Reads ADVANCED_FACE param[3] (same_sense flag).
Returns true when the face normal agrees with the underlying surface
normal (.T.), false when it is reversed (.F.).  Any parse failure
defaults to true (no flip).
*/
bool _StepSolidBuilder::parseFaceSameSense(_StepEntity* faceEntity)
{
    if ( faceEntity->params.size() < 4 ) {
        return true;
    }
    return !(faceEntity->params.get(3).trim() == ".F.");
}

void _StepSolidBuilder::buildLoopsForFace(_PolyhedralBoundedSolidFace* face,
                                          _StepEntity* faceEntity,
                                          bool faceSameSense)
{
    java::ArrayList<java::String> boundsAgg =
        _StepTokenizer::parseAggregate(faceEntity->params.get(1));
    long int i;
    for ( i = 0; i < boundsAgg.size(); i++ ) {
        int boundId = _StepTokenizer::parseRef(boundsAgg.get(i));
        _StepEntity* bound = entities.get(boundId);
        if ( bound == nullptr ) {
            continue;
        }
        int loopId = _StepTokenizer::parseRef(bound->params.get(1));
        _StepEntity* loop = require(loopId, "EDGE_LOOP");
        buildLoop(face, loop, faceSameSense);
    }
}

void _StepSolidBuilder::buildLoop(_PolyhedralBoundedSolidFace* face,
                                  _StepEntity* loopEntity,
                                  bool faceSameSense)
{
    _PolyhedralBoundedSolidLoop* loop =
        new _PolyhedralBoundedSolidLoop(face);

    java::ArrayList<java::String> oeAgg =
        _StepTokenizer::parseAggregate(loopEntity->params.get(1));

    java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> halfEdges;

    long int i;
    for ( i = 0; i < oeAgg.size(); i++ ) {
        int oeId = _StepTokenizer::parseRef(oeAgg.get(i));
        _StepEntity* oe = require(oeId, "ORIENTED_EDGE");
        int ecId = _StepTokenizer::parseRef(oe->params.get(3));
        // When same_sense = .F. the face normal is flipped, which means
        // the loop traversal direction is also reversed.  XOR-flipping
        // isForward restores the canonical half-edge direction so that
        // pass3 always sees exactly one .T. (rightHalf) and one .F.
        // (leftHalf) per EDGE_CURVE.
        bool isForward = oe->params.get(4).trim() == ".T.";
        if ( !faceSameSense ) {
            isForward = !isForward;
        }

        int vpId = isForward
            ? *ecStartVpId.get(ecId)
            : *ecEndVpId.get(ecId);
        _PolyhedralBoundedSolidVertex** vSlot = vertexByVpId.get(vpId);
        _PolyhedralBoundedSolidVertex* v = vSlot != nullptr ? *vSlot : nullptr;
        if ( v == nullptr ) {
            throw java::IllegalStateException(
                java::String("Vertex not found for VERTEX_POINT id ") +
                java::String::valueOf(vpId));
        }

        _PolyhedralBoundedSolidHalfEdge* he =
            new _PolyhedralBoundedSolidHalfEdge(v, loop);
        loop->halfEdgesList.add(he);
        heEdgeCurveId.put(he, ecId);
        heIsForward.put(he, isForward);
        halfEdgesInInsertionOrder.add(he);
        halfEdges.add(he);
    }

    if ( halfEdges.isEmpty() ) {
        return;
    }

    // When same_sense = .F., the STEP loop is wound in the opposite
    // direction (CW from outside).  The isForward flip above corrects
    // the rightHalf/leftHalf assignment for pass3, but the list order
    // still reflects the CW traversal.  Reversing the list here restores
    // CCW winding for rendering without affecting the per-edge direction
    // flags already recorded in heIsForward.
    if ( !faceSameSense ) {
        loop->halfEdgesList.reverse();
    }

    loop->boundaryStartHalfEdge = loop->halfEdgesList.get(0);
}

//=================================================================
//= PASS 3: EDGE WIRING ===========================================

void _StepSolidBuilder::pass3WireEdges(PolyhedralBoundedSolid* solid)
{
    java::HashMap<int, _PolyhedralBoundedSolidHalfEdge*> forwardHe;
    java::HashMap<int, _PolyhedralBoundedSolidHalfEdge*> reverseHe;

    long int i;
    for ( i = 0; i < halfEdgesInInsertionOrder.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* he = halfEdgesInInsertionOrder.get(i);
        int ecId = *heEdgeCurveId.get(he);
        bool forward = *heIsForward.get(he);
        if ( forward ) {
            forwardHe.put(ecId, he);
        }
        else {
            reverseHe.put(ecId, he);
        }
    }

    java::ArrayList<int> ecIds =
        java::HashMapIterationOrder::integerKeys(ecIdsInInsertionOrder);
    for ( i = 0; i < ecIds.size(); i++ ) {
        int ecId = ecIds.get(i);
        _PolyhedralBoundedSolidHalfEdge* const* rightSlot = forwardHe.get(ecId);
        _PolyhedralBoundedSolidHalfEdge* const* leftSlot = reverseHe.get(ecId);
        _PolyhedralBoundedSolidHalfEdge* right =
            rightSlot != nullptr ? *rightSlot : nullptr;
        _PolyhedralBoundedSolidHalfEdge* left =
            leftSlot != nullptr ? *leftSlot : nullptr;
        if ( right == nullptr || left == nullptr ) {
            throw java::IllegalStateException(
                java::String("EDGE_CURVE #") + java::String::valueOf(ecId) +
                " is not referenced by exactly two ORIENTED_EDGEs " +
                "with opposite orientations.");
        }
        // C++ port: the edge does not list itself in its solid
        _PolyhedralBoundedSolidEdge* edge = new _PolyhedralBoundedSolidEdge();
        solid->getEdgesList().add(edge);
        edge->rightHalf = right;
        edge->leftHalf = left;
        right->parentEdge = edge;
        left->parentEdge = edge;
    }
}

//=================================================================
//= PASS 4: EMANATING HALF-EDGE ===================================

void _StepSolidBuilder::pass4SetEmanatingHalfEdges()
{
    long int i;
    for ( i = 0; i < halfEdgesInInsertionOrder.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* he = halfEdgesInInsertionOrder.get(i);
        _PolyhedralBoundedSolidVertex* v = he->startingVertex;
        if ( v->emanatingHalfEdge == nullptr ) {
            v->emanatingHalfEdge = he;
        }
    }
}

//=================================================================
//= UTILITIES =====================================================

void _StepSolidBuilder::updateMaxIds(PolyhedralBoundedSolid* solid)
{
    int maxV = 0;
    long int i;
    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        int vid = solid->getVerticesList().get(i)->id;
        if ( vid > maxV ) {
            maxV = vid;
        }
    }
    int maxF = 0;
    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        int fid = solid->getPolygonsList().get(i)->id;
        if ( fid > maxF ) {
            maxF = fid;
        }
    }
    solid->setMaxVertexId(maxV);
    solid->setMaxFaceId(maxF);
}

_StepEntity* _StepSolidBuilder::require(int id, const java::String& expectedName)
{
    _StepEntity* e = entities.get(id);
    if ( e == nullptr ) {
        throw java::IllegalStateException(
            java::String("Expected entity #") + java::String::valueOf(id) +
            " (" + expectedName + ") but it was not found.");
    }
    if ( !(expectedName == e->name) ) {
        throw java::IllegalStateException(
            java::String("Entity #") + java::String::valueOf(id) +
            " expected to be " + expectedName +
            " but is " + e->name + ".");
    }
    return e;
}
