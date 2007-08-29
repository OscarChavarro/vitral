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
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;

public class SimpleBody extends Entity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    //=======================================================================
    //- Model (1/6): body form ----------------------------------------
    private Geometry geometry;

    //- Model (2/6): body geometric transformations -------------------
    private Vector3D position;
    private Vector3D scale;
    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation;
    /// Warning: The translation value in this matrix must be <0, 0, 0>
    private Matrix4x4 rotation_i;

    //- Model (3/6): body visual data ---------------------------------
    private Material globalMaterial;
    private Image globalTextureMap;
    private NormalMap globalNormalMap;

    //- Model (4/6): body physical data -------------------------------

    //- Model (5/6): body structural relationships --------------------

    //- Model (6/6): body semantic data -------------------------------
    /// This string should be used for specific application defined
    /// functionality. Can be null.
    private String name;
    //=======================================================================

    public SimpleBody()
    {
        geometry = null;
        rotation = new Matrix4x4();
        rotation_i = new Matrix4x4();
        position = new Vector3D(0, 0, 0);
        scale = new Vector3D(1, 1, 1);
        globalMaterial = new Material();
        globalTextureMap = null;
        globalNormalMap = null;
    }

    public void finalize()
    {
        geometry = null;
        position = null;
        scale = null;
        rotation = null;
        rotation_i = null;
        globalMaterial = null;
        globalTextureMap = null;
        globalNormalMap = null;
        name = null;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String n)
    {
        name = new String(n);
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
        return globalMaterial;
    }

    public void setMaterial(Material m)
    {
        globalMaterial = m;
    }

    public Image getTexture()
    {
        return globalTextureMap;
    }

    public void setTexture(Image in)
    {
        globalTextureMap = in;
    }

    public NormalMap getNormalMap()
    {
        return globalNormalMap;
    }

    public void setNormalMap(NormalMap in)
    {
        globalNormalMap = in;
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

    public Matrix4x4 getTransformationMatrix()
    {
        Matrix4x4 S = new Matrix4x4(), T = new Matrix4x4(), M;
        S.scale(scale);
        T.translation(position);
        M = T.multiply(rotation.multiply(S));
        return M;
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

    /**
    WARNING: Check if this method works ok for modified geometric operations!
    */
    public void doExtraInformation(Ray inRay, double inT, GeometryIntersectionInformation outInfo)
    {
        Vector3D po;
        Matrix4x4 R, Ri;
	Ray myRay;

        po = getPosition();
        R = getRotation();
        Ri = getRotationInverse();
        myRay = new Ray ( 
            Ri.multiply(inRay.origin.substract(po) ),
            Ri.multiply(inRay.direction)
        );
        myRay.t = inRay.t;
        geometry.doExtraInformation(myRay, inT, outInfo);

        outInfo.p = R.multiply(outInfo.p).add(po);
        outInfo.n = R.multiply(outInfo.n);

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
