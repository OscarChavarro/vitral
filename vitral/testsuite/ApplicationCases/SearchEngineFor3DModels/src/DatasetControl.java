//===========================================================================

// Java basic classes
import java.io.File;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;

// VSDK Classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.VoxelVolume;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.SphericalHarmonicShapeDescriptor;
import vsdk.toolkit.processing.SpharmonicKitWrapper;

public class DatasetControl
{
    /**
    Given a sphere's texture map codified for ocuppancy test, this method
    computes a spherical harmonic (Fourier transform) and inserts the first
    coefficient in to the shape description.
    */
    private static boolean
    computeSphericalHarmonicsCoefficients(IndexedColorImage texture,
                           SphericalHarmonicShapeDescriptor featureVector,
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
                featureVector.setFeature(groupIndex, i, hr, hi); 
            }
        }

        return true;
    }

    /**
    Given a voxelized geometry, current method extract from it the
    shape descriptor using a spherical harmonic technique.
    */
    private static boolean processSphericalHarmonics(VoxelVolume vv,
        int groupIndex, SphericalHarmonicShapeDescriptor featureVector)
    {
        double r = ((double)groupIndex) / 31.0;
        Sphere sphere;
        IndexedColorImage texture;
        double tetha, phi;
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
                p.setSphericalCoordinates(r, tetha, phi);
                voxelValue = vv.getVoxelAtPosition(p.x, p.y, p.z);
                if ( voxelValue < 128 ) {
                    texture.putPixel(s, t, (byte)255);
                }
                else {
                    texture.putPixel(s, t, (byte)0);
                }
            }
        }

        if ( !computeSphericalHarmonicsCoefficients(
                  texture, featureVector, groupIndex, r) ) {
            return false;
        }

        //-----------------------------------------------------------------
        return true;
    }

    public static void addModel(String filename)
    {
        ArrayList<SimpleBody> things = new ArrayList<SimpleBody>();
        int i;
        VoxelVolume vv = new VoxelVolume();
        vv.init(64, 64, 64);
        Geometry referenceGeometry;

        System.out.println("Adding model " + filename);
        try {
            File fd = new File(filename);
            EnvironmentPersistence.importEnvironment(fd,
                things, null, null, null);
            System.out.println("  - Processing " + things.size() + " geometries...");
            for ( i = 0; i < things.size(); i++ ) {
                referenceGeometry = things.get(i).getGeometry();
                System.out.println("    . " +
                    referenceGeometry.getClass().getName());

                //- Calculate transform matrix --------------------------------
                double minmax[] = referenceGeometry.getMinMax();
                // Transform from voxelspace to geometry minmax space
                Matrix4x4 M;
                M = VoxelVolume.getTransformFromVoxelFrameToMinMax(minmax);

                //- Primitive rasterization -----------------------------------
                ProgressMonitorConsole reporter = new ProgressMonitorConsole();
                referenceGeometry.doVoxelization(vv, M, reporter);
            }

            //- Spherical harmonic shape descriptor extraction ------------
            SphericalHarmonicShapeDescriptor featureVector;
            featureVector = new SphericalHarmonicShapeDescriptor();
            for ( i = 0; i < 32; i++ ) {
                if ( !processSphericalHarmonics(vv, i, featureVector) ) {
                    System.err.println("Error processing spherical harmonics.");
                    System.exit(1);
                }
            }
            System.out.println(featureVector);
        }
        catch ( Exception e ) {
            System.err.println("ERROR: Can not open file " + filename);
            return;
        }
    }

    public static void searchModel(String filename)
    {
        System.out.println("Searching for closest 3D model to " + filename);
    }

    public static void main(String args[])
    {
        int i;
        if ( args.length < 1 ) {
            System.err.println("Usage: java DatasetControl command files...");
            System.err.println("Commands are: add, searchModel");
            return;
        }
        if ( args[0].equals("add") ) {
            // Import shape descriptions from file

            // Add new model descriptions
            for ( i = 1; i < args.length; i++ ) {
                addModel(args[i]);
            }

            // Export shape descriptions to file
        }
        else if ( args[0].equals("searchModel") ) {
            searchModel(args[1]);
        }
        else {
            System.err.println("Invalid command " + args[0]);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
