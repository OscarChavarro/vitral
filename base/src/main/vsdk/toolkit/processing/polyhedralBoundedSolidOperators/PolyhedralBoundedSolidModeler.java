package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

// Java classes
import java.util.ArrayList;

// Vitral classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.processing.ProcessingElement;

/**
Utility class with static modeling and boolean operations specific to
`PolyhedralBoundedSolid`.

This class contains creation/sweep/split/set-op helpers and centralizes the
polyhedral B-Rep operations previously exposed through `GeometricModeler`.
*/
public class PolyhedralBoundedSolidModeler extends ProcessingElement
{
    public static final int UNION = 1;
    public static final int INTERSECTION = 2;
    public static final int SUBTRACT = 3;
    public static final int DIFFERENCE = SUBTRACT;

    /**
    Implements the construction style from [MANT1988] section 12.2 / program
    12.1.

    Builds an arc on plane `z = h`, centered at (`cx`, `cy`), radius `rad`.
    The start vertex already exists in `faceId` and is identified by
    `vertexId`; the method appends `n` new edge steps from `phi1` to `phi2`
    (degrees, counterclockwise, 0 degrees on +X).
    */
    public static void addArc(PolyhedralBoundedSolid solid,
        int faceId, int vertexId,
        double cx, double cy, double rad, double h, double phi1, double phi2,
        int n)
    {
        double x;
        double y;
        double angle;
        double inc;
        int prev;
        int i;
        int nextVertexId;

        angle = Math.toRadians(phi1);
        inc = Math.toRadians((phi2 - phi1) / ((double)n));
        prev = vertexId;
        for ( i = 0; i < n; i++ ) {
            angle += inc;
            x = cx + rad * Math.cos(angle);
            y = cy + rad * Math.sin(angle);
            nextVertexId = solid.getMaxVertexId() + 1;
            solid.smev(faceId, prev, nextVertexId, new Vector3D(x, y, h));
            prev = nextVertexId;
        }
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    /**
    Implements [MANT1988] section 12.2 / program 12.2.

    Creates a planar circular lamina as a single face by:
    1) creating an initial vertex (`mvfs`)
    2) adding arc vertices (`addArc`)
    3) closing the loop (`smef`)
    */
    public static PolyhedralBoundedSolid createCircularLamina(
        double cx, double cy, double rad, double h, int n)
    {
        PolyhedralBoundedSolid solid;

        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(cx + rad, cy, h), 1, 1);
        addArc(solid, 1, 1, cx, cy, rad, h, 0,
            ((double)(n-1)) * 360.0 / ((double)n), n-1);
        solid.smef(1, n, 1, 2);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    /**
    Generalized translational sweep of a face, inspired by [MANT1988] 12.3.1.

    Instead of pure translation, transform `T` can include translation,
    rotation, and scale in the face-local context.

    PRE: `face` is closed and planar.
    */
    public static void translationalSweepExtrudeFace(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face,
        Matrix4x4 T)
    {
        _PolyhedralBoundedSolidLoop l;
        _PolyhedralBoundedSolidHalfEdge first;
        _PolyhedralBoundedSolidHalfEdge scan;
        _PolyhedralBoundedSolidVertex v;
        Vector3D newPos;
        int i;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            l = face.boundariesList.get(i);
            first = l.boundaryStartHalfEdge;
            scan = first.next();
            v = scan.startingVertex;
            newPos = T.multiply(v.position);
            solid.lmev(scan, scan, solid.getMaxVertexId()+1, newPos);
            while ( scan != first ) {
                v = scan.next().startingVertex;
                newPos = T.multiply(v.position);
                solid.lmev(scan.next(), scan.next(),
                    solid.getMaxVertexId()+1, newPos);
                solid.lmef(scan.previous(), scan.next().next(),
                    solid.getMaxFaceId()+1);
                scan = (scan.next().mirrorHalfEdge()).next();
            }
            solid.lmef(scan.previous(), scan.next().next(),
                solid.getMaxFaceId()+1);
        }
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    /**
    Variant of `translationalSweepExtrudeFace` with an extra planar check pass.

    After sweep generation, each created side face is tested for planarity.
    If a face is non-planar, it is split once (`lmef`) to triangulate it.
    */
    public static void translationalSweepExtrudeFacePlanar(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face,
        Matrix4x4 T)
    {
        _PolyhedralBoundedSolidLoop l;
        _PolyhedralBoundedSolidHalfEdge first;
        _PolyhedralBoundedSolidHalfEdge scan;
        _PolyhedralBoundedSolidVertex v;
        Vector3D newPos;
        ArrayList<Integer> newfaces = new ArrayList<Integer>();
        int i;
        int newfaceid;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            l = face.boundariesList.get(i);
            first = l.boundaryStartHalfEdge;
            scan = first.next();
            v = scan.startingVertex;
            newPos = T.multiply(v.position);
            solid.lmev(scan, scan, solid.getMaxVertexId()+1, newPos);
            while ( scan != first ) {
                v = scan.next().startingVertex;
                newPos = T.multiply(v.position);
                solid.lmev(scan.next(), scan.next(),
                    solid.getMaxVertexId()+1, newPos);
                newfaceid = solid.getMaxFaceId()+1;
                solid.lmef(scan.previous(), scan.next().next(), newfaceid);
                newfaces.add(Integer.valueOf(newfaceid));
                scan = (scan.next().mirrorHalfEdge()).next();
            }
            newfaceid = solid.getMaxFaceId()+1;
            solid.lmef(scan.previous(), scan.next().next(), newfaceid);
            newfaces.add(Integer.valueOf(newfaceid));
        }

        _PolyhedralBoundedSolidFace newface;
        for ( i = 0; i < newfaces.size(); i++ ) {
            newfaceid = newfaces.get(i).intValue();
            newface = solid.findFace(newfaceid);
            if ( !PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(newface) ) {
                scan = newface.boundariesList.get(0).boundaryStartHalfEdge;
                newfaceid = solid.getMaxFaceId()+1;
                solid.lmef(scan.next(), scan.previous(), newfaceid);
            }
        }

        while ( newfaces.size() > 0 ) {
            newfaces.remove(0);
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    private static _PolyhedralBoundedSolidHalfEdge[] findWireSweepEnds(
        PolyhedralBoundedSolid solid)
    {
        _PolyhedralBoundedSolidHalfEdge first;
        _PolyhedralBoundedSolidHalfEdge last;

        first = solid.polygonsList.get(0).boundariesList.get(0)
            .boundaryStartHalfEdge;
        while ( first.parentEdge != first.next().parentEdge ) {
            first = first.next();
        }
        last = first.next();
        while ( last.parentEdge != last.next().parentEdge ) {
            last = last.next();
        }
        return new _PolyhedralBoundedSolidHalfEdge[] { first, last };
    }

    private static boolean isOnXAxis(Vector3D p, double tolerance)
    {
        return Math.abs(p.y) <= tolerance && Math.abs(p.z) <= tolerance;
    }

    private static void collapseFaceToAxisVertex(
        _PolyhedralBoundedSolidFace face, double x)
    {
        if ( face == null ) {
            return;
        }

        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            if ( he == null ) {
                continue;
            }
            do {
                he.startingVertex.position.x = x;
                he.startingVertex.position.y = 0.0;
                he.startingVertex.position.z = 0.0;
                he = he.next();
            } while ( he != null && he != start );
        }
    }

    /**
    Rotational sweep of an open wire profile around the X axis, following the
    construction style of [MANT1988] 12.2 and 12.5.

    PRE:
    - `solid` is a wire-like profile (single face, open loop)
    - profile lies on `z = 0`
    - `nfaces >= 3`
    */
    public static void rotationalSweepExtrudeWireAroundXAxis(
        PolyhedralBoundedSolid solid, int nfaces)
    {
        if ( solid == null || solid.polygonsList.size() < 1 || nfaces < 3 ) {
            return;
        }

        _PolyhedralBoundedSolidHalfEdge[] ends = findWireSweepEnds(solid);
        _PolyhedralBoundedSolidHalfEdge first = ends[0];
        _PolyhedralBoundedSolidHalfEdge last = ends[1];
        _PolyhedralBoundedSolidFace headf = solid.polygonsList.get(0);

        double axisTolerance = VSDK.EPSILON * 100.0;
        Vector3D firstEndpointPosition = new Vector3D(
            first.next().startingVertex.position);
        Vector3D lastEndpointPosition = new Vector3D(
            last.startingVertex.position);
        boolean firstEndpointOnAxis = isOnXAxis(firstEndpointPosition,
            axisTolerance);
        boolean lastEndpointOnAxis = isOnXAxis(lastEndpointPosition,
            axisTolerance);

        _PolyhedralBoundedSolidHalfEdge cfirst;
        _PolyhedralBoundedSolidHalfEdge scan = null;
        _PolyhedralBoundedSolidFace tailf;
        Vector3D v;
        Matrix4x4 rotation;

        cfirst = first;
        rotation = new Matrix4x4();
        rotation.axisRotation((2*Math.PI) / ((double)nfaces), 1, 0, 0);

        int i;
        for ( i = 0; i < nfaces-1; i++ ) {
            v = rotation.multiply(cfirst.next().startingVertex.position);
            solid.lmev(cfirst.next(), cfirst.next(), solid.getMaxVertexId()+1,
                v);
            scan = cfirst.next();

            while ( scan != last.next() ) {
                v = rotation.multiply(scan.previous().startingVertex.position);
                solid.lmev(scan.previous(), scan.previous(),
                    solid.getMaxVertexId()+1, v);
                solid.lmef(scan.previous().previous(), scan.next(),
                    solid.getMaxFaceId()+1);
                scan = (scan.next().next()).mirrorHalfEdge();
            }
            last = scan;
            cfirst = (cfirst.next().next()).mirrorHalfEdge();
        }

        tailf = solid.lmef(cfirst.next(), first.mirrorHalfEdge(),
            solid.getMaxFaceId()+1);
        while ( cfirst != scan ) {
            solid.lmef(cfirst, cfirst.next().next().next(),
                solid.getMaxFaceId()+1);
            cfirst = (cfirst.previous()).mirrorHalfEdge().previous();
        }

        // [MANT1988] 12.2: if a profile endpoint lies on the rotation axis,
        // cap vertices collapse to a single pole instead of leaving a
        // degenerate ring of coincident points.
        if ( firstEndpointOnAxis ) {
            collapseFaceToAxisVertex(headf, firstEndpointPosition.x);
        }
        if ( lastEndpointOnAxis ) {
            collapseFaceToAxisVertex(tailf, lastEndpointPosition.x);
        }
        if ( firstEndpointOnAxis || lastEndpointOnAxis ) {
            solid.maximizeFaces();
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    public static PolyhedralBoundedSolid createBrepFromParametricCurve(
        ParametricCurve curve)
    {
        int i;
        int j;
        int totalNumberOfPoints;
        double[][] list;
        Vector3D first;
        boolean beginning;
        int count;

        totalNumberOfPoints = 0;

        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == ParametricCurve.BREAK ) {
                i++;
                continue;
            }
            ArrayList<Vector3D> polyline = curve.calculatePoints(i, false);
            totalNumberOfPoints += polyline.size();
        }

        list = new double[totalNumberOfPoints][3];

        PolyhedralBoundedSolid solid;
        boolean firstLoop = true;
        int nextVertexId = 1;
        int lastLoopStartVertexId = 1;
        count = 0;
        boolean needAnEnding = false;
        int nextFaceId = 1;

        solid = new PolyhedralBoundedSolid();

        first = new Vector3D();
        beginning = true;
        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == ParametricCurve.BREAK ) {
                i++;

                solid.mef(1, 1,
                          lastLoopStartVertexId, lastLoopStartVertexId+1,
                          nextVertexId-1, nextVertexId-2, nextFaceId);
                nextFaceId++;

                if ( !firstLoop ) {
                    solid.kfmrh(2, nextFaceId-1);
                }
                firstLoop = false;
                beginning = true;
                continue;
            }

            // Approximate the current parametric segment with a polyline.
            ArrayList<Vector3D> polyline = curve.calculatePoints(i, false);

            // Insert the sampled polyline points into the current contour.
            for ( j = 0; j < polyline.size(); j++ ) {
                Vector3D vec = polyline.get(j);
                if ( !beginning ) {
                    Vector3D prev = new Vector3D(list[count-1][0],
                                                 list[count-1][1],
                                                 list[count-1][2]);
                    if ( VSDK.vectorDistance(vec, prev) > VSDK.EPSILON &&
                         VSDK.vectorDistance(vec, first) > VSDK.EPSILON ) {
                        list[count][0] = vec.x;
                        list[count][1] = vec.y;
                        list[count][2] = vec.z;
                        solid.smev(1, nextVertexId-1, nextVertexId,
                            new Vector3D(vec));
                        nextVertexId++;
                        count++;
                    }
                }
                else {
                    beginning = false;
                    list[count][0] = vec.x;
                    list[count][1] = vec.y;
                    list[count][2] = vec.z;
                    if ( firstLoop ) {
                        solid.mvfs(new Vector3D(vec), nextVertexId, nextFaceId);
                        nextVertexId++;
                        nextFaceId++;
                    }
                    else {
                        solid.smev(1, nextVertexId-1, nextVertexId,
                            new Vector3D(vec));
                        nextVertexId++;
                        solid.kemr(1, 1, nextVertexId-2, nextVertexId-1,
                            nextVertexId-1, nextVertexId-2);
                        lastLoopStartVertexId = nextVertexId-1;
                        needAnEnding = true;
                    }

                    first = new Vector3D(vec.x, vec.y, vec.z);
                    count++;
                }
            }
        }

        solid.mef(1, 1,
                  lastLoopStartVertexId, lastLoopStartVertexId+1,
                  nextVertexId-1, nextVertexId-2, nextFaceId);
        nextFaceId++;
        if ( needAnEnding ) {
            solid.kfmrh(2, nextFaceId-1);
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSplitter.split`.

    Splits `inSolid` by `inSplittingPlane` and appends resulting pieces to
    `outSolidsAbove` and `outSolidsBelow`.
    */
    public static void split(
        PolyhedralBoundedSolid inSolid,
        InfinitePlane inSplittingPlane,
        ArrayList<PolyhedralBoundedSolid> outSolidsAbove,
        ArrayList<PolyhedralBoundedSolid> outSolidsBelow)
    {
        PolyhedralBoundedSolidSplitter.split(inSolid, inSplittingPlane,
                                             outSolidsAbove, outSolidsBelow);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSetOperator.setOp`.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        return PolyhedralBoundedSolidSetOperator.setOp(inSolidA, inSolidB, op);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSetOperator.setOp`
    including debug mode.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug)
    {
        return PolyhedralBoundedSolidSetOperator.setOp(inSolidA, inSolidB, op,
            withDebug);
    }
}
