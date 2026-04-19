package vsdk.toolkit.environment.scene;
import java.io.Serial;

import java.util.ArrayList;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.Ray;

public class SimpleBodyGroup extends Entity {
    @Serial private static final long serialVersionUID = 20070526L;

    //=======================================================================
    //- Model (1/6): set of bodies ------------------------------------
    private ArrayList <SimpleBody> bodies;

    //- Model (2/6): body geometric transformations -------------------
    private Vector3D position;
    private Vector3D scale;
    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation;
    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation_i;

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
        rotation = new Matrix4x4();
        rotation_i = new Matrix4x4();
        position = new Vector3D(0, 0, 0);
        scale = new Vector3D(1, 1, 1);
    }

    @Override
    public void finalize()
    {
        int i;

        for ( i = 0; i < bodies.size(); i++ ) {
            bodies.set(i, null);
        }
        while ( bodies.size() > 0 ) {
            bodies.remove(0);
        }
        bodies = null;
        position = null;
        scale = null;
        rotation = null;
        rotation_i = null;
        name = null;
        try {
            super.finalize();
        } catch (Throwable ex) {
            
        }
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

    public Matrix4x4 getRotation()
    {
        return rotation;
    }

    public void setRotation(Matrix4x4 rotation)
    {
        // This is an homogeneous matrix, but it should contain only rotation.
        this.rotation = rotation.withoutTranslation();
    }

    public Matrix4x4 getRotationInverse()
    {
        return rotation_i;
    }

    public void setRotationInverse(Matrix4x4 rotationi)
    {
        this.rotation_i = rotationi;
    }

    public Vector3D getPosition()
    {
        return position;
    }

    public void setPosition(Vector3D p)
    {
        position = p;
    }

    public Vector3D getScale()
    {
        return scale;
    }

    public void setScale(Vector3D s)
    {
        scale = s;
    }

    public Matrix4x4 getTransformationMatrix()
    {
        Matrix4x4 S = new Matrix4x4(), T = new Matrix4x4(), M;
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
    public boolean doIntersection(Ray inOutRay)
    {
        Ray myRay;
        boolean answer;
        int i;

        inOutRay.t = Double.MAX_VALUE;

        myRay = new Ray (
            rotation_i.multiply(inOutRay.origin.subtract(position)),
            rotation_i.multiply(inOutRay.direction)
        );
        myRay.t = inOutRay.t;

        answer = false;

        for ( i = 0; i < bodies.size(); i++ ) {
            if ( bodies.get(i).getGeometry().doIntersection(myRay) ) {
                answer = true;
                if ( myRay.t < inOutRay.t ) {
                    inOutRay.t = myRay.t;
                }
            }
        }
        return answer;
    }

    public double[] getMinMax()
    {
        //-----------------------------------------------------------------
        int i;
        SimpleBody bi;
        Matrix4x4 T = new Matrix4x4(), R, S = new Matrix4x4(), M;
        Vector3D p2;
        ArrayList<Vector3D> points = new ArrayList<Vector3D>();
        double[] minmaxSub;

        for ( i = 0; i < bodies.size(); i++ ) {
            bi = bodies.get(i);
            minmaxSub = bi.getGeometry().getMinMax();
            R = bi.getRotation();
            T = T.translation(bi.getPosition());
            S = S.scale(bi.getScale()); 
            M = T.multiply(R).multiply(S);

            p2 = M.multiply(new Vector3D(minmaxSub[0], minmaxSub[1], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3D(minmaxSub[3], minmaxSub[1], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3D(minmaxSub[0], minmaxSub[4], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3D(minmaxSub[3], minmaxSub[4], minmaxSub[2]));
            points.add(p2);

            p2 = M.multiply(new Vector3D(minmaxSub[0], minmaxSub[1], minmaxSub[5]));
            points.add(p2);

            p2 = M.multiply(new Vector3D(minmaxSub[3], minmaxSub[1], minmaxSub[5]));
            points.add(p2);

            p2 = M.multiply(new Vector3D(minmaxSub[0], minmaxSub[4], minmaxSub[5]));
            points.add(p2);

            p2 = M.multiply(new Vector3D(minmaxSub[3], minmaxSub[4], minmaxSub[5]));
            points.add(p2);
        }

        //-----------------------------------------------------------------
        double[] MinMax = new double[6];
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
            minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE,
            maxZ = -Double.MAX_VALUE;

        for ( i = 0; i < points.size(); i++ ) {
            Vector3D p = points.get(i);
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
