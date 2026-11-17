//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include <stddef.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/String.h"
#include "java/util/ArrayDeque.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/common/dataStructures/CircularArrayList.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidMeshConstructions.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

/**
C++ port note: the solid is owned by the builder until `build` gives it,
so it is deleted when a malformed mesh throws an exception. The keys of the
directed edges are `long` (32 bits on Turbo C++ 3.0, enough for meshes of
some tens of thousands of vertices), and the sets of the Java code are maps
to a dummy value.
*/
class PolyhedralBoundedSolidMeshConstructions::_MeshBuilder {
private:
    const ArrayList<Vector3Dd>& positions;
    const ArrayList<ArrayList<int> >& faces;
    long numberOfVertices;
    PolyhedralBoundedSolid* solid;

    /// Mesh face of each directed edge
    HashMap<long, int> faceOfDirectedEdge;
    /// Half-edge of the solid along each directed edge already created
    HashMap<long, _PolyhedralBoundedSolidHalfEdge*> halfEdges;
    /// Half-edges of the solid starting at each vertex
    ArrayList<ArrayList<_PolyhedralBoundedSolidHalfEdge*> > outgoing;
    ArrayList<BOOLEAN> vertexCreated;
    HashMap<_PolyhedralBoundedSolidFace*, int> builtFaces;
    long registeredEdges;

    long key(int from, int to) const;
    static int meshIndex(_PolyhedralBoundedSolidVertex* vertex);
    void indexMesh();
    void registerNewEdges();
    void registerHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    BOOLEAN isOpen(_PolyhedralBoundedSolidFace* face) const;
    static _PolyhedralBoundedSolidFace* faceOf(
        _PolyhedralBoundedSolidHalfEdge* he);
    void enqueueNeighbors(int f, ArrayList<BOOLEAN>& queued,
        ArrayDeque<int>& queue);
    void buildFirstFace(const ArrayList<int>& polygon);
    BOOLEAN edgeExists(int from, int to) const;
    void buildFace(int f);
    void buildRun(int f, int beforeA, int a, const ArrayList<int>& newVertices,
        int b, int afterB);
    void connect(_PolyhedralBoundedSolidHalfEdge* corner,
        _PolyhedralBoundedSolidHalfEdge* target);
    _PolyhedralBoundedSolidHalfEdge* findCorner(int x, int f);
    int nextInFace(int f, int x) const;
    void finishFace(const ArrayList<int>& polygon);

public:
    _MeshBuilder(const ArrayList<Vector3Dd>& positions,
        const ArrayList<ArrayList<int> >& faces);
    ~_MeshBuilder();
    PolyhedralBoundedSolid* build();
};

PolyhedralBoundedSolid* PolyhedralBoundedSolidMeshConstructions::createFromPolygonMesh(
    const ArrayList<Vector3Dd>& positions,
    const ArrayList<ArrayList<int> >& faces)
{
    if ( faces.isEmpty() ) {
        return NULL;
    }
    PolyhedralBoundedSolid* solid;
    {
        _MeshBuilder builder(positions, faces);
        solid = builder.build();
    }
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::_MeshBuilder(
    const ArrayList<Vector3Dd>& positions,
    const ArrayList<ArrayList<int> >& faces)
    : positions(positions), faces(faces),
      numberOfVertices(positions.size()), solid(NULL),
      faceOfDirectedEdge(), halfEdges(), outgoing(), vertexCreated(),
      builtFaces(), registeredEdges(0)
{
    long i;

    solid = new PolyhedralBoundedSolid();
    for ( i = 0; i < numberOfVertices; i++ ) {
        outgoing.add(ArrayList<_PolyhedralBoundedSolidHalfEdge*>());
        vertexCreated.add(FALSE);
    }
}

PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::~_MeshBuilder()
{
    if ( solid != NULL ) {
        delete solid;
    }
}

long PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::key(
    int from, int to) const
{
    return ((long)from) * numberOfVertices + to;
}

int PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::meshIndex(
    _PolyhedralBoundedSolidVertex* vertex)
{
    return vertex->id - 1;
}

//= Mesh checks =============================================================

/**
C++ port note: the check of the reverse of each directed edge goes over the
faces again, as the `HashMap` of the port has no key set; only the edge that
is reported when there are several on the border may change.
*/
void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::indexMesh()
{
    long f;
    long i;

    for ( f = 0; f < faces.size(); f++ ) {
        const ArrayList<int>& polygon = faces[f];
        if ( polygon.size() < 3 ) {
            throw IllegalArgumentException(
                String("Face ") + String::valueOf(f) +
                " has less than three vertices");
        }
        for ( i = 0; i < polygon.size(); i++ ) {
            int from = polygon[i];
            int to = polygon[(i + 1) % polygon.size()];
            if ( from < 0 || from >= numberOfVertices || from == to ) {
                throw IllegalArgumentException(
                    String("Face ") + String::valueOf(f) +
                    " has an invalid edge");
            }
            if ( faceOfDirectedEdge.containsKey(key(from, to)) ) {
                throw IllegalArgumentException(
                    String("Edge ") + String::valueOf(from) + "-" +
                    String::valueOf(to) +
                    " is used twice in the same direction");
            }
            faceOfDirectedEdge.put(key(from, to), (int)f);
        }
    }
    for ( f = 0; f < faces.size(); f++ ) {
        const ArrayList<int>& polygon = faces[f];
        for ( i = 0; i < polygon.size(); i++ ) {
            int from = polygon[i];
            int to = polygon[(i + 1) % polygon.size()];
            if ( !faceOfDirectedEdge.containsKey(key(to, from)) ) {
                throw IllegalArgumentException(
                    String("Edge ") + String::valueOf(from) + "-" +
                    String::valueOf(to) + " is on the border of the mesh");
            }
        }
    }
}

//= Bookkeeping =============================================================

/**
Indexes the half-edges of the edges created since the last call.
*/
void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::registerNewEdges()
{
    CircularArrayList<_PolyhedralBoundedSolidEdge*>& edges = solid->getEdgesList();
    for ( ; registeredEdges < edges.size(); registeredEdges++ ) {
        _PolyhedralBoundedSolidEdge* edge = edges.get(registeredEdges);
        registerHalfEdge(edge->rightHalf);
        registerHalfEdge(edge->leftHalf);
    }
}

void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::registerHalfEdge(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    int from = meshIndex(he->startingVertex);
    int to = meshIndex(he->mirrorHalfEdge()->startingVertex);
    halfEdges.put(key(from, to), he);
    outgoing[from].add(he);
}

BOOLEAN PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::isOpen(
    _PolyhedralBoundedSolidFace* face) const
{
    return !builtFaces.containsKey(face);
}

_PolyhedralBoundedSolidFace*
PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::faceOf(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    return he->parentLoop->parentFace;
}

//= Construction ============================================================

PolyhedralBoundedSolid* PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::build()
{
    indexMesh();

    ArrayList<BOOLEAN> queued;
    ArrayDeque<int> queue;
    long first;

    for ( first = 0; first < faces.size(); first++ ) {
        queued.add(FALSE);
    }

    // Each connected component of the mesh is a shell
    for ( first = 0; first < faces.size(); first++ ) {
        if ( queued[first] ) {
            continue;
        }
        buildFirstFace(faces[first]);
        queued[first] = TRUE;
        enqueueNeighbors((int)first, queued, queue);

        while ( !queue.isEmpty() ) {
            int f = queue.pollFirst();
            buildFace(f);
            enqueueNeighbors(f, queued, queue);
        }
    }

    if ( solid->getPolygonsList().size() != faces.size() ) {
        throw IllegalStateException(
            String("Mesh construction left ") +
            String::valueOf((long)(solid->getPolygonsList().size() - faces.size())) +
            " faces that are not part of the mesh");
    }
    PolyhedralBoundedSolid* result = solid;
    solid = NULL;
    return result;
}

void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::enqueueNeighbors(
    int f, ArrayList<BOOLEAN>& queued, ArrayDeque<int>& queue)
{
    const ArrayList<int>& polygon = faces[f];
    long i;
    for ( i = 0; i < polygon.size(); i++ ) {
        int from = polygon[i];
        int to = polygon[(i + 1) % polygon.size()];
        int neighbor = *faceOfDirectedEdge.get(key(to, from));
        if ( !queued[neighbor] ) {
            queued[neighbor] = TRUE;
            queue.addLast(neighbor);
        }
    }
}

/**
[MANT1988].12.2: a lamina with the vertices of the polygon; its side
that follows the order of the polygon is the mesh face, the other one
is the first open face.
*/
void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::buildFirstFace(
    const ArrayList<int>& polygon)
{
    long n = polygon.size();
    int faceId = solid->getMaxFaceId() + 1;
    long i;

    PolyhedralBoundedSolidEulerOperators::mvfs(solid,
        positions[polygon[0]], polygon[0] + 1, faceId);
    vertexCreated[polygon[0]] = TRUE;
    for ( i = 1; i < n; i++ ) {
        PolyhedralBoundedSolidEulerOperators::smev(solid, faceId,
            polygon[i - 1] + 1, polygon[i] + 1, positions[polygon[i]]);
        vertexCreated[polygon[i]] = TRUE;
    }
    PolyhedralBoundedSolidEulerOperators::smef(solid, faceId,
        polygon[n - 1] + 1, polygon[0] + 1, faceId + 1);
    registerNewEdges();
    finishFace(polygon);
}

BOOLEAN PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::edgeExists(
    int from, int to) const
{
    return halfEdges.containsKey(key(from, to));
}

void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::buildFace(int f)
{
    const ArrayList<int>& polygon = faces[f];
    long n = polygon.size();
    long start = -1;
    long i;

    // A run of new edges that starts right after an existing one
    for ( i = 0; i < n; i++ ) {
        BOOLEAN previousExists = edgeExists(polygon[(i + n - 1) % n], polygon[i]);
        BOOLEAN currentExists = edgeExists(polygon[i], polygon[(i + 1) % n]);
        if ( previousExists && !currentExists ) {
            start = i;
            break;
        }
    }

    if ( start >= 0 ) {
        long processed = 0;
        i = start;
        while ( processed < n ) {
            int from = polygon[i];
            int to = polygon[(i + 1) % n];
            if ( edgeExists(from, to) ) {
                i = (i + 1) % n;
                processed++;
                continue;
            }
            // New run: from polygon[i] through new vertices up to
            // the first vertex that already exists
            ArrayList<int> newVertices;
            long j = (i + 1) % n;
            long steps = 1;
            while ( !vertexCreated[polygon[j]] ) {
                newVertices.add(polygon[j]);
                j = (j + 1) % n;
                steps++;
            }
            buildRun(f, polygon[(i + n - 1) % n], polygon[i], newVertices,
                polygon[j], polygon[(j + 1) % n]);
            i = j;
            processed += steps;
        }
    }
    finishFace(polygon);
}

/**
Adds a run of new edges of mesh face `f`, from vertex `a` (whose
previous edge in the face, from `beforeA`, already exists) through the
new vertices to vertex `b` (followed in the face by `afterB`).
*/
void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::buildRun(
    int f, int beforeA, int a, const ArrayList<int>& newVertices,
    int b, int afterB)
{
    _PolyhedralBoundedSolidHalfEdge* corner =
        (*halfEdges.get(key(beforeA, a)))->next();
    _PolyhedralBoundedSolidHalfEdge* target;
    int last = a;
    long i;

    if ( edgeExists(b, afterB) ) {
        target = *halfEdges.get(key(b, afterB));
    }
    else {
        target = findCorner(b, f);
    }

    for ( i = 0; i < newVertices.size(); i++ ) {
        int w = newVertices[i];
        PolyhedralBoundedSolidEulerOperators::lmev(solid, corner, corner,
            w + 1, positions[w]);
        vertexCreated[w] = TRUE;
        registerNewEdges();
        // The strut is last -> w -> last, before the corner: the
        // corner at w is its returning half-edge
        corner = *halfEdges.get(key(w, last));
        last = w;
    }
    connect(corner, target);
    registerNewEdges();
}

/**
Adds the edge from the vertex of `corner` to the vertex of `target`,
before both half-edges.
*/
void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::connect(
    _PolyhedralBoundedSolidHalfEdge* corner,
    _PolyhedralBoundedSolidHalfEdge* target)
{
    if ( corner->parentLoop == target->parentLoop ) {
        // The new face is the one from target to corner
        PolyhedralBoundedSolidEulerOperators::lmef(solid, target, corner,
            solid->getMaxFaceId() + 1);
        return;
    }

    _PolyhedralBoundedSolidFace* cornerFace = faceOf(corner);
    _PolyhedralBoundedSolidFace* targetFace = faceOf(target);

    if ( cornerFace != targetFace ) {
        if ( !isOpen(cornerFace) || !isOpen(targetFace) ) {
            throw IllegalStateException(
                "Mesh construction tried to merge a finished face");
        }
        // Both loops in one face: a ring of the other one
        if ( targetFace->boundariesList.size() == 1 ) {
            PolyhedralBoundedSolidEulerOperators::lkfmrh(solid, cornerFace, targetFace);
        }
        else if ( cornerFace->boundariesList.size() == 1 ) {
            PolyhedralBoundedSolidEulerOperators::lkfmrh(solid, targetFace, cornerFace);
        }
        else {
            while ( targetFace->boundariesList.size() > 1 ) {
                PolyhedralBoundedSolidEulerOperators::lringmv(solid,
                    targetFace->boundariesList.get(1), cornerFace, FALSE);
            }
            PolyhedralBoundedSolidEulerOperators::lkfmrh(solid, cornerFace, targetFace);
        }
    }
    PolyhedralBoundedSolidEulerOperators::lmekr(solid, corner, target);
}

/**
Finds, among the open corners of vertex `x` (the open half-edges that
start at it), the one where mesh face `f` goes: the one whose gap
between its previous edge and its own edge contains `f` in the star of
`x`.
*/
_PolyhedralBoundedSolidHalfEdge*
PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::findCorner(int x, int f)
{
    ArrayList<_PolyhedralBoundedSolidHalfEdge*> candidates;
    ArrayList<_PolyhedralBoundedSolidHalfEdge*>& fromX = outgoing[x];
    long i;
    long steps;

    for ( i = 0; i < fromX.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* he = fromX[i];
        if ( isOpen(faceOf(he)) ) {
            candidates.add(he);
        }
    }
    if ( candidates.size() == 1 ) {
        return candidates[0];
    }
    for ( i = 0; i < candidates.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* he = candidates[i];
        int incoming = meshIndex(he->previous()->startingVertex);
        int outgoingTo = meshIndex(he->mirrorHalfEdge()->startingVertex);
        int g = *faceOfDirectedEdge.get(key(incoming, x));
        int end = *faceOfDirectedEdge.get(key(x, outgoingTo));
        for ( steps = 0; steps <= faces.size(); steps++ ) {
            if ( g == f ) {
                return he;
            }
            if ( g == end ) {
                break;
            }
            g = *faceOfDirectedEdge.get(key(nextInFace(g, x), x));
        }
    }
    throw IllegalStateException(
        String("Mesh construction found no corner at vertex ") +
        String::valueOf(x) + " for face " + String::valueOf(f));
}

int PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::nextInFace(
    int f, int x) const
{
    const ArrayList<int>& polygon = faces[f];
    long i;
    for ( i = 0; i < polygon.size(); i++ ) {
        if ( polygon[i] == x ) {
            return polygon[(i + 1) % polygon.size()];
        }
    }
    throw IllegalStateException(String("Vertex ") + String::valueOf(x) +
        " is not in face " + String::valueOf(f));
}

/**
Once all the edges of the polygon exist, its half-edges form a loop:
its face becomes a mesh face.
*/
void PolyhedralBoundedSolidMeshConstructions::_MeshBuilder::finishFace(
    const ArrayList<int>& polygon)
{
    _PolyhedralBoundedSolidHalfEdge* he =
        *halfEdges.get(key(polygon[0], polygon[1]));
    _PolyhedralBoundedSolidLoop* loop = he->parentLoop;
    _PolyhedralBoundedSolidFace* face = loop->parentFace;

    if ( loop->halfEdgesList.size() != polygon.size() || !isOpen(face) ) {
        throw IllegalStateException(
            String("Mesh construction could not close a face of ") +
            String::valueOf(polygon.size()) + " vertices");
    }
    if ( face->boundariesList.size() > 1 ) {
        face = PolyhedralBoundedSolidEulerOperators::lmfkrh(solid, loop,
            solid->getMaxFaceId() + 1);
    }
    builtFaces.put(face, 1);
}
