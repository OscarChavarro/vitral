//===========================================================================

// Java basic classes
import java.io.File;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import vsdk.toolkit.io.metadata.ShapeDescriptorPersistence;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.ShapeDescriptor;
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

    public static GeometryMetadata analyzeModel(String filename)
    {
        ArrayList<SimpleBody> things = new ArrayList<SimpleBody>();
        int i;
        VoxelVolume vv = new VoxelVolume();
        vv.init(64, 64, 64);
        Geometry referenceGeometry;
        GeometryMetadata metadata = new GeometryMetadata();

        System.out.println("Analizing model " + filename);
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
                    return null;
                }
            }
            metadata.getDescriptors().add(featureVector);
        }
        catch ( Exception e ) {
            System.err.println("ERROR: Can not open file " + filename);
            return null;
        }
        metadata.setFilename(filename);
        return metadata;
    }

    private static ArrayList <String> matchModel(
        String filename,
        ArrayList<GeometryMetadata> descriptorsArray,
        double tolerance)
    {
        GeometryMetadata m = analyzeModel(filename);
        int i;
        double Ls;

        ArrayList <String> results = new ArrayList <String>();
        System.out.println("Searching for closest 3D model to " + filename);

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            // Takes into consideration the euclidean distance
            Ls = m.doMinskowskiDistance(descriptorsArray.get(i), 2);
            System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
                " to " + descriptorsArray.get(i).getFilename());
            if ( Ls < tolerance ) {
                results.add(descriptorsArray.get(i).getFilename());
            }
        }

        return results;
    }

    /**
    Returns null if gets error... can also return Exception
    */
    private static GeometryMetadata
    readEntry(BufferedInputStream reader) throws Exception {
        GeometryMetadata m;
        byte subChunkId;
        byte chunkId;
        int bytesToSkip;
        double vector[];
        int i;
        ShapeDescriptor shapeDescriptor;

        chunkId = ShapeDescriptorPersistence.importByte(reader);
        switch ( chunkId ) {
          case ShapeDescriptorPersistence.TYPE_STRING:
            m = new GeometryMetadata();
            m.setFilename(ShapeDescriptorPersistence.readAsciiString(reader));
            do {
                subChunkId = ShapeDescriptorPersistence.importByte(reader);
                switch( subChunkId ) {
                  case ShapeDescriptorPersistence.TYPE_ENDING:
                    break;
                  case ShapeDescriptorPersistence.TYPE_SPHERICAL_HARMONIC:
                    bytesToSkip = ShapeDescriptorPersistence.readIntBE(reader);
                    vector = new double[bytesToSkip/4];
                    for ( i = 0; i < vector.length; i++ ) {
                        vector[i] =
                            ShapeDescriptorPersistence.readFloatBE(reader);
                    }
                    shapeDescriptor = new SphericalHarmonicShapeDescriptor();
                    shapeDescriptor.setFeatureVector(vector);
                    m.getDescriptors().add(shapeDescriptor);
                    reader.skip(bytesToSkip - vector.length*4);
                    break;
                  default:
                    bytesToSkip = ShapeDescriptorPersistence.readIntBE(reader);
                    System.out.println("Skipping bytes: " + bytesToSkip);
                    reader.skip(bytesToSkip);
                    break;
                }
            } while ( subChunkId != ShapeDescriptorPersistence.TYPE_ENDING );
            break;
          default:
            System.err.println("ERROR importing database (wrong format " +
                chunkId + ")!");
            return null;
        }
        return m;
    }

    private static void
    readDatabase(ArrayList<GeometryMetadata> descriptorsArray)
    {
        File fd = new File("etc/metadata.bin");
        FileInputStream fis;
        BufferedInputStream reader;
        GeometryMetadata m;

        try {
            fis = new FileInputStream(fd);
            reader = new BufferedInputStream(fis);

            while ( reader.available() > 0 ) {
                m = readEntry(reader);
                if ( m != null ) {
                    descriptorsArray.add(m);
                }
            }
            reader.close();
            fis.close();
        }
        catch ( Exception e ) {
            if ( !(e instanceof FileNotFoundException) ) {
                System.err.println("ERROR importing database!" + e);
            }
        }
    }

    private static void
    saveDatabase(ArrayList<GeometryMetadata> descriptorsArray)
    {
        GeometryMetadata m;
        int i;

        File fd = new File("etc/metadata.bin");
        FileOutputStream fos;
        BufferedOutputStream writer;
        ArrayList<ShapeDescriptor> list;

        try {
            fos = new FileOutputStream(fd);
            writer = new BufferedOutputStream(fos);
            for ( i = 0; i < descriptorsArray.size(); i++ ) {
                m = descriptorsArray.get(i);
                ShapeDescriptorPersistence.exportByte(writer, 
                    ShapeDescriptorPersistence.TYPE_STRING);
                ShapeDescriptorPersistence.writeAsciiString(
                    writer, m.getFilename());
                list = m.getDescriptors();
                ShapeDescriptorPersistence.exportDescriptorMetadata(
                    writer, list);
                ShapeDescriptorPersistence.exportEnding(writer);
            }
            writer.flush();
            writer.close();
            fos.close();
        }
        catch ( Exception e ) {
            System.err.println("ERROR exporting database!");
        }
    }

    public static void addFiles(String filenamesList[],
        ArrayList<GeometryMetadata> descriptorsArray)
    {
        int i, j;
        GeometryMetadata m, other;

        // Add new model descriptions
        for ( i = 1; i < filenamesList.length; i++ ) {
            // If that model was before in database, delete
            for ( j = 0; j < descriptorsArray.size(); j++ ) {
                other = descriptorsArray.get(j);
                if ( other.getFilename().equals(filenamesList[i]) ) {
                    descriptorsArray.remove(j);
                    j--;
                    break;
                }
            }

            // Insert new model in database
            m = analyzeModel(filenamesList[i]);
            if ( m != null ) {
                descriptorsArray.add(m);
            }
        }
    }

    public static void main(String args[])
    {
        ArrayList<GeometryMetadata> descriptorsArray;
        descriptorsArray = new ArrayList<GeometryMetadata>();

        if ( args.length < 1 ) {
            System.err.println("Usage: java DatasetControl command files...");
            System.err.println("Commands are: add, searchModel");
            return;
        }
        if ( args[0].equals("add") ) {
            readDatabase(descriptorsArray);
            addFiles(args, descriptorsArray);
            saveDatabase(descriptorsArray);
        }
        else if ( args[0].equals("searchModel") ) {
            ArrayList <String> similarModels;
            readDatabase(descriptorsArray);
            similarModels = matchModel(args[1], descriptorsArray, 1);
            System.out.println("Founded " + similarModels.size() +
                " similar objects:");
            for ( int i = 0; i < similarModels.size(); i++ ) {
                System.out.println("  - " + similarModels.get(i));
            }
        }
        else {
            System.err.println("Invalid command " + args[0]);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
