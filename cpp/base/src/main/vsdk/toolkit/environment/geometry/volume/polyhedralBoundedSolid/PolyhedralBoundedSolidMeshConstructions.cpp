//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/String.h"
#include "java/util/ArrayDeque.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "java/util/TreeMap.txx"
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

namespace {

class _MeshBuilder
{
private:
    const java::ArrayList<Vector3Dd>& positions;
    const java::ArrayList<java::ArrayList<int> >& faces;
    int numberOfVertices;
    /// Owned until `build` gives it away
    PolyhedralBoundedSolid* solid;

    /// Mesh face of each directed edge (kept in key order, as the keys are
    /// iterated)
    java::TreeMap<long, int> faceOfDirectedEdge;
    /// Half-edge of the solid along each directed edge already created
    java::HashMap<long, _PolyhedralBoundedSolidHalfEdge*> halfEdges;
    /// Half-edges of the solid starting at each vertex
    java::ArrayList<java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> > outgoing;
    java::ArrayList<bool> vertexCreated;
    java::HashMap<_PolyhedralBoundedSolidFace*, bool> builtFaces;
    int registeredEdges;

    long key(int from, int to) const
    {
        return ((long)from) * numberOfVertices + to;
    }

    static int meshIndex(_PolyhedralBoundedSolidVertex* vertex)
    {
        return vertex->id - 1;
    }

    //= Mesh checks =====================================================

    void indexMesh()
    {
        for ( int f = 0; f < faces.size(); f++ ) {
            const java::ArrayList<int>& polygon = faces[f];
            if ( polygon.size() < 3 ) {
                throw java::IllegalArgumentException(
                    "Face " + java::String::valueOf(f) +
                    " has less than three vertices");
            }
            for ( int i = 0; i < polygon.size(); i++ ) {
                int from = polygon[i];
                int to = polygon[(i + 1) % polygon.size()];
                if ( from < 0 || from >= numberOfVertices || from == to ) {
                    throw java::IllegalArgumentException(
                        "Face " + java::String::valueOf(f) +
                        " has an invalid edge");
                }
                if ( !faceOfDirectedEdge.put(key(from, to), f) ) {
                    throw java::IllegalArgumentException(
                        "Edge " + java::String::valueOf(from) + "-" +
                        java::String::valueOf(to) +
                        " is used twice in the same direction");
                }
            }
        }
        for ( long n = 0; n < faceOfDirectedEdge.size(); n++ ) {
            long directed = faceOfDirectedEdge.getKey(n);
            int from = (int)(directed / numberOfVertices);
            int to = (int)(directed % numberOfVertices);
            if ( !faceOfDirectedEdge.containsKey(key(to, from)) ) {
                throw java::IllegalArgumentException(
                    "Edge " + java::String::valueOf(from) + "-" +
                    java::String::valueOf(to) +
                    " is on the border of the mesh");
            }
        }
    }

    //= Bookkeeping =====================================================

    /**
    Indexes the half-edges of the edges created since the last call.
    */
    void registerNewEdges()
    {
        CircularArrayList<_PolyhedralBoundedSolidEdge*>& edges =
            solid->getEdgesList();
        for ( ; registeredEdges < edges.size(); registeredEdges++ ) {
            _PolyhedralBoundedSolidEdge* edge = edges.get(registeredEdges);
            registerHalfEdge(edge->rightHalf);
            registerHalfEdge(edge->leftHalf);
        }
    }

    void registerHalfEdge(_PolyhedralBoundedSolidHalfEdge* he)
    {
        int from = meshIndex(he->startingVertex);
        int to = meshIndex(he->mirrorHalfEdge()->startingVertex);
        halfEdges.put(key(from, to), he);
        outgoing[from].add(he);
    }

    bool isOpen(_PolyhedralBoundedSolidFace* face) const
    {
        return !builtFaces.containsKey(face);
    }

    static _PolyhedralBoundedSolidFace* faceOf(
        _PolyhedralBoundedSolidHalfEdge* he)
    {
        return he->parentLoop->parentFace;
    }

    int faceOfEdge(long directed) const
    {
        return *faceOfDirectedEdge.get(directed);
    }

    _PolyhedralBoundedSolidHalfEdge* halfEdgeOf(long directed)
    {
        return *halfEdges.get(directed);
    }

public:
    _MeshBuilder(const java::ArrayList<Vector3Dd>& inPositions,
        const java::ArrayList<java::ArrayList<int> >& inFaces) :
        positions(inPositions), faces(inFaces)
    {
        int i;
        numberOfVertices = (int)positions.size();
        solid = new PolyhedralBoundedSolid();
        registeredEdges = 0;
        for ( i = 0; i < numberOfVertices; i++ ) {
            outgoing.add(java::ArrayList<_PolyhedralBoundedSolidHalfEdge*>());
            vertexCreated.add(false);
        }
    }

    ~_MeshBuilder()
    {
        delete solid;
    }

    //= Construction ====================================================

    PolyhedralBoundedSolid* build()
    {
        int first;

        indexMesh();

        java::ArrayList<bool> queued;
        for ( first = 0; first < faces.size(); first++ ) {
            queued.add(false);
        }
        java::ArrayDeque<int> queue;

        // Each connected component of the mesh is a shell
        for ( first = 0; first < faces.size(); first++ ) {
            if ( queued[first] ) {
                continue;
            }
            buildFirstFace(faces[first]);
            queued[first] = true;
            enqueueNeighbors(first, queued, queue);

            while ( !queue.isEmpty() ) {
                int f = queue.pollFirst();
                buildFace(f);
                enqueueNeighbors(f, queued, queue);
            }
        }

        if ( solid->getPolygonsList().size() != faces.size() ) {
            throw java::IllegalStateException(
                "Mesh construction left " +
                java::String::valueOf(
                    (long)(solid->getPolygonsList().size() - faces.size())) +
                " faces that are not part of the mesh");
        }
        PolyhedralBoundedSolid* result = solid;
        solid = nullptr;
        return result;
    }

private:
    void enqueueNeighbors(int f, java::ArrayList<bool>& queued,
        java::ArrayDeque<int>& queue)
    {
        const java::ArrayList<int>& polygon = faces[f];
        for ( int i = 0; i < polygon.size(); i++ ) {
            int from = polygon[i];
            int to = polygon[(i + 1) % polygon.size()];
            int neighbor = faceOfEdge(key(to, from));
            if ( !queued[neighbor] ) {
                queued[neighbor] = true;
                queue.addLast(neighbor);
            }
        }
    }

    /**
    [MANT1988].12.2: a lamina with the vertices of the polygon; its side
    that follows the order of the polygon is the mesh face, the other one
    is the first open face.
    */
    void buildFirstFace(const java::ArrayList<int>& polygon)
    {
        int n = (int)polygon.size();
        int faceId = solid->getMaxFaceId() + 1;

        PolyhedralBoundedSolidEulerOperators::mvfs(solid,
            positions[polygon[0]], polygon[0] + 1, faceId);
        vertexCreated[polygon[0]] = true;
        for ( int i = 1; i < n; i++ ) {
            PolyhedralBoundedSolidEulerOperators::smev(solid, faceId,
                polygon[i - 1] + 1, polygon[i] + 1, positions[polygon[i]]);
            vertexCreated[polygon[i]] = true;
        }
        PolyhedralBoundedSolidEulerOperators::smef(solid, faceId,
            polygon[n - 1] + 1, polygon[0] + 1, faceId + 1);
        registerNewEdges();
        finishFace(polygon);
    }

    bool edgeExists(int from, int to) const
    {
        return halfEdges.containsKey(key(from, to));
    }

    void buildFace(int f)
    {
        const java::ArrayList<int>& polygon = faces[f];
        int n = (int)polygon.size();
        int start = -1;

        // A run of new edges that starts right after an existing one
        for ( int i = 0; i < n; i++ ) {
            bool previousExists = edgeExists(polygon[(i + n - 1) % n], polygon[i]);
            bool currentExists = edgeExists(polygon[i], polygon[(i + 1) % n]);
            if ( previousExists && !currentExists ) {
                start = i;
                break;
            }
        }

        if ( start >= 0 ) {
            int processed = 0;
            int i = start;
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
                java::ArrayList<int> newVertices;
                int j = (i + 1) % n;
                int steps = 1;
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
    void buildRun(int f, int beforeA, int a,
        const java::ArrayList<int>& newVertices, int b, int afterB)
    {
        _PolyhedralBoundedSolidHalfEdge* corner =
            halfEdgeOf(key(beforeA, a))->next();
        _PolyhedralBoundedSolidHalfEdge* target;
        int last = a;

        if ( edgeExists(b, afterB) ) {
            target = halfEdgeOf(key(b, afterB));
        }
        else {
            target = findCorner(b, f);
        }

        for ( long k = 0; k < newVertices.size(); k++ ) {
            int w = newVertices[k];
            PolyhedralBoundedSolidEulerOperators::lmev(solid, corner, corner,
                w + 1, positions[w]);
            vertexCreated[w] = true;
            registerNewEdges();
            // The strut is last -> w -> last, before the corner: the
            // corner at w is its returning half-edge
            corner = halfEdgeOf(key(w, last));
            last = w;
        }
        connect(corner, target);
        registerNewEdges();
    }

    /**
    Adds the edge from the vertex of `corner` to the vertex of `target`,
    before both half-edges.
    */
    void connect(_PolyhedralBoundedSolidHalfEdge* corner,
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
                throw java::IllegalStateException(
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
                        targetFace->boundariesList.get(1), cornerFace, false);
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
    _PolyhedralBoundedSolidHalfEdge* findCorner(int x, int f)
    {
        java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> candidates;
        long i;

        for ( i = 0; i < outgoing[x].size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge* he = outgoing[x][i];
            if ( isOpen(faceOf(he)) ) {
                candidates.add(he);
            }
        }
        if ( candidates.size() == 1 ) {
            return candidates.get(0);
        }
        for ( i = 0; i < candidates.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge* he = candidates[i];
            int incoming = meshIndex(he->previous()->startingVertex);
            int outgoingTo = meshIndex(he->mirrorHalfEdge()->startingVertex);
            int g = faceOfEdge(key(incoming, x));
            int end = faceOfEdge(key(x, outgoingTo));
            for ( int steps = 0; steps <= faces.size(); steps++ ) {
                if ( g == f ) {
                    return he;
                }
                if ( g == end ) {
                    break;
                }
                g = faceOfEdge(key(nextInFace(g, x), x));
            }
        }
        throw java::IllegalStateException(
            "Mesh construction found no corner at vertex " +
            java::String::valueOf(x) + " for face " + java::String::valueOf(f));
    }

    int nextInFace(int f, int x) const
    {
        const java::ArrayList<int>& polygon = faces[f];
        for ( int i = 0; i < polygon.size(); i++ ) {
            if ( polygon[i] == x ) {
                return polygon[(i + 1) % polygon.size()];
            }
        }
        throw java::IllegalStateException("Vertex " + java::String::valueOf(x) +
            " is not in face " + java::String::valueOf(f));
    }

    /**
    Once all the edges of the polygon exist, its half-edges form a loop:
    its face becomes a mesh face.
    */
    void finishFace(const java::ArrayList<int>& polygon)
    {
        _PolyhedralBoundedSolidHalfEdge* he =
            halfEdgeOf(key(polygon[0], polygon[1]));
        _PolyhedralBoundedSolidLoop* loop = he->parentLoop;
        _PolyhedralBoundedSolidFace* face = loop->parentFace;

        if ( loop->halfEdgesList.size() != polygon.size() || !isOpen(face) ) {
            throw java::IllegalStateException(
                "Mesh construction could not close a face of " +
                java::String::valueOf((long)polygon.size()) + " vertices");
        }
        if ( face->boundariesList.size() > 1 ) {
            face = PolyhedralBoundedSolidEulerOperators::lmfkrh(solid, loop,
                solid->getMaxFaceId() + 1);
        }
        builtFaces.put(face, true);
    }
};

}

PolyhedralBoundedSolid*
PolyhedralBoundedSolidMeshConstructions::createFromPolygonMesh(
    const java::ArrayList<Vector3Dd>& positions,
    const java::ArrayList<java::ArrayList<int> >& faces)
{
    if ( faces.isEmpty() ) {
        return nullptr;
    }
    PolyhedralBoundedSolid* solid = _MeshBuilder(positions, faces).build();
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}
