//===========================================================================
//= A 3D Search Engine based on Vitral SDK toolkit platform                 =
//=-------------------------------------------------------------------------=
//= Oscar Chavarro, May 23 2007                                             =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
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
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.VoxelVolume;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.metadata.ShapeDescriptorPersistence;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.media.SphericalHarmonicShapeDescriptor;
import vsdk.toolkit.processing.SpharmonicKitWrapper;
import vsdk.toolkit.processing.ImageProcessing;

/**
Current application is the VitralSDK implementation of a search engine for
3d models, as proposed in [FUNK2003].
*/
public class DatasetControl
{
    private static JoglOfflineRenderer offlineRenderer = null;
    private static int distanceFieldSide = 64;
    private static JoglProjectedViewRenderer projectedViewRenderer = null;

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
    a (partial) shape descriptor using a spherical harmonic technique.
    This algorithm implements the radial decomposition of a voxel volume
    in to spheres with (texture) maps associated with it, as described in
    [FUNK2003].4.2.
    */
    private static boolean processSphericalHarmonics(VoxelVolume vv,
        int groupIndex, SphericalHarmonicShapeDescriptor featureVector,
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
                p.setSphericalCoordinates(r, tetha, phi);
                p = cm.add(p);
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
    //ImagePersistence.exportJPG(new File("sphere"+(100+groupIndex)+".jpg"),
        //    texture);
        return true;
    }

    private static Image
    createProjectedView(SimpleBodyGroup referenceBodies, int cam)
    {
        //- Will render a normalized body inside the unit cube ------------
        double minmax[];
        SimpleBodyGroup bodySet = new SimpleBodyGroup();
        int i;

        Vector3D p;
        {
            //-----------------------------------------------------------------
            minmax = referenceBodies.getMinMax();
            Vector3D min, max, s;
            min = new Vector3D(minmax[0], minmax[1], minmax[2]);
            max = new Vector3D(minmax[3], minmax[4], minmax[5]);
            s = new Vector3D(max.x - min.x, max.y - min.y, max.z - min.z);

            double maxsize = s.x;
            if ( s.y > maxsize ) maxsize = s.y;
            if ( s.z > maxsize ) maxsize = s.z;
            // The 95% scale factor is to allow a full render of the object to
            // fit inside the rendered view
            s.x = s.y = s.z = (2/maxsize) * 0.95;

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
            //-----------------------------------------------------------------
        }

        //- Render will proceed in a PBuffer ------------------------------
        IndexedColorImage distanceFieldIndexed;

    if ( projectedViewRenderer == null ) {
            projectedViewRenderer = new JoglProjectedViewRenderer(distanceFieldSide, distanceFieldSide, false);
    }

        projectedViewRenderer.configureScene(bodySet, cam);

    if ( offlineRenderer == null ) {
            offlineRenderer = new JoglOfflineRenderer(distanceFieldSide, distanceFieldSide, projectedViewRenderer);
    }

        //if ( offlineRenderer.isPbufferSupported() ) {
            offlineRenderer.execute();
            offlineRenderer.waitForMe();
        //}
        //else {
        //    this.viewportResizeNeeded = true;
        //    projectedViewRenderer.draw(gl);
        //}

        //-----------------------------------------------------------------
        Image finalImage;
        //finalImage = projectedViewRenderer.image;
        System.out.print("    . Processing projected view " + cam + "... ");
        distanceFieldIndexed = new IndexedColorImage();
        distanceFieldIndexed.init(distanceFieldSide, distanceFieldSide);
        ImageProcessing.processDistanceFieldWithArray(projectedViewRenderer.image, distanceFieldIndexed, 1);
        ImageProcessing.gammaCorrection(distanceFieldIndexed, 2.0);

        RGBAImage distanceFieldRgba;
        distanceFieldRgba = distanceFieldIndexed.exportToRgbaImage();
        finalImage = distanceFieldRgba;
        System.out.println("Ok!");

        //vsdk.toolkit.io.image.ImagePersistence.exportPPM(new java.io.File("./test" + cam + ".ppm"), finalImage);

        //- Obtain Pbuffer's rendered image -------------------------------
        return finalImage;
    }

    private static void
    processProjectedViews(SimpleBodyGroup referenceBodies)
    {
        Image texture;
    int i;

        for ( i = 1; i <= 13; i++ ) {
            texture = createProjectedView(referenceBodies, i);
            if ( texture == null ) {
        System.err.println("Error processing projected texture!");
                System.exit(1);
            }
        }
    }

    /**
    This 3D model indexing method implements the analysis technique presented
    in [FUNK2003].4.
    */
    public static GeometryMetadata analyzeModel(String filename)
    {
        //- Variables -----------------------------------------------------
        ArrayList<SimpleBody> things;
        int i, R;
        VoxelVolume vv;
        Geometry referenceGeometry;
        Vector3D cm; // Center of mass for vv, in VoxelVolume coordinates
    int x, y, z;

        vv = new VoxelVolume();
        // Spherical harmonic bandwitdh config. as defined at [FUNK2003].4.1.
        R = 32;
        // Voxel grid config. as defined at [FUNK2003].4.1.
        vv.init(2*R, 2*R, 2*R);

        GeometryMetadata metadata = new GeometryMetadata();
        things = new ArrayList<SimpleBody>();
        System.out.println("Analyzing model " + filename);
        try {
            File fd = new File(filename);
            EnvironmentPersistence.importEnvironment(fd,
                things, null, null, null);
            System.out.println("  - Processing " + things.size() + " geometries...");
            for ( i = 0; i < things.size(); i++ ) {
                referenceGeometry = things.get(i).getGeometry();
                System.out.println("    . Voxelizing a " +
                    referenceGeometry.getClass().getName());

                //- Calculate transform matrix --------------------------------
                double minmax[] = referenceGeometry.getMinMax();
                // Transform from voxelspace to geometry minmax space
                Matrix4x4 M;
                M = VoxelVolume.getTransformFromVoxelFrameToMinMax(minmax);

                //- Primitive rasterization ([FUNK2003].4.1.) -----------------
                ProgressMonitorConsole reporter = new ProgressMonitorConsole();
                referenceGeometry.doVoxelization(vv, M, reporter);
            }

            //- Calculate average distance from nonzero voxels to cm ----------
            // This accounts for scale normalization as in [FUNK2003].4.1.
            System.out.print("    . Processing spherical harmonics ... ");

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
            SphericalHarmonicShapeDescriptor featureVector;
            featureVector = new SphericalHarmonicShapeDescriptor();
            for ( i = 0; i < 32; i++ ) {
                if ( !processSphericalHarmonics(vv, i, featureVector, cm,
                          averageDistance) ) {
                    System.err.println("Error processing spherical harmonics.");
                    return null;
                }
            }
            metadata.getDescriptors().add(featureVector);
        System.out.println("Ok!");

            //- Projection views image descriptor extraction --------------
            SimpleBodyGroup bodySet;
            SimpleBody referenceBody;

            bodySet = new SimpleBodyGroup();
            for ( i = 0; i < things.size(); i++ ) {
                referenceBody = things.get(i);
                bodySet.getBodies().add(referenceBody);
            }

            processProjectedViews(bodySet);
        }
        catch ( Exception e ) {
            System.err.println("ERROR: Can not open file " + filename);
            return null;
        }
        metadata.setFilename(filename);
        return metadata;
    }

    /**
    This method implements the matching step as described in [FUNK2003].3.2.
    @todo: at a negative tolerance, this method should not use "tolerance" but
    return the -tolerance closest models. For example, for an equal behavior
    to the search engine described in [FUNK2003], this value should be -16.0.
    */
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
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
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

    /**
    This method implements the indexing step as described in [FUNK2003].3.2.
    */
    public static void indexFiles(String filenamesList[],
        ArrayList<GeometryMetadata> descriptorsArray)
    {
        int i, j;
        GeometryMetadata m, other;

        for ( i = 1; i < filenamesList.length; i++ ) {
            // If that model was before in database, delete it
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
            indexFiles(args, descriptorsArray);
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
