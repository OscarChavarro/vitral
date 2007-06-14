//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 26 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.environment.scene;

import java.util.ArrayList;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Ray;

public class SimpleBodyGroup extends Entity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20070526L;

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
        name = new String(n);
    }

    public Matrix4x4 getRotation()
    {
        return rotation;
    }

    public void setRotation(Matrix4x4 rotation)
    {
        this.rotation = rotation;

        // This is an homogeneus matrix, but its supposed to contain only
        // the rotation component.
        this.rotation.M[0][3] = 0;
        this.rotation.M[1][3] = 0;
        this.rotation.M[2][3] = 0;
        this.rotation.M[3][3] = 1;
        this.rotation.M[3][0] = 0;
        this.rotation.M[3][1] = 0;
        this.rotation.M[3][2] = 0;
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

    /**
    Given a Ray in world coordinates, this method calculates the intersection
    with a Geometry located at the position and with the rotation stored
    in this object. Note that this method only relies in the capability of
    a geometry to calculate an intersection with a ray IN IT'S OWN OBJECT
    SPACE COORDINATES. Note that this technique is a central part of the
    VSDK geometric modeling proposal, where geometric transformations are
    not included in the geometries representations, making the internal
    code of `doIntersection` methods much easier to develop and maintain.
    */
    public boolean doIntersection(Ray inOutRay)
    {
        Ray myRay;
        boolean answer;
    int i;

    inOutRay.t = Double.MAX_VALUE;

        myRay = new Ray (
            rotation_i.multiply(inOutRay.origin.substract(position)),
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
    int i;
        double[] MinMax = new double[6];
        boolean first = true;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
            minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE,
            maxZ = Double.MIN_VALUE;
    Vector3D min = new Vector3D();
    Vector3D max = new Vector3D();
    SimpleBody bi;
    Matrix4x4 T = new Matrix4x4(), R, S = new Matrix4x4(), M;

        for ( i = 0; i < bodies.size(); i++ ) {
        bi = bodies.get(i);
            double[] minmax_mesh = bi.getGeometry().getMinMax();
            R = bi.getRotation();
            T.translation(bi.getPosition());
            S.scale(bi.getScale()); 
        M = S.multiply(R).multiply(T);

            min.x = minmax_mesh[0];
            min.y = minmax_mesh[1];
            min.z = minmax_mesh[2];
            max.x = minmax_mesh[3];
            max.y = minmax_mesh[4];
            max.z = minmax_mesh[5];

            min = M.multiply(min);
            max = M.multiply(max);

            if (first) {
                minX = min.x;
                maxX = max.x;
                minY = min.y;
                maxY = max.y;
                minZ = min.z;
                maxZ = max.z;
                first = false;
            }

            if (min.x < minX) {
                minX = min.x;
            }
            if (min.y < minY) {
                minY = min.y;
            }
            if (min.z < minZ) {
                minZ = min.z;
            }
            if (max.x > maxX) {
                maxX = max.x;
            }
            if (max.y > maxY) {
                maxY = max.y;
            }
            if (max.z > maxZ) {
                maxZ = max.z;
            }
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

//===========================================================================
//= EOF                                                                     =
//===========================================================================
