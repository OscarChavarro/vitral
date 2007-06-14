//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 9 2005 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Material;

public class RayableObject {
    private Geometry geometry;
    private Vector3D position;

    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation;

    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation_i;

    private Material material;

    public RayableObject()
    {
        geometry = null;
        rotation = null;
        rotation_i = null;
        position = null;
        material = null;
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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
