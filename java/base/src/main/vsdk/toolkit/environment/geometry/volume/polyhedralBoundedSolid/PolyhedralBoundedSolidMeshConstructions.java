//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import vsdk.toolkit.common.dataStructures.CircularArrayList;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Builds the boundary representation of a closed polygon mesh with Euler
operators only, so it can be used for any solid whose boundary is known as a
list of polygons (i.e. the polygonization of an implicit surface), of any
genus, and with any number of connected components (one shell each).

The mesh must be a closed, oriented 2-manifold: every directed edge of a face
appears once, and its reverse in exactly one other face; faces are listed
counterclockwise as seen from outside the solid, and are planar.

The faces are added one at a time in breadth first order over the face
adjacency, so each new face shares at least one edge with the ones already
built. The faces of the solid that are not mesh faces yet ("open" faces)
always cover the part of the surface still to be built; a new face is carved
out of them:
  - The first face is a lamina (`mvfs`, `smev`, `smef`, as the circular
    lamina of [MANT1988].12.2), whose other side is the first open face.
  - The polygon of a new face alternates runs of edges that already exist
    (whose open side is on the face) with runs of new edges. A run of new
    edges goes from the end of an existing run to the start of the next one,
    through vertices that do not exist yet: they are created as struts
    (`lmev`, [MANT1988].11.3.2), and the last edge closes the run:
      - with `lmef` ([MANT1988].11.3.3) when both ends are on the same loop,
        splitting the face so the new face is on the side of the polygon,
      - with `lmekr` ([MANT1988].11.5) when they are on two loops of the same
        face,
      - or with `lkfmrh` and then `lmekr` when they are on two different
        faces, as when closing a handle: the same strategy used to glue the
        ends of a closed rotational sweep in [MANT1988].12.4.2.
  - When all the edges of a face already exist, it is the open face they
    bound.
  - A finished face that shares its face record with other loops gets a face
    of its own (`lmfkrh`).
*/
public class PolyhedralBoundedSolidMeshConstructions
{
    /**
    @param positions positions of the vertices of the mesh
    @param faces vertex indexes of each face, counterclockwise as seen from
    outside
    @return a new solid with the mesh as its boundary, or null if the mesh
    has no faces
    @throws IllegalArgumentException if the mesh is not a closed oriented
    2-manifold
    */
    public static PolyhedralBoundedSolid createFromPolygonMesh(
        List<Vector3Dd> positions, List<int[]> faces)
    {
        if ( faces == null || faces.isEmpty() ) {
            return null;
        }
        PolyhedralBoundedSolid solid =
            new _MeshBuilder(positions, faces).build();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static final class _MeshBuilder
    {
        private final List<Vector3Dd> positions;
        private final List<int[]> faces;
        private final int numberOfVertices;
        private final PolyhedralBoundedSolid solid;

        /// Mesh face of each directed edge
        private final HashMap<Long, Integer> faceOfDirectedEdge;
        /// Half-edge of the solid along each directed edge already created
        private final HashMap<Long, _PolyhedralBoundedSolidHalfEdge> halfEdges;
        /// Half-edges of the solid starting at each vertex
        private final ArrayList<ArrayList<_PolyhedralBoundedSolidHalfEdge>> outgoing;
        private final boolean[] vertexCreated;
        private final Set<_PolyhedralBoundedSolidFace> builtFaces;
        private int registeredEdges;

        _MeshBuilder(List<Vector3Dd> positions, List<int[]> faces)
        {
            this.positions = positions;
            this.faces = faces;
            this.numberOfVertices = positions.size();
            this.solid = new PolyhedralBoundedSolid();
            this.faceOfDirectedEdge = new HashMap<Long, Integer>();
            this.halfEdges = new HashMap<Long, _PolyhedralBoundedSolidHalfEdge>();
            this.outgoing = new ArrayList<ArrayList<_PolyhedralBoundedSolidHalfEdge>>();
            this.vertexCreated = new boolean[numberOfVertices];
            this.builtFaces = Collections.newSetFromMap(
                new IdentityHashMap<_PolyhedralBoundedSolidFace, Boolean>());
            this.registeredEdges = 0;
            for ( int i = 0; i < numberOfVertices; i++ ) {
                outgoing.add(new ArrayList<_PolyhedralBoundedSolidHalfEdge>());
            }
        }

        private long key(int from, int to)
        {
            return ((long)from) * numberOfVertices + to;
        }

        private static int meshIndex(_PolyhedralBoundedSolidVertex vertex)
        {
            return vertex.id - 1;
        }

        //= Mesh checks =====================================================

        private void indexMesh()
        {
            for ( int f = 0; f < faces.size(); f++ ) {
                int[] polygon = faces.get(f);
                if ( polygon.length < 3 ) {
                    throw new IllegalArgumentException(
                        "Face " + f + " has less than three vertices");
                }
                for ( int i = 0; i < polygon.length; i++ ) {
                    int from = polygon[i];
                    int to = polygon[(i + 1) % polygon.length];
                    if ( from < 0 || from >= numberOfVertices || from == to ) {
                        throw new IllegalArgumentException(
                            "Face " + f + " has an invalid edge");
                    }
                    if ( faceOfDirectedEdge.put(key(from, to), f) != null ) {
                        throw new IllegalArgumentException(
                            "Edge " + from + "-" + to + " is used twice in the same direction");
                    }
                }
            }
            for ( Long directed : faceOfDirectedEdge.keySet() ) {
                int from = (int)(directed / numberOfVertices);
                int to = (int)(directed % numberOfVertices);
                if ( !faceOfDirectedEdge.containsKey(key(to, from)) ) {
                    throw new IllegalArgumentException(
                        "Edge " + from + "-" + to + " is on the border of the mesh");
                }
            }
        }

        //= Bookkeeping =====================================================

        /**
        Indexes the half-edges of the edges created since the last call.
        */
        private void registerNewEdges()
        {
            CircularArrayList<_PolyhedralBoundedSolidEdge> edges = solid.getEdgesList();
            for ( ; registeredEdges < edges.size(); registeredEdges++ ) {
                _PolyhedralBoundedSolidEdge edge = edges.get(registeredEdges);
                registerHalfEdge(edge.rightHalf);
                registerHalfEdge(edge.leftHalf);
            }
        }

        private void registerHalfEdge(_PolyhedralBoundedSolidHalfEdge he)
        {
            int from = meshIndex(he.startingVertex);
            int to = meshIndex(he.mirrorHalfEdge().startingVertex);
            halfEdges.put(key(from, to), he);
            outgoing.get(from).add(he);
        }

        private boolean isOpen(_PolyhedralBoundedSolidFace face)
        {
            return !builtFaces.contains(face);
        }

        private static _PolyhedralBoundedSolidFace faceOf(
            _PolyhedralBoundedSolidHalfEdge he)
        {
            return he.parentLoop.parentFace;
        }

        //= Construction ====================================================

        PolyhedralBoundedSolid build()
        {
            indexMesh();

            boolean[] queued = new boolean[faces.size()];
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();

            // Each connected component of the mesh is a shell
            for ( int first = 0; first < faces.size(); first++ ) {
                if ( queued[first] ) {
                    continue;
                }
                buildFirstFace(faces.get(first));
                queued[first] = true;
                enqueueNeighbors(first, queued, queue);

                while ( !queue.isEmpty() ) {
                    int f = queue.poll();
                    buildFace(f);
                    enqueueNeighbors(f, queued, queue);
                }
            }

            if ( solid.getPolygonsList().size() != faces.size() ) {
                throw new IllegalStateException(
                    "Mesh construction left " +
                    (solid.getPolygonsList().size() - faces.size()) +
                    " faces that are not part of the mesh");
            }
            return solid;
        }

        private void enqueueNeighbors(int f, boolean[] queued,
            ArrayDeque<Integer> queue)
        {
            int[] polygon = faces.get(f);
            for ( int i = 0; i < polygon.length; i++ ) {
                int from = polygon[i];
                int to = polygon[(i + 1) % polygon.length];
                int neighbor = faceOfDirectedEdge.get(key(to, from));
                if ( !queued[neighbor] ) {
                    queued[neighbor] = true;
                    queue.add(neighbor);
                }
            }
        }

        /**
        [MANT1988].12.2: a lamina with the vertices of the polygon; its side
        that follows the order of the polygon is the mesh face, the other one
        is the first open face.
        */
        private void buildFirstFace(int[] polygon)
        {
            int n = polygon.length;
            int faceId = solid.getMaxFaceId() + 1;

            PolyhedralBoundedSolidEulerOperators.mvfs(solid,
                positions.get(polygon[0]), polygon[0] + 1, faceId);
            vertexCreated[polygon[0]] = true;
            for ( int i = 1; i < n; i++ ) {
                PolyhedralBoundedSolidEulerOperators.smev(solid, faceId,
                    polygon[i - 1] + 1, polygon[i] + 1, positions.get(polygon[i]));
                vertexCreated[polygon[i]] = true;
            }
            PolyhedralBoundedSolidEulerOperators.smef(solid, faceId,
                polygon[n - 1] + 1, polygon[0] + 1, faceId + 1);
            registerNewEdges();
            finishFace(polygon);
        }

        private boolean edgeExists(int from, int to)
        {
            return halfEdges.containsKey(key(from, to));
        }

        private void buildFace(int f)
        {
            int[] polygon = faces.get(f);
            int n = polygon.length;
            int start = -1;

            // A run of new edges that starts right after an existing one
            for ( int i = 0; i < n; i++ ) {
                boolean previousExists = edgeExists(polygon[(i + n - 1) % n], polygon[i]);
                boolean currentExists = edgeExists(polygon[i], polygon[(i + 1) % n]);
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
                    ArrayList<Integer> newVertices = new ArrayList<Integer>();
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
        private void buildRun(int f, int beforeA, int a, List<Integer> newVertices,
            int b, int afterB)
        {
            _PolyhedralBoundedSolidHalfEdge corner =
                halfEdges.get(key(beforeA, a)).next();
            _PolyhedralBoundedSolidHalfEdge target;
            int last = a;

            if ( edgeExists(b, afterB) ) {
                target = halfEdges.get(key(b, afterB));
            }
            else {
                target = findCorner(b, f);
            }

            for ( int w : newVertices ) {
                PolyhedralBoundedSolidEulerOperators.lmev(solid, corner, corner,
                    w + 1, positions.get(w));
                vertexCreated[w] = true;
                registerNewEdges();
                // The strut is last -> w -> last, before the corner: the
                // corner at w is its returning half-edge
                corner = halfEdges.get(key(w, last));
                last = w;
            }
            connect(corner, target);
            registerNewEdges();
        }

        /**
        Adds the edge from the vertex of `corner` to the vertex of `target`,
        before both half-edges.
        */
        private void connect(_PolyhedralBoundedSolidHalfEdge corner,
            _PolyhedralBoundedSolidHalfEdge target)
        {
            if ( corner.parentLoop == target.parentLoop ) {
                // The new face is the one from target to corner
                PolyhedralBoundedSolidEulerOperators.lmef(solid, target, corner,
                    solid.getMaxFaceId() + 1);
                return;
            }

            _PolyhedralBoundedSolidFace cornerFace = faceOf(corner);
            _PolyhedralBoundedSolidFace targetFace = faceOf(target);

            if ( cornerFace != targetFace ) {
                if ( !isOpen(cornerFace) || !isOpen(targetFace) ) {
                    throw new IllegalStateException(
                        "Mesh construction tried to merge a finished face");
                }
                // Both loops in one face: a ring of the other one
                if ( targetFace.boundariesList.size() == 1 ) {
                    PolyhedralBoundedSolidEulerOperators.lkfmrh(solid, cornerFace, targetFace);
                }
                else if ( cornerFace.boundariesList.size() == 1 ) {
                    PolyhedralBoundedSolidEulerOperators.lkfmrh(solid, targetFace, cornerFace);
                }
                else {
                    while ( targetFace.boundariesList.size() > 1 ) {
                        PolyhedralBoundedSolidEulerOperators.lringmv(solid,
                            targetFace.boundariesList.get(1), cornerFace, false);
                    }
                    PolyhedralBoundedSolidEulerOperators.lkfmrh(solid, cornerFace, targetFace);
                }
            }
            PolyhedralBoundedSolidEulerOperators.lmekr(solid, corner, target);
        }

        /**
        Finds, among the open corners of vertex `x` (the open half-edges that
        start at it), the one where mesh face `f` goes: the one whose gap
        between its previous edge and its own edge contains `f` in the star of
        `x`.
        */
        private _PolyhedralBoundedSolidHalfEdge findCorner(int x, int f)
        {
            ArrayList<_PolyhedralBoundedSolidHalfEdge> candidates =
                new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

            for ( _PolyhedralBoundedSolidHalfEdge he : outgoing.get(x) ) {
                if ( isOpen(faceOf(he)) ) {
                    candidates.add(he);
                }
            }
            if ( candidates.size() == 1 ) {
                return candidates.get(0);
            }
            for ( _PolyhedralBoundedSolidHalfEdge he : candidates ) {
                int incoming = meshIndex(he.previous().startingVertex);
                int outgoingTo = meshIndex(he.mirrorHalfEdge().startingVertex);
                int g = faceOfDirectedEdge.get(key(incoming, x));
                int end = faceOfDirectedEdge.get(key(x, outgoingTo));
                for ( int steps = 0; steps <= faces.size(); steps++ ) {
                    if ( g == f ) {
                        return he;
                    }
                    if ( g == end ) {
                        break;
                    }
                    g = faceOfDirectedEdge.get(key(nextInFace(g, x), x));
                }
            }
            throw new IllegalStateException(
                "Mesh construction found no corner at vertex " + x + " for face " + f);
        }

        private int nextInFace(int f, int x)
        {
            int[] polygon = faces.get(f);
            for ( int i = 0; i < polygon.length; i++ ) {
                if ( polygon[i] == x ) {
                    return polygon[(i + 1) % polygon.length];
                }
            }
            throw new IllegalStateException("Vertex " + x + " is not in face " + f);
        }

        /**
        Once all the edges of the polygon exist, its half-edges form a loop:
        its face becomes a mesh face.
        */
        private void finishFace(int[] polygon)
        {
            _PolyhedralBoundedSolidHalfEdge he =
                halfEdges.get(key(polygon[0], polygon[1]));
            _PolyhedralBoundedSolidLoop loop = he.parentLoop;
            _PolyhedralBoundedSolidFace face = loop.parentFace;

            if ( loop.halfEdgesList.size() != polygon.length || !isOpen(face) ) {
                throw new IllegalStateException(
                    "Mesh construction could not close a face of " +
                    polygon.length + " vertices");
            }
            if ( face.boundariesList.size() > 1 ) {
                face = PolyhedralBoundedSolidEulerOperators.lmfkrh(solid, loop,
                    solid.getMaxFaceId() + 1);
            }
            builtFaces.add(face);
        }
    }
}
