//= References:                                                             =
//= [MANT1986] Mantyla Martti. "Boolean Operations of 2-Manifolds through   =
//=     Vertex Neighborhood Classification". ACM Transactions on Graphics,  =
//=     Vol. 5, No. 1, January 1986, pp. 1-29.                              =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

#ifndef ___POLYHEDRAL_BOUNDED_SOLID_SET_OPERATOR__
#define ___POLYHEDRAL_BOUNDED_SOLID_SET_OPERATOR__

#include "java/lang/IllegalStateException.h"
#include "java/util/ArrayList.h"

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidLoop;
class _SetOperationContext;
class _PolyhedralBoundedSolidIdNamespace;
class _PolyhedralBoundedSolidSetOperatorVertexFace;
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex;

/**
This class encapsulates the set operations algorithms for boundary
representation solids in VitralSDK. Basically, this class implements the
original algorithm published in the paper [MANT1986] and in the second
part of the book [MANT1988].
The algorithm is structured in 5 big phases:
  0. Calculate vertex/face and vertex/vertex crossings.
  1. Classify and split for vertex/face cases.
  2. Classify and split for vertex/vertex cases.
  3. Connect.
  4. Finish.
Note that each big phase is controlled in a method (mark as "big phase" in
its documentation).

C++ port notes:
- `setOp` returns a new solid owned by the caller. As in Java the operands
  are consumed (they are modified and parts of them are moved into the
  result), but before returning, every node used by the result is unlisted
  from the operands, so the caller can delete the operands and the result
  independently.
- On the exceptions of `completeSetOpResult` and of the connect stage the
  partial result is deleted (Java leaves it to the garbage collector).

C++ counterpart of Java's `_PolyhedralBoundedSolidSetOperator`.
*/
class _PolyhedralBoundedSolidSetOperator : public _PolyhedralBoundedSolidOperator {
public:
    /**
    Optional callback used to export internal state in graphical form.
    */
    typedef void (*DebugSolidExporter)(PolyhedralBoundedSolid* solid,
        const java::String& pattern);

    static void setDebugSolidExporter(DebugSolidExporter exporter);

    /**
    Procedure `updmaxnames` functionality is described on section
    [MANT1988].15.4. This method increments the face and vertex
    identifiers of `solidToUpdate` so that they do not overlap with
    `referenceSolid` identifiers.
    */
    static void updmaxnames(PolyhedralBoundedSolid* solidToUpdate,
        PolyhedralBoundedSolid* referenceSolid);

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    that points inward the he's containing face. This adapts the sector
    bisector idea from problem [MANT1988].14.1 to the set-operations
    classifiers of chapter [MANT1988].15.

    C++ port note: protected in Java, used by the classifiers of the same
    package; public here.
    */
    static Vector3Dd inside(_PolyhedralBoundedSolidHalfEdge* he);

    static bool colinearVectorsWithDirection(const Vector3Dd& a,
                                             const Vector3Dd& b);

    /**
    Outcome of the endpoint-pairing recovery loop in `separateEdgeSequence`.
    */
    enum SeparateEdgeSequenceResult {
        OK,
        FAILED_NULL_INPUT,
        FAILED_DIFFERENT_SOLIDS,
        FAILED_CYCLE_DETECTED,
        FAILED_NO_PAIRING_REACHED
    };

    /**
    Following program [MANT1988].15.12. Adapts the wMANT2008 recovery
    extensions for null-edge endpoints (cases A-E) using strict cycle
    detection. Retained (as in Java) only to validate the failure modes;
    the live pipeline uses the classifier version.
    @return diagnostic result; OK only when `from` and `to` share starting
    vertex and the LMEV split has been applied
    */
    static SeparateEdgeSequenceResult separateEdgeSequence(
        _PolyhedralBoundedSolidHalfEdge* from,
        _PolyhedralBoundedSolidHalfEdge* to,
        int type,
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB);

    /**
    Result path recorded when the operation is rejected with
    NonManifoldBooleanResultException.
    */
    static const char* const REJECTED_NON_MANIFOLD_PATH;

    /**
    Name of the path that produced the last setOp result on the calling
    thread ("normal-pipeline" or the preflight that answered instead).
    @return null when none was recorded
    */
    static const java::String* getLastResultPath();

    /**
    Violations of the post-connect invariant found on the last setOp of the
    calling thread that reached connect.
    @return null when connect was not reached
    */
    static const java::ArrayList<java::String>* getLastConnectInvariantViolations();

    /**
    Detects result pieces that touch only along an edge or at a vertex.
    @return false (Java null) when there is none; otherwise true and the
    description of the first contact in `description`
    */
    static bool findNonManifoldContact(PolyhedralBoundedSolid* result,
        java::String* description);

    /**
    Following program [MANT1988].15.1.
    @param inSolidA first operand (consumed)
    @param inSolidB second operand (consumed)
    @param op UNION, INTERSECTION or SUBTRACT
    @param withDebug true to dump the pipeline state to files and stdout
    @param maximizeResultFaces true to maximize the faces of the result
    @param doStrictValidation true to enforce strict validation of the result
    @return the result solid, owned by the caller
    @throws InvalidOperandException when the operands are not valid solids
    @throws SolidOperationFailedException when the intersection curve can
    not be closed
    @throws NonManifoldBooleanResultException for a non-manifold result
    @throws InvalidSolidResultException when `doStrictValidation` is true
    and the result fails strict validation
    */
    static PolyhedralBoundedSolid* setOp(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op,
        bool withDebug = false,
        bool maximizeResultFaces = true,
        bool doStrictValidation = true);

private:
    typedef _PolyhedralBoundedSolidSetNonIntersectingClassifier::_PreflightCache PreflightCache;

    static const int DEBUG_01_STRUCTURE = 0x01;
    static const int DEBUG_02_GENERATOR = 0x02;
    static const int DEBUG_03_VERTEXFACECLASIFFIER = 0x04;
    static const int DEBUG_04_VERTEXVERTEXCLASIFFIER = 0x08;
    static const int DEBUG_05_CONNECT = 0x10;
    static const int DEBUG_06_FINISH = 0x20;
    static const int DEBUG_99_SHOWOPERATIONS = 0x40;
    static int debugFlags;
    static DebugSolidExporter debugSolidExporter;

    /** Namespace installed by the most recent setOp (owned here). */
    static _PolyhedralBoundedSolidIdNamespace* ownedNamespace;

    /** Java ThreadLocal<String>: the value and whether it is set. */
    static thread_local java::String lastResultPath;
    static thread_local bool hasLastResultPath;
    static thread_local java::ArrayList<java::String> lastConnectInvariantViolations;
    static thread_local bool hasLastConnectInvariantViolations;

    static void setLastResultPath(const char* path);
    static void weldCoincidentVerticesOfOperand(PolyhedralBoundedSolid* solid,
        const char* name);
    static PolyhedralBoundedSolid* setOpNoIntersectionCase(
        PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes, int op);
    static PolyhedralBoundedSolid* setOpNoIntersectionCase(
        PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes, int op, PreflightCache& cache);
    static java::ArrayList<java::String> checkConnectInvariant(
        PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
        _SetOperationContext& ctx);
    static void collectNullFaceViolations(const char* label,
        const java::ArrayList<_PolyhedralBoundedSolidFace*>& nullFaces,
        java::ArrayList<java::String>& violations);
    static bool isPointLikeFace(_PolyhedralBoundedSolidFace* f);
    static void collectCoincidentLoopViolations(const char* label,
        PolyhedralBoundedSolid* solid,
        const java::ArrayList<_PolyhedralBoundedSolidFace*>& nullFaces,
        java::ArrayList<java::String>& violations);
    static bool loopsCoincide(_PolyhedralBoundedSolidLoop* a,
        _PolyhedralBoundedSolidLoop* b);
    static bool sameEndpoints(_PolyhedralBoundedSolidEdge* e,
        _PolyhedralBoundedSolidEdge* f);

    static int nextVertexId(PolyhedralBoundedSolid* current,
        PolyhedralBoundedSolid* other);
    static void setOpGenerate(_SetOperationContext& ctx,
        PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB);
    static void weldIntersectionVertices(_SetOperationContext& ctx,
        PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB);
    static void pruneStaleVertexFaceEntries(
        java::ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>& list,
        PolyhedralBoundedSolid* solid);
    static bool sectoroverlap(
        const _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex& na,
        const _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex& nb);
    static _PolyhedralBoundedSolidHalfEdge* recoverEdgeSequenceEndpointFromStrut(
        _PolyhedralBoundedSolidHalfEdge* endpoint, bool isFromEndpoint);
    static bool nulledge(_PolyhedralBoundedSolidHalfEdge* he);
    static bool strutnulledge(_PolyhedralBoundedSolidHalfEdge* he);
    static void setOpClassify(_SetOperationContext& ctx, int op,
        PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB);
    static void traceSelfTouchingLoops(PolyhedralBoundedSolid* solid,
        const char* label);
    static void splitSelfTouchingLoops(PolyhedralBoundedSolid* solid);
    static void setOpConnect(_SetOperationContext& ctx, int op);
    static void setOpFinish(_SetOperationContext& ctx,
        PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes, int op);
    static void debugSolid(PolyhedralBoundedSolid* solid, const char* pattern);
    static void postProcessResult(PolyhedralBoundedSolid* res,
        bool maximizeResultFaces);
    static PolyhedralBoundedSolid* completeSetOpResult(
        PolyhedralBoundedSolid* result,
        const java::String& operandADiagnostics,
        const java::String& operandBDiagnostics,
        int op,
        const char* resultPath,
        bool maximizeResultFaces,
        bool doStrictValidation);
    static java::String operationName(int op);
    static java::String cardinalitiesAndBounds(PolyhedralBoundedSolid* solid);
    static PolyhedralBoundedSolid* deepCloneSolid(PolyhedralBoundedSolid* solid,
        const char* solidLabel);
    static bool hasIncompleteConnectState();

    /**
    C++ ownership helper: removes from the lists of `from` every face, edge
    and vertex that is also listed by `reference`.
    */
    static void unlistSharedNodes(PolyhedralBoundedSolid* from,
        PolyhedralBoundedSolid* reference);

    /**
    C++ ownership helper: deletes a structural copy of an operand, after
    unlisting the nodes the result took from it.
    */
    static void releaseCopy(PolyhedralBoundedSolid* copy,
        PolyhedralBoundedSolid* result);
};

#endif
