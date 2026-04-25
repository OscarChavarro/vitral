//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;
import java.io.Serial;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vertex2D;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Solid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.processing.ComputationalGeometry;
import vsdk.toolkit.processing.polygonClipper.PolygonProcessor;

/**
This class encapsulates a polyhedral boundary representation for 2-manifold
solids, as presented in [MANT1988].

As noted in [MANT1988].6.2.1., a "polyhedral model" is a boundary model
that has only planar faces. So, the name of this class `PolyhedralBoundedSolid`
implies that its faces should be planar. However, some intermediate steps
in complex algorithms such as the splitter and the set operators, permits
the use of "special" non-planar faces for "gluing".  Check [MANT1988] book
for more details on that.

As noted in [MANT1988].10.2.1, current implementation of the
`PolyhedralBoundedSolid` class uses a five-level hierarchical data
structure, consisting of:
  - PolyhedralBoundedSolid
  - _PolyhedralBoundedSolidFace
  - _PolyhedralBoundedSolidLoop
  - _PolyhedralBoundedSolidHalfEdge (and _PolyhedralBoundedSolidEdge)
  - _PolyhedralBoundedSolidVertex
Current class forms the root element (Facade) that gives access to faces,
edges and vertices of the model through aggregations in
CircularDoubleLinkedList's.

Note that this is a quite complex data-structure. Its implementation follows
the strategies outlined on book [MANT1988]. For the sake of clarity, it
was decided to keep most of its internal datastructures public, breaking
so the encapsulation concept. Note that if internal data structures are
made private and accessing get/set methods are provided for them, then
the complexity of algorithms using current data-structure should become
unmanageable, both in terms of code verbosity and bad performance (time
complexity) due to extra calls to a lot of simple methods.
*/
public class PolyhedralBoundedSolid extends Solid {
    @Serial private static final long serialVersionUID = 20061118L;

    public static final int PLUS = 1;
    public static final int MINUS = 0;
    private static final String SOLID_WITH_MESSAGE = "Solid with ";

    //= Main boundary representation solid data structure =============
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidFace> polygonsList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge> edgesList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex> verticesList;
    public int maxVertexId;
    public int maxFaceId;
    private boolean modelIsValid;

    //=================================================================
    public PolyhedralBoundedSolid()
    {
        polygonsList = new CircularDoubleLinkedList<>();
        edgesList = new CircularDoubleLinkedList<>();
        verticesList = new CircularDoubleLinkedList<>();
        maxVertexId = -1;
        maxFaceId = -1;
        modelIsValid = false;
    }

    //= SUPPORT MACROS FOR BASIC DATA-STRUCTURE MANIPULATION ==========

    /**
    Find the face identified with `id`. Returns null if face not found,
    or current founded face otherwise.
    Build based over function `fface` in program [MANT1988].11.9.
    @param id face id to search.
    @return matching face, or null when not found.
    */
    public _PolyhedralBoundedSolidFace
    findFace(int id)
    {
        int i;
        _PolyhedralBoundedSolidFace face;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            face = polygonsList.get(i);
            if ( face.id == id ) {
                return face;
            }
        }
        return null;
    }

    /**
    Finds the vertex identified by `id`.
    @param id vertex id to search.
    @return matching vertex, or null when not found.
    */
    public _PolyhedralBoundedSolidVertex
    findVertex(int id)
    {
        int i;
        _PolyhedralBoundedSolidVertex v;

        for ( i = 0; i < verticesList.size(); i++ ) {
            v = verticesList.get(i);
            if ( v.id == id ) {
                return v;
            }
        }
        return null;
    }

    /**
    Returns the list of faces stored in this solid.
    @return internal face list.
    */
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidFace>
    getPolygonsList()
    {
        return polygonsList;
    }

    /**
    Returns the list of edges stored in this solid.
    @return internal edge list.
    */
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge>
    getEdgesList()
    {
        return edgesList;
    }

    /**
    Returns the list of vertices stored in this solid.
    @return internal vertex list.
    */
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex>
    getVerticesList()
    {
        return verticesList;
    }

    //=================================================================

    /**
    This method gives access to the higher vertex id used in current solid
    model. This method is useful for higher level modeling operations, as
    noted in section [MANT1988].12.2. Current method (and method getMaxFaceId)
    is build after the function `getmaxnames` of program [MANT1988].12.1.
    @return the maximum id used in vertices set
    */
    public int getMaxVertexId()
    {
        return maxVertexId;
    }

    public void setMaxVertexId(int maxVertexId)
    {
        this.maxVertexId = maxVertexId;
    }

    /**
    This method gives access to the higher face id used in current solid
    model. This method is useful for higher level modeling operations, as
    noted in section [MANT1988].12.2. Current method (and method
    getMaxVertexId) is build after the function `getmaxnames` of program
    [MANT1988].12.1.
    @return the maximum id used on faces set
    */
    public int getMaxFaceId()
    {
        return maxFaceId;
    }

    public void setMaxFaceId(int maxFaceId)
    {
        this.maxFaceId = maxFaceId;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inOutRay input ray to test.
    @return hit ray with updated `t` when intersection exists, or null otherwise.
    */
    public Ray doIntersection(Ray inOutRay) {
        RayHit hit = new RayHit(RayHit.DETAIL_NONE, true);
        if ( doIntersection(inOutRay, hit) ) {
            return hit.ray();
        }
        return null;
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        int i;
        double minT; // Shortest distance founded so far
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(this);

        // Initialization values for search algorithm
        minT = Double.MAX_VALUE;
        RayHit bestInfo = null;
        Vector3D p;
        int pos;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            Ray ray = new Ray(inRay);
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            InfinitePlane containingPlane = face.getContainingPlane();
            if ( containingPlane == null ) {
                continue;
            }
            RayHit planeHit = new RayHit();
            if ( containingPlane.doIntersection(ray, planeHit) ) {
                Ray hit = planeHit.ray();
                if ( hit.t() < minT ) {
                    hit = hit.withDirection(hit.direction().normalized());
                    p = hit.origin().add(hit.direction().multiply(hit.t()));
                    pos = testPointInsideForRayIntersection(
                        face, p, numericContext.bigEpsilon());
                    if ( pos == Geometry.INSIDE || pos == Geometry.LIMIT ) {
                        minT = hit.t();
                        bestInfo = new RayHit(planeHit);
                    }
                }
            }
        }

        if ( bestInfo == null ) {
            return false;
        }
        if ( outHit != null ) {
            outHit.clone(bestInfo);
            outHit.setRay(inRay.withT(minT));
        }
        return true;
    }

    private static Vector3D dropCoordinate(Vector3D in, int coordinate)
    {
        return switch (coordinate) {
            case 1 -> new Vector3D(in.y(), in.z(), 0);
            case 2 -> new Vector3D(in.x(), in.z(), 0);
            default -> new Vector3D(in.x(), in.y(), 0);
        };
    }

    private static int dominantCoordinateForFace(_PolyhedralBoundedSolidFace face)
    {
        Vector3D n = face.getContainingPlane().getNormal();

        if ( Math.abs(n.x()) >= Math.abs(n.y()) &&
             Math.abs(n.x()) >= Math.abs(n.z()) ) {
            return 1;
        }
        if ( Math.abs(n.y()) >= Math.abs(n.x()) &&
             Math.abs(n.y()) >= Math.abs(n.z()) ) {
            return 2;
        }
        return 3;
    }

    private static int testPointInsideForRayIntersection(
        _PolyhedralBoundedSolidFace face,
        Vector3D point,
        double tolerance)
    {
        int dominantCoordinate;
        int insideLoopCount;
        int i;
        Vector3D projectedPoint;
        Vertex2D projectedPoint2D;

        if ( face == null || face.getContainingPlane() == null ) {
            return Geometry.OUTSIDE;
        }

        dominantCoordinate = dominantCoordinateForFace(face);
        projectedPoint = dropCoordinate(point, dominantCoordinate);
        projectedPoint2D = new Vertex2D(projectedPoint.x(), projectedPoint.y());
        insideLoopCount = 0;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge start;
            List<Vertex2D> projectedLoopVertices;
            byte loopStatus;

            if ( he == null ) {
                return Geometry.OUTSIDE;
            }
            start = he;
            projectedLoopVertices = new ArrayList<>();

            do {
                if ( VSDK.vectorDistance(point, he.startingVertex.position)
                     < 2 * tolerance ) {
                    return Geometry.LIMIT;
                }
                if ( ComputationalGeometry.lineSegmentContainmentTest(
                         he.startingVertex.position,
                         he.next().startingVertex.position,
                         point, tolerance) == Geometry.LIMIT ) {
                    return Geometry.LIMIT;
                }

                projectedPoint = dropCoordinate(he.startingVertex.position,
                    dominantCoordinate);
                projectedLoopVertices.add(
                    new Vertex2D(projectedPoint.x(), projectedPoint.y()));
                he = he.next();
            } while ( he != start );

            loopStatus = PolygonProcessor.isPointInsidePolygon2D(
                projectedPoint2D, projectedLoopVertices);
            if ( loopStatus == 0 ) {
                return Geometry.LIMIT;
            }
            if ( loopStatus > 0 ) {
                insideLoopCount++;
            }
        }

        return ((insideLoopCount % 2) == 1) ?
            Geometry.INSIDE : Geometry.OUTSIDE;
    }

    /**
    Fills `outData` with intersection details for the provided ray and distance.
    @param inRay input ray.
    @param inT ray parameter used as candidate intersection distance.
    @param outData output hit information container.
    */
    @Override
    public void doExtraInformation(Ray inRay, double inT, 
                                  RayHit outData) {
        if ( outData == null ) {
            return;
        }
        doIntersection(inRay.withT(inT), outData);
    }

    /** Needed for supplying the Geometry.getMinMax operation */
    private double[] calculateMinMaxPositions() {
        double[] minMax = new double[6];

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        int i;

        for ( i = 0; i < verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v;
            v = verticesList.get(i);
            double x = v.position.x();
            double y = v.position.y();
            double z = v.position.z();

            if ( x < minX ) minX = x;
            if ( y < minY ) minY = y;
            if ( z < minZ ) minZ = z;
            if ( x > maxX ) maxX = x;
            if ( y > maxY ) maxY = y;
            if ( z > maxZ ) maxZ = z;
        }
        minMax[0] = minX;
        minMax[1] = minY;
        minMax[2] = minZ;
        minMax[3] = maxX;
        minMax[4] = maxY;
        minMax[5] = maxZ;
        return minMax;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    @return six-value array with min/max coordinates: `[minX, minY, minZ, maxX, maxY, maxZ]`.
    */
    @Override
    public double[] getMinMax() {
        return calculateMinMaxPositions();
    }

    /**
    Returns true if the model was validated using
    `PolyhedralBoundedSolidValidationEngine.validateIntermediate` or
    `PolyhedralBoundedSolidValidationEngine.validateStrict`, and validation
    succeeded after the latest geometrical or topological operation.
    @return true when the cached validation flag is valid; false otherwise.
    */
    public boolean isValid()
    {
        return modelIsValid;
    }

    void setValidationState(boolean flag)
    {
        modelIsValid = flag;
    }

    /**
    Given `this` and `other` solids, this method erases the `other` solid
    while appending its parts to current one as a new shell. This method
    follows section [MANT1988].12.4.1 and program [MANT1988].12.8.
    @param other solid whose topology is moved into this solid.
    */
    public void merge(PolyhedralBoundedSolid other)
    {
        //-----------------------------------------------------------------
        int offsetFacesId = getMaxFaceId();
        int offsetVertexId = getMaxVertexId();
        _PolyhedralBoundedSolidFace f;
        _PolyhedralBoundedSolidVertex v;

        //-----------------------------------------------------------------
        while ( other.polygonsList.size() > 0 ) {
            f = other.polygonsList.get(0);
            f.id += offsetFacesId;
            if ( f.id > maxFaceId ) maxFaceId = f.id;
            polygonsList.add(f);
            other.polygonsList.remove(0);
        }
        while ( other.edgesList.size() > 0 ) {
            edgesList.add(other.edgesList.get(0));
            other.edgesList.remove(0);
        }
        while ( other.verticesList.size() > 0 ) {
            v = other.verticesList.get(0);
            v.id += offsetVertexId;
            if ( v.id > maxVertexId ) maxVertexId = v.id;
            verticesList.add(v);
            other.verticesList.remove(0);
        }
    }

    //= TEXTUAL QUERY OPERATIONS ======================================

    private String intPreSpaces(int val, int fieldSize)
    {
        String cad;
        int remain;
        StringBuilder sb;

        cad = VSDK.formatNumberWithinZeroes(val, 1);
        remain = fieldSize - cad.length();
        sb = new StringBuilder(Math.max(fieldSize, cad.length()));

        for ( ; remain > 0; remain-- ) {
            sb.append(" ");
        }
        sb.append(cad);
        return sb.toString();
    }

    /**
    Check the general interface contract in superclass method
    Geometry.computeQuantitativeInvisibility.

    This is not well understood for cases of intersection with face limits
    (vertices and edges). In some cases, computation of quantitative
    invisibility seems to be failing.
    \todo  check well all limiting cases.
    @param origin ray origin.
    @param p target point.
    @return  the number of front facing surface elements (with
    respect to `origin`) between the `origin` point and the `p` point
    */
    @Override
    public int computeQuantitativeInvisibility(Vector3D origin, Vector3D p)
    {
        int qi = 0;
        int i;
        int j;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(this);
        Vector3D d = p.subtract(origin);
        Vector3D pi;
        double t0 = d.length();
        d = d.normalized();
        int pos;
        double[] distances = new double[polygonsList.size()];
        int frontHitCount = 0;

        Ray ray = new Ray(origin, d);
        RayHit info;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            RayHit planeHit = new RayHit();
            if ( face.getContainingPlane().doIntersection(ray, planeHit) ) {
                Ray hit = planeHit.ray();
                if ( hit.t() < t0 - numericContext.epsilon() ) {
                    hit = hit.withDirection(hit.direction().normalized());
                    pi = hit.origin().add(hit.direction().multiply(hit.t()));
                    pos = face.testPointInside(pi, numericContext.bigEpsilon());
                    if ( pos == Geometry.INSIDE ||
                         (pos == Geometry.LIMIT &&
                          boundaryHitProducesInteriorPenetration(
                              pi, d, numericContext.bigEpsilon())) ) {
                        info = planeHit;
                        if ( info.n.dotProduct(d) < 0.0 ) {
                            boolean considerIt = true;
                            for ( j = 0; j < frontHitCount; j++ ) {
                                if ( Math.abs(distances[j]-hit.t()) <
                                     numericContext.bigEpsilon() ) {
                                    considerIt = false;
                                    break;
                                }
                            }
                            if ( considerIt ) {
                                qi++;
                                distances[frontHitCount] = hit.t();
                                frontHitCount++;
                            }
                        }
                    }
                }
            }
        }

        return qi;
    }

    private boolean boundaryHitProducesInteriorPenetration(
        Vector3D hitPoint,
        Vector3D direction,
        double tolerance)
    {
        Vector3D afterHit = hitPoint.add(direction.multiply(4.0 * tolerance));
        boolean touchesBoundary = false;

        for ( int i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);

            if ( Math.abs(face.getContainingPlane().pointDistance(hitPoint)) >
                 tolerance ) {
                continue;
            }
            int pointStatus = face.testPointInside(hitPoint, tolerance);
            if ( pointStatus == Geometry.OUTSIDE ) {
                continue;
            }

            touchesBoundary = true;
            int halfSpaceStatus = face.getContainingPlane().doContainmentTestHalfSpace(
                afterHit, tolerance);
            if ( halfSpaceStatus != Geometry.INSIDE ) {
                return false;
            }
        }
        return touchesBoundary;
    }

    /**
    Utility routine used to compare floating values inside the boundary
    representation winged edge data structure, following procedure `comp`
    from program [MANT1988].13.2.
    @param a first value.
    @param b second value.
    @param tolerance comparison tolerance.
    @return 0 if two numbers are nearly equal, 1 if a > b, -1 if a < b
    */
    public static int compareValue(double a, double b, double tolerance)
    {
        double delta;

        delta = Math.abs(a - b);
        if ( delta < tolerance ) {
            return 0;
        }
        else if ( a > b ) {
            return 1;
        }
        return -1;
    }

    /**
    This method get current solid in an "inverted" (geometrical sense) solid.
    Works on half edge data structure by inverting the order of each loop.
    This is an answer to problem [MANT1988].15.6.

    Current implementation does NOT correct face normals. Explicit model
    validation is encouraged after the application of this method.
    */
    public void revert()
    {
        int i;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            polygonsList.get(i).revert();
        }
    }


    /**
    Modifies ids for current solid's vertices, edges, faces and half-edges to
    make them consecutive from 1. Note that ids does not impact current
    solid geometry or topology, as they are use only for debugging and
    further construction / modification support.
    User of this class should keep in mind id changes when using this method.
    */
    public void compactIds()
    {
        int i;
        int j;

        for ( i = 0; i < verticesList.size(); i++ ) {
            verticesList.get(i).id = i+1;
        }
        maxVertexId = i;
        for ( i = 0; i < edgesList.size(); i++ ) {
            edgesList.get(i).id = i+1;
        }

        int k = 1;
        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            face.id = i+1;
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he;
                _PolyhedralBoundedSolidHalfEdge heStart;

                loop = face.boundariesList.get(j);

                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
                }
                heStart = he;
                do {
                    he.id = k;
                    k++;
                    he = he.next();
                    if ( he == null ) {
                        break;
                    }
                } while( he != heStart );
            }
        }
        maxFaceId = i;
    }

    @Override
    public String toString()
    {
        StringBuilder msg = new StringBuilder();
        int i;
        int j;

        msg.append("= POLYHEDRAL BOUNDED SOLID STRUCTURE ==========================================\n");
        msg.append(SOLID_WITH_MESSAGE).append(verticesList.size()).append(" vertices:\n");
        for ( i = 0; i < verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v;
            v = verticesList.get(i);
            msg.append("  - ").append(v).append("\n");
        }

        msg.append(SOLID_WITH_MESSAGE).append(edgesList.size()).append(" edges:\n");
        for ( i = 0; i < edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = edgesList.get(i);
            msg.append("  - ").append(e).append("\n");
        }
        msg.append(SOLID_WITH_MESSAGE).append(polygonsList.size()).append(" faces:\n");

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            msg.append("  - ").append(face).append("\n");
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he;
                _PolyhedralBoundedSolidHalfEdge heStart;

                msg.append("    . Loop ").append(j).append(", with half-edges: \n");
                loop = face.boundariesList.get(j);


                msg.append("      | HeID  | StartVertex | End Vertex | nccw He | pccw He | parentEdge | mirror He | neighbor face\n");
                msg.append("      +-------+-------------+------------+---------+---------+------------+-----------+-------------+\n");

                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    msg.append("<Loop without starting half-edge!>\n");
                    continue;
                }
                heStart = he;
                do {
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        msg.append("      |  - (not closed loop)\n");
                        break;
                    }

                    msg.append("      | ")
                        .append(intPreSpaces(he.id, 4))
                        .append((he == loop.boundaryStartHalfEdge) ? "*" : " ")
                        .append(" | ")
                        .append(intPreSpaces(he.startingVertex.id, 11))
                        .append(" | ")
                        .append(intPreSpaces(he.next().startingVertex.id, 10))
                        .append(" | ")
                        .append(intPreSpaces(he.next().id, 7))
                        .append(" | ")
                        .append(intPreSpaces(he.previous().id, 7))
                        .append(" | ");
                    msg.append((he.parentEdge!=null)?
                        intPreSpaces(he.parentEdge.id, 10):"    <null>");
                    msg.append(" | ");
                    if ( he.mirrorHalfEdge() != null ) {
                        msg.append(intPreSpaces(he.mirrorHalfEdge().id, 9))
                            .append(" | ")
                            .append(intPreSpaces(he.mirrorHalfEdge().parentLoop.parentFace.id, 11))
                            .append(" | ");
                    }
                    else {
                        msg.append(" No Mirror Half Edge!   | ");
                    }

                    msg.append("\n");

                } while( he != heStart );
            }
        }
        msg.append("= END OF POLYHEDRAL BOUNDED SOLID STRUCTURE ===================================\n");
        return msg.toString();
    }

    @Override
    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        return this;
    }
}
