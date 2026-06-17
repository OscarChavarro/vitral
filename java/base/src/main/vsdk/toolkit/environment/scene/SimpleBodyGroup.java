package vsdk.toolkit.environment.scene;
import java.io.Serial;

import java.util.ArrayList;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;

public class SimpleBodyGroup extends Entity {
    @Serial private static final long serialVersionUID = 20070526L;

    //=======================================================================
    //- Model (1/6): set of bodies ------------------------------------
    private ArrayList <SimpleBody> bodies;

    //- Model (2/6): body geometric transformations -------------------
    private Vector3Dd position;
    private Vector3Dd scale;
    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4d rotation;
    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4d rotation_i;

    //- Model (3/6): body visual data ---------------------------------

    //- Model (4/6): body physical data -------------------------------

    //- Model (5/6): body structural relationships --------------------

    //- Model (6/6): body semantic data -------------------------------
    /// This string should be used for specific application defined
    /// functionality. Can be null.
    private String name;
    //=======================================================================

    public SimpleBodyGroup()
    {
        bodies = new ArrayList<SimpleBody>();
        rotation = new Matrix4x4d();
        rotation_i = new Matrix4x4d();
        position = new Vector3Dd(0, 0, 0);
        scale = new Vector3Dd(1, 1, 1);
    }

    public ArrayList <SimpleBody> getBodies()
    {
        return bodies;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String n)
    {
        name = n;
    }

    public Matrix4x4d getRotation()
    {
        return rotation;
    }

    public void setRotation(Matrix4x4d rotation)
    {
        // This is an homogeneous matrix, but it should contain only rotation.
        this.rotation = rotation.withoutTranslation();
    }

    public Matrix4x4d getRotationInverse()
    {
        return rotation_i;
    }

    public void setRotationInverse(Matrix4x4d rotationi)
    {
        this.rotation_i = rotationi;
    }

    public Vector3Dd getPosition()
    {
        return position;
    }

    public void setPosition(Vector3Dd p)
    {
        position = p;
    }

    public Vector3Dd getScale()
    {
        return scale;
    }

    public void setScale(Vector3Dd s)
    {
        scale = s;
    }

    public Matrix4x4d getTransformationMatrix()
    {
        Matrix4x4d S = new Matrix4x4d(), T = new Matrix4x4d(), M;
        S = S.scale(scale);
        T = T.translation(position);
        M = T.multiply(rotation.multiply(S));
        return M;
    }

    /**
    Given a Ray in world coordinates, this method calculates the intersection
    with a Geometry located at the position and with the rotation stored
    in this object. Note that this method only relies in the capability of
    a geometry to calculate an intersection with a ray IN IT'S OWN OBJECT
    SPACE COORDINATES. Note that this technique is a central part of the
    VSDK geometric modeling proposal, where geometric transformations are
    not included in the geometries representations, making the internal
    code of `doIntersection` methods much easier to develop and maintain.
    @param inOutRay
    @return true if given line intersects with any body inside current body
    group
    */
    public Ray doIntersection(Ray inOutRay)
    {
        Ray myRay;
        int i;

        inOutRay = inOutRay.withT(Double.MAX_VALUE);

        myRay = new Ray (
            rotation_i.multiply(inOutRay.getOrigin().subtract(position)),
            rotation_i.multiply(inOutRay.getDirection())
        );
        myRay = myRay.withT(inOutRay.getT());

        Ray nearestHit = null;

        for ( i = 0; i < bodies.size(); i++ ) {
            RayHit hit = new RayHit();
            if ( bodies.get(i).getGeometry().doIntersection(myRay, hit) ) {
                if ( hit.ray().getT() < inOutRay.getT() ) {
                    inOutRay = inOutRay.withT(hit.ray().getT());
                    nearestHit = inOutRay;
                }
            }
        }
        return nearestHit;
    }

    public double[] getMinMax()
    {
        //-----------------------------------------------------------------
        int i;
        SimpleBody bi;
        Matrix4x4d T = new Matrix4x4d(), R, S = new Matrix4x4d(), M;
        Vector3Dd p2;
        ArrayList<Vector3Dd> points = new ArrayList<Vector3Dd>();
        double[] minmaxSub;

        for ( i = 0; i < bodies.size(); i++ ) {
            bi = bodies.get(i);
            minmaxSub = bi.getGeometry().getMinMax();
            R = bi.getRotation();
            T = T.translation(bi.getPosition());
            S = S.scale(bi.getScale()); 
            M = T.multiply(R).multiply(S);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0], minmaxSub[1], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3], minmaxSub[1], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0], minmaxSub[4], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3], minmaxSub[4], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0], minmaxSub[1], minmaxSub[5]));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3], minmaxSub[1], minmaxSub[5]));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0], minmaxSub[4], minmaxSub[5]));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3], minmaxSub[4], minmaxSub[5]));
            points.add(p2);
        }

        //-----------------------------------------------------------------
        double[] MinMax = new double[6];
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
            minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE,
            maxZ = -Double.MAX_VALUE;

        for ( i = 0; i < points.size(); i++ ) {
            Vector3Dd p = points.get(i);
            if ( p.x() < minX ) minX = p.x();
            if ( p.y() < minY ) minY = p.y();
            if ( p.z() < minZ ) minZ = p.z();
            if ( p.x() > maxX ) maxX = p.x();
            if ( p.y() > maxY ) maxY = p.y();
            if ( p.z() > maxZ ) maxZ = p.z();
        }
        MinMax[0] = minX;
        MinMax[1] = minY;
        MinMax[2] = minZ;
        MinMax[3] = maxX;
        MinMax[4] = maxY;
        MinMax[5] = maxZ;

        return MinMax;
    }
}
