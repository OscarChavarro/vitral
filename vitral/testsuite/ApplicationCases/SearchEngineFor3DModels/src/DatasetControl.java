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
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.SphericalHarmonicShapeDescriptor;

public class DatasetControl
{
    private static Process runOperatingSystemCommand(String command) 
    {
        Process process;
        Runtime runtime;

        runtime = Runtime.getRuntime();
        try {
            process = runtime.exec(command);
        }
        catch( Exception error ){
            System.out.println(error);
            process = null;
        }
        return process;
    }

    private static boolean processSphericalHarmonics(VoxelVolume vv,
        int groupIndex, SphericalHarmonicShapeDescriptor featureVector)
    {
        double r = ((double)groupIndex) / 31.0;
        Sphere sphere;
        RGBAImage texture;
        double tetha, phi;
        int s, t;
        int voxelValue;
        Vector3D p = new Vector3D();

        sphere = new Sphere(r);

        texture = new RGBAImage();
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
                    texture.putPixel(s, t, (byte)255, (byte)255, (byte)255, (byte)0);
                }
                else {
                    texture.putPixel(s, t, (byte)0, (byte)0, (byte)0, (byte)255);
                }
            }
        }

        //- Generate spherical harmonics for current sphere's texture map -
        String inputFilename = "input_" + VSDK.formatDouble(r) + ".ppm";
        String outputFilename = "output_" + VSDK.formatDouble(r);
        ImagePersistence.exportPPM(new File(inputFilename), texture);
        runOperatingSystemCommand("../../../pkgs/SpharmonicKit27/bin/generateSphericalHarmonics " + inputFilename);
        runOperatingSystemCommand("mv output.ppm " + outputFilename + ".ppm");
        runOperatingSystemCommand("mv output.txt " + outputFilename + ".txt");
        runOperatingSystemCommand("sync");
    //try{Thread.sleep(100);}catch ( Exception e ) {}
        
        //-----------------------------------------------------------------
        BufferedReader br;
        String lineOfText;
        int h; // harmonic
        double hr; // real part of harmonic amplitude
        double hi; // imaginary part of harmonic amplitude
        StringTokenizer auxStringTokenizer;

        try {
            br = new BufferedReader(new FileReader(outputFilename + ".txt"));
            for ( h = 0; h < 16 && (lineOfText = br.readLine()) != null; h++ ) {
                auxStringTokenizer = new StringTokenizer(lineOfText, ", ");
                hr = Double.parseDouble(auxStringTokenizer.nextToken());
                hi = Double.parseDouble(auxStringTokenizer.nextToken());
                featureVector.setFeature(groupIndex, h, hr, hi); 
            }
            br.close();
        }
        catch (Exception e) {
            System.out.println("Retrying incomplete files at " + groupIndex);
            return false;
        }

        //-----------------------------------------------------------------
        runOperatingSystemCommand("rm -f " + outputFilename + ".ppm");
        runOperatingSystemCommand("rm -f " + outputFilename + ".txt");
        runOperatingSystemCommand("rm -f " + inputFilename);

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
                while ( !processSphericalHarmonics(vv, i, featureVector) );
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
            for ( i = 1; i < args.length; i++ ) {
                addModel(args[i]);
            }
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
