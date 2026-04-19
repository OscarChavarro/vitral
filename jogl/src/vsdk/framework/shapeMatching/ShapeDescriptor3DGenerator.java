//= Oscar Chavarro, June 16 2007.                                           =
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

package vsdk.framework.shapeMatching;

// Java basic classes
import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL2;

// VSDK Classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.media.FourierShapeDescriptor;
import vsdk.toolkit.media.PrimitiveCountShapeDescriptor;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.processing.SpharmonicKitWrapper;
import vsdk.toolkit.processing.ImageProcessing;
import vsdk.framework.Component;

public class ShapeDescriptor3DGenerator extends Component
{
    /**
    Given a set of possed geometries (with respecto to unit cube), this method
    calculates an ortogonal projection to a newly created distance field image,
    for shape extraction technique described in [FUNK2003].5, and figure
    [FUNK2003].8.

    PRE:
    Current implementation uses hardware accelerated ZBuffer algorithm via
    OpenGL / JOGL, so it needs a working GL context, large enough to contain
    the viewport image to be rendered.
    */
    private IndexedColorImage
    createCube13ProjectedViewDistanceField(GL2 gl, SimpleBodyGroup referenceBodies, int cam, int distanceFieldSide, JoglProjectedViewRenderer projectedViewRenderer)
    {
        //- Execute posing ------------------------------------------------
        SimpleBodyGroup bodySet = calculateUnitCubePosing(referenceBodies);

        //- Render will proceed in a PBuffer ------------------------------
        IndexedColorImage distanceFieldIndexed;

        projectedViewRenderer.configureScene(bodySet, cam);
        projectedViewRenderer.draw(gl);

        //-----------------------------------------------------------------
        distanceFieldIndexed = new IndexedColorImage();
        distanceFieldIndexed.init(distanceFieldSide, distanceFieldSide);
        ImageProcessing.processDistanceFieldWithArray(projectedViewRenderer.image, distanceFieldIndexed, 1);

        //vsdk.toolkit.io.image.ImagePersistence.exportGIF(new java.io.File("outline"+(100+cam)+".gif"), projectedViewRenderer.image);
        //vsdk.toolkit.io.image.ImagePersistence.exportGIF(new java.io.File("distanceField"+(100+cam)+".gif"), distanceFieldIndexed);

        //- Mark unused objects for garbage collection --------------------
        //bodySet = null;
        projectedViewRenderer.configureScene(null, cam);

        //- Obtain Pbuffer's rendered image -------------------------------
        return distanceFieldIndexed;
    }

    /**
    Given a sphere's texture map codified for ocuppancy test, this method
    computes a spherical harmonic (Fourier transform) and inserts the first
    coefficient in to the shape description.
    */
    private boolean
    computeSphericalHarmonicsCoefficients(IndexedColorImage texture,
                           FourierShapeDescriptor fourierShapeDescriptor,
                           int groupIndex, double r)
    {
        byte image[];
        double sphericalHarmonicsR[];
        double sphericalHarmonicsI[];
        int i;

        sphericalHarmonicsR = new double[16];
        sphericalHarmonicsI = new double[16];
        image = texture.getRawImage();

        double hr, hi;
        boolean status = 
        SpharmonicKitWrapper.calculateSphericalHarmonics(
            image, sphericalHarmonicsR, sphericalHarmonicsI);

        if ( !status ) {
            return false;
        }
        else {
            for ( i = 0; i < sphericalHarmonicsR.length; i++ ) {
                hr = sphericalHarmonicsR[i];
                hi = sphericalHarmonicsI[i];
                fourierShapeDescriptor.setFeature(groupIndex, i, hr, hi); 
            }
        }

        return true;
    }

    /**
    Given a voxelized geometry, current method extract from it the
    a (partial) shape descriptor using a spherical harmonic technique.
    This algorithm implements the radial decomposition of a voxel volume
    in to spheres with (texture) maps associated with it, as described in
    [FUNK2003].4.2.
    */
    private boolean processSphericalHarmonics(VoxelVolume vv,
        int groupIndex, FourierShapeDescriptor fourierShapeDescriptor,
        Vector3D cm, double averageDistance)
    {
        // (r, tetha, phi)
        double r = (((double)groupIndex) / 31.0) * (2 * averageDistance);
        double tetha, phi;

        Sphere sphere;
        IndexedColorImage texture;
        int s, t;
        int voxelValue;
        Vector3D p = new Vector3D();

        sphere = new Sphere(r);

        texture = new IndexedColorImage();
        texture.init(64, 64);

        //- Build sphere's texture map from voxel grid --------------------
        for ( s = 0; s < texture.getXSize(); s++ ) {
            for ( t = 0; t < texture.getYSize(); t++ ) {
                tetha =
             ((double)s) / ((double)texture.getXSize()) * Math.PI * 2;
                phi =
             ((double)t) / ((double)texture.getYSize()) * Math.PI;
                p = Vector3D.fromSpherical(r, tetha, phi);
                p = cm.add(p);
                voxelValue = vv.getVoxelAtPosition(p.x(), p.y(), p.z());
                if ( voxelValue < 128 ) {
                    texture.putPixel(s, t, (byte)255);
                }
                else {
                    texture.putPixel(s, t, (byte)0);
                }
            }
        }

        if ( !computeSphericalHarmonicsCoefficients(
                  texture, fourierShapeDescriptor, groupIndex, r) ) {
            return false;
        }

        //-----------------------------------------------------------------
        //vsdk.toolkit.io.image.ImagePersistence.exportJPG(new File("sphere"+(100+groupIndex)+".jpg"), texture);
        return true;
    }

    /**
    Given a set of bodies, this method is responsible of generating a new
    geometric transformation for the set, so the models contained be
    "normalized".
    Note this operation just accounts for scale/translate normalization,
    and leaves untouched rotation. Other methods could normalize also
    rotation.
    This simple method is useful for rotational invariant image shape
    descriptor use.
    */
    public SimpleBodyGroup
    calculateUnitCubePosing(SimpleBodyGroup referenceBodies)
    {
        //- Will render a normalized body inside the unit cube ------------
        SimpleBodyGroup bodySet = new SimpleBodyGroup();
        int i;
        double minmax[];

        Vector3D p;

        //-----------------------------------------------------------------
        minmax = referenceBodies.getMinMax();
        Vector3D min, max, s;
        min = new Vector3D(minmax[0], minmax[1], minmax[2]);
        max = new Vector3D(minmax[3], minmax[4], minmax[5]);
        s = new Vector3D(max.x() - min.x(), max.y() - min.y(), max.z() - min.z());

        double maxsize = s.x();
        if ( s.y() > maxsize ) maxsize = s.y();
        if ( s.z() > maxsize ) maxsize = s.z();
        // The 95% scale factor is to allow a full render of the object to
        // fit inside the rendered view
        s = new Vector3D((2/maxsize) * 0.95, (2/maxsize) * 0.95,
                         (2/maxsize) * 0.95);

        p = max.add(min);
        p = p.multiply(-1/maxsize);

        bodySet.setPosition(p);
        bodySet.setScale(s);

        //-----------------------------------------------------------------
        SimpleBody referenceBody;
        SimpleBody framedBody;

        for ( i = 0; i < referenceBodies.getBodies().size(); i++ ) {
            referenceBody = referenceBodies.getBodies().get(i);
            framedBody = new SimpleBody();
            framedBody.setGeometry(referenceBody.getGeometry());
            framedBody.setPosition(referenceBody.getPosition());
            framedBody.setRotation(referenceBody.getRotation());
            framedBody.setRotationInverse(referenceBody.getRotationInverse());
            framedBody.setMaterial(new Material());
            bodySet.getBodies().add(framedBody);
        }

        return bodySet;
    }

    public PrimitiveCountShapeDescriptor
    calculatePrimitiveCountShapeDescriptor(SimpleBodyGroup referenceBodies, String label)
    {
        PrimitiveCountShapeDescriptor primitiveCountShapeDescriptor;
        primitiveCountShapeDescriptor = new PrimitiveCountShapeDescriptor(label);
        int i, j;
        Geometry referenceGeometry;
        TriangleMesh mesh;
        TriangleMeshGroup meshGroup;
        SimpleBody referenceBody;
        long totalVertices = 0;
        long totalTriangles = 0;

        for ( i = 0; i < referenceBodies.getBodies().size(); i++ ) {
            referenceBody = referenceBodies.getBodies().get(i);
            referenceGeometry = referenceBody.getGeometry();
            if ( referenceGeometry instanceof TriangleMesh ) {
                mesh = (TriangleMesh)referenceGeometry;
                totalVertices += mesh.getNumVertices();
                totalTriangles += mesh.getNumTriangles();
            }
            else if ( referenceGeometry instanceof TriangleMeshGroup ) {
                meshGroup = (TriangleMeshGroup)referenceGeometry;
                for ( j = 0; j < meshGroup.getMeshes().size(); j++ ) {
                    mesh = meshGroup.getMeshes().get(j);
                    totalVertices += mesh.getNumVertices();
                    totalTriangles += mesh.getNumTriangles();
                }
            }
        }
        primitiveCountShapeDescriptor.setFeature(VSDK.POINT, totalVertices);
            
        primitiveCountShapeDescriptor.setFeature(VSDK.TRIANGLE, totalTriangles);
        return primitiveCountShapeDescriptor;
    }

    public FourierShapeDescriptor
    calculateSphericalHarmonicsShapeDescriptor(VoxelVolume vv, String label)
        throws Exception
    {
        FourierShapeDescriptor fourierShapeDescriptor;

        //- Calculate average distance from nonzero voxels to cm ----------
        // This accounts for scale normalization as in [FUNK2003].4.1.
        Vector3D cm; // Center of mass for vv, in VoxelVolume coordinates
        int x, y, z;

        cm = vv.doCenterOfMass();
        int numberOfNonZeroVoxels = 0;
        double d; // Distance between a given voxel and center of mass
        Vector3D p; // Position of voxel
        double averageDistance = 0;

        for ( x = 0; x < vv.getXSize(); x++ ) {
            for ( y = 0; y < vv.getYSize(); y++ ) {
                for ( z = 0; z < vv.getZSize(); z++ ) {
                    if ( vv.getVoxel(x, y, z) != 0 ) {
                        p = vv.getVoxelPosition(x, y, z);
                        averageDistance += VSDK.vectorDistance(cm, p);
                        numberOfNonZeroVoxels++;
                    }
                }
            }
        }
        averageDistance /= (double)numberOfNonZeroVoxels;

        //- Spherical harmonic shape descriptor extraction ------------
        int i;

        fourierShapeDescriptor = new FourierShapeDescriptor(label);
        for ( i = 0; i < 32; i++ ) {
            if ( !processSphericalHarmonics(vv, i, fourierShapeDescriptor, cm,
                      averageDistance) ) {
                throw new Exception ("Error processing spherical harmonics.");
            }
        }
        return fourierShapeDescriptor;
    }

    /**
    For each one of the 13 views as described in [FUNK2003].5, and figure
    [FUNK2003].8. this method executes the following steps:
      - Calculate the distance field for the given view
      - Extracts 32 radial functions from the distance field
      - Computes the first 16 circular harmonics for each circular function
        (trigonometrics decomposition)
      - Adds newly computed shape descriptor to specified `descriptorsList`
    */
    public IndexedColorImage[]
    calculateCube13ProjectedViewsShapeDescriptors(GL2 gl, SimpleBodyGroup referenceBodies, ArrayList<ShapeDescriptor> descriptorsList, int distanceFieldSide, JoglProjectedViewRenderer projectedViewRenderer, boolean exportDistanceFields) throws Exception
    {
        FourierShapeDescriptor fourierShapeDescriptor;
        IndexedColorImage distanceField;
        int i;
        IndexedColorImage array[] = null;

        if ( exportDistanceFields ) {
            array = new IndexedColorImage[13];
        }

        for ( i = 1; i <= 13; i++ ) {
            //- Generate distance field for i-th projection -------------------
            distanceField = createCube13ProjectedViewDistanceField(
                gl, referenceBodies, i, distanceFieldSide,
                projectedViewRenderer);
            if ( distanceField == null ) {
                throw new Exception("Error processing projected texture!");
            }

            //- Calculate Fourier coefficients for radial functions -----------
            ShapeDescriptor2DGenerator component = new ShapeDescriptor2DGenerator();
            fourierShapeDescriptor = component.calculateCircularHarmonicsShapeDescriptor(distanceField, "PROJECTED_VIEW_CUBE_"+i);
            if ( fourierShapeDescriptor == null ) {
                throw new Exception("Error processing projected texture circular shape descriptor!");
            }
            descriptorsList.add(fourierShapeDescriptor);

            //- Mark null reference for garbage collector ---------------------
            if ( exportDistanceFields ) {
                array[i-1] = distanceField;
            }
            else {
                //distanceField = null;
            }
        }
        return array;
    }

}
