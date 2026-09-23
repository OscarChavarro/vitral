package vsdk.toolkit.environment.geometry.surface;
import java.io.Serial;

// VitralSDK classes
import vsdk.toolkit.common.symbolicAlgebra.AlgebraicExpression;
import vsdk.toolkit.common.symbolicAlgebra.AlgebraicExpressionException;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.RayHit;

public class FunctionalExplicitSurface extends Surface
{
    @Serial private static final long serialVersionUID = 20071015L;

    private AlgebraicExpression xyFunction;
    private String functionExpression;
    private double minXBound;
    private double minYBound;
    private double minZBound;
    private double maxXBound;
    private double maxYBound;
    private double maxZBound;
    private int tesselationHintX;
    private int tesselationHintY;
    private TriangleMesh internalTriangleMesh;

    public FunctionalExplicitSurface(String fxy)
    {
        init(fxy);
    }

    private void init(String fxy)
    {
        functionExpression = fxy;
        xyFunction = new AlgebraicExpression();
        try {
            xyFunction.setExpression(fxy);
        }
        catch ( AlgebraicExpressionException e ) {
            Logger.reportMessage(this, VSDK.WARNING, 
                "constructor",
                "Cannot create algebraic expression for \"" + fxy + "\":\n" + e);
            try {
                xyFunction.setExpression("0");
            }
            catch ( AlgebraicExpressionException e2 ) {
                Logger.reportMessage(this, VSDK.FATAL_ERROR, 
                    "constructor",
                    "So bad. Something is wrong with algebraic expressions!:\n" + e2);
            }
        }
        minXBound = minYBound = minZBound = -1.0;
        maxXBound = maxYBound = maxZBound = 1.0;
        tesselationHintX = 10;
        tesselationHintY = 10;
        updateInternalGeometry();
    }

    public String getFunctionExpression()
    {
        return functionExpression;
    }

    public void setBounds(double minXBound, double minYBound, double minZBound,
                          double maxXBound, double maxYBound, double maxZBound)
    {
        this.minXBound = minXBound;
        this.minYBound = minYBound;
        this.minZBound = minZBound;
        this.maxXBound = maxXBound;
        this.maxYBound = maxYBound;
        this.maxZBound = maxZBound;
        updateInternalGeometry();
    }

    public void setTesselationHint(int tesx, int tesy)
    {
        tesselationHintX = tesx;
        tesselationHintY = tesy;
        updateInternalGeometry();
    }

    public int getTesselationHintX()
    {
        return tesselationHintX;
    }

    public int getTesselationHintY()
    {
        return tesselationHintY;
    }

    public double getMinXBound()
    {
        return minXBound;
    }

    public double getMinYBound()
    {
        return minYBound;
    }

    public double getMinZBound()
    {
        return minZBound;
    }

    public double getMaxXBound()
    {
        return maxXBound;
    }

    public double getMaxYBound()
    {
        return maxYBound;
    }

    public double getMaxZBound()
    {
        return maxZBound;
    }

    private int coord(int tesselationHintX, int tesselationHintY, int ix, int iy)
    {
        return ((tesselationHintX+1)*iy) + ix;
    }

    private void updateInternalGeometry()
    {
        //-----------------------------------------------------------------
        // Size of each tile in x direction
        double dx = (maxXBound - minXBound) / ((double)tesselationHintX); 
        // Size of each tile in y direction
        double dy = (maxYBound - minYBound) / ((double)tesselationHintY);

        // Temporary variable
        double x;
        double y;
        int ix;
        int iy;
        int index;

        internalTriangleMesh = new TriangleMesh();

        //-----------------------------------------------------------------
        internalTriangleMesh.initVertexPositionsArray((tesselationHintX+1)*(tesselationHintY+1));
        double v[];
        double z;

        v = internalTriangleMesh.getVertexPositions();
        try {
            index = 0;
            for ( iy = 0, y = minYBound; iy <= tesselationHintY; iy++, y += dy ) {
                xyFunction.defineValue("y", y);
                for ( ix = 0, x = minXBound; ix <= tesselationHintX; ix++, x += dx ) {
                    xyFunction.defineValue("x", x);
                    z = xyFunction.eval();
                    if ( z > maxZBound ) {
                        z = maxZBound;
                    }
                    if ( z < minZBound ) {
                        z = minZBound;
                    }
                    v[3*index] = x;
                    v[3*index+1] = y;
                    v[3*index+2] = z;
                    index++;
                }
            }
        }
        catch ( AlgebraicExpressionException e ) {
            Logger.reportMessage(this, VSDK.WARNING, 
                "constructor",
                "Cannot evaluate algebraic expression!" + e);
            return;
        }

        //-----------------------------------------------------------------
        internalTriangleMesh.initTriangleArrays(tesselationHintX*tesselationHintY*2);
        int t[];

        index = 0;
        t = internalTriangleMesh.getTriangleIndexes();
        for ( iy = 0; iy < tesselationHintY; iy++ ) {
            for ( ix = 0; ix < tesselationHintX; ix++ ) {
                t[3*index] = coord(tesselationHintX, tesselationHintY, ix, iy);
                t[3*index+1] = coord(tesselationHintX, tesselationHintY, ix+1, iy);
                t[3*index+2] = coord(tesselationHintX, tesselationHintY, ix+1, iy+1);
                index++;

                t[3*index] = coord(tesselationHintX, tesselationHintY, ix, iy);
                t[3*index+1] = coord(tesselationHintX, tesselationHintY, ix+1, iy+1);
                t[3*index+2] = coord(tesselationHintX, tesselationHintY, ix, iy+1);
                index++;
            }
        }

        //-----------------------------------------------------------------
        internalTriangleMesh.calculateNormals();
    }

    public TriangleMesh getInternalTriangleMesh()
    {
        return internalTriangleMesh;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax() {
        return internalTriangleMesh.getMinMax();
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    \todo  Should not delegate work over tesselated geometry version. Should
    evaluate directly from algebraic function surface!
    @param inOut_Ray
    @return true if given ray intersects current FunctionalExplicitSurface
    */
    public Ray
    doIntersectionFirstHit(Ray inOut_Ray) {
        RayHit hit = new RayHit();
        if ( internalTriangleMesh.doIntersectionFirstHit(inOut_Ray, hit) ) {
            return hit.getRay();
        }
        return null;
    }

    @Override
    public boolean doIntersectionFirstHit(Ray inRay, RayHit outHit)
    {
        return internalTriangleMesh.doIntersectionFirstHit(inRay, outHit);
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param inT
    @param outData
    */
    public void
    doExtraInformation(Ray inRay, double inT,
                                   RayHit outData) {
        RayHit hit = new RayHit();
        if ( internalTriangleMesh.doIntersectionFirstHit(inRay.withT(inT), hit) ) {
            outData.clone(hit);
        }
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doContainmentTest.
    @param p
    @param distanceTolerance
    @return INSIDE, OUTSIDE or LIMIT constant value 
    */
    @Override
    public int doContainmentTest(Vector3Dd p, double distanceTolerance)
    {
        return internalTriangleMesh.doContainmentTest(p, distanceTolerance);
    }

}
