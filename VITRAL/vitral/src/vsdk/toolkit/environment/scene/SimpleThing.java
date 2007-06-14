//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 9 2005 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.environment.scene;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Geometry;

public class SimpleThing extends Entity {
    private Geometry geometry;
    private Vector3D position;
    private Vector3D scale;

    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation;

    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation_i;

    private Material material;

    public SimpleThing()
    {
        geometry = null;
        rotation = new Matrix4x4();
        rotation_i = new Matrix4x4();
        position = new Vector3D(0, 0, 0);
        scale = new Vector3D(1, 1, 1);
        material = new Material();
    }

    public Geometry getGeometry()
    {
        return geometry;
    }

    public void setGeometry(Geometry g)
    {
        geometry = g;
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

    public Material getMaterial()
    {
        return material;
    }

    public void setMaterial(Material m)
    {
        material = m;
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

        myRay = new Ray (
            rotation_i.multiply(inOutRay.origin.substract(position)),
            rotation_i.multiply(inOutRay.direction)
        );
        myRay.t = inOutRay.t;

        answer = false;
        if ( geometry.doIntersection(myRay) ) {
            answer = true;
            inOutRay.t = myRay.t;
        }

        return answer;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
