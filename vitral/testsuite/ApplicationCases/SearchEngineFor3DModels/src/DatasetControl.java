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

// Awt / swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
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
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.media.SphericalHarmonicShapeDescriptor;
import vsdk.toolkit.processing.SpharmonicKitWrapper;
import vsdk.toolkit.processing.ImageProcessing;

/**
Current application is the VitralSDK implementation of a search engine for
3d models, as proposed in [FUNK2003].
*/
public class DatasetControl implements GLEventListener
{
    private JoglOfflineRenderer offlineRenderer = null;
    private int distanceFieldSide = 64;
    private JoglProjectedViewRenderer projectedViewRenderer = null;
    private String args[];
    private GLCanvas canvas;
    private IndexedColorImage radialFunction;

    private DatasetControl(String args[])
    {
        this.args = args;
        radialFunction = new IndexedColorImage();
        radialFunction.init(64, 64);

        projectedViewRenderer = new JoglProjectedViewRenderer(distanceFieldSide, distanceFieldSide, false);
        offlineRenderer = new JoglOfflineRenderer(distanceFieldSide, distanceFieldSide, projectedViewRenderer);
    }

    /**
    Given a sphere's texture map codified for ocuppancy test, this method
    computes a spherical harmonic (Fourier transform) and inserts the first
    coefficient in to the shape description.
    */
    private boolean
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
    private boolean processSphericalHarmonics(VoxelVolume vv,
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
        //vsdk.toolkit.io.image.ImagePersistence.exportJPG(new File("sphere"+(100+groupIndex)+".jpg"), texture);
        return true;
    }

    private Image
    createProjectedView(GL gl, SimpleBodyGroup referenceBodies, int cam)
    {
        //- Will render a normalized body inside the unit cube ------------
        double minmax[];
        SimpleBodyGroup bodySet = new SimpleBodyGroup();
        int i;

        Vector3D p;

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

        //- Render will proceed in a PBuffer ------------------------------
        IndexedColorImage distanceFieldIndexed;

        projectedViewRenderer.configureScene(bodySet, cam);
        if ( offlineRenderer.isPbufferSupported() ) {
            offlineRenderer.execute();
            offlineRenderer.waitForMe();
        }
        else {
            projectedViewRenderer.draw(gl);
            canvas.swapBuffers();
        }

        //-----------------------------------------------------------------
        Image finalImage;
        //finalImage = projectedViewRenderer.image;
        System.out.print("    . Processing projected view " + cam + "... ");
        distanceFieldIndexed = new IndexedColorImage();
        distanceFieldIndexed.init(distanceFieldSide, distanceFieldSide);
        ImageProcessing.processDistanceFieldWithArray(projectedViewRenderer.image, distanceFieldIndexed, 1);
        ImageProcessing.gammaCorrection(distanceFieldIndexed, 2.0);

        finalImage = distanceFieldIndexed;
        System.out.println("Ok!");

        //vsdk.toolkit.io.image.ImagePersistence.exportPPM(new java.io.File("./test" + cam + ".ppm"), finalImage);

        //- Obtain Pbuffer's rendered image -------------------------------
        return finalImage;
    }

    /**
    For each one of the 13 views as described in [FUNK2003].5, and figure
    [FUNK2003].8. this method executes the following steps:
      - Calculate the distance field for the given view
      - Extracts 32 radial functions from the distance field
      - Computes the first 16 harmonics for each radial function
        (trigonometrics decomposition)
    */
    private boolean
    processProjectedViews(GL gl, SimpleBodyGroup referenceBodies,
        ArrayList<ShapeDescriptor> descriptorsList)
    {
        Image distanceField;
        int i;

        SphericalHarmonicShapeDescriptor featureVector;

        for ( i = 1; i <= 13; i++ ) {
            //- Generate distance field for i-th projection -------------------
            distanceField = createProjectedView(gl, referenceBodies, i);
            if ( distanceField == null ) {
                System.err.println("Error processing projected texture!");
                System.exit(1);
            }

            //- Code to debug distance field to radial functions ... ----------
            /*
            int x, y;

            for ( x = 0; x < 64; x++ ) {
                for ( y = 0; y < 64; y++ ) {
                    val = 100;
                    if ( x < 32 && y < 32 ) {
                        val = 0;
                    }
                    else if ( x >= 32 && y < 32 ) {
                        val = 255/3;
                    }
                    else if ( x < 32 && y >= 32 ) {
                        val = 255/2;
                    }
                    else if ( x >= 32 && y >= 32 ) {
                        val = 255;
                    }
                    //r = Math.sqrt((32.0-(double)x)*(32.0-(double)x)+(32.0-(double)y)*(32.0-(double)y));
                    //val = 255 - (int)(r * (255.0/32.0));
                    ((IndexedColorImage)distanceField).putPixel(x, y, VSDK.unsigned8BitInteger2signedByte(val));
                }
            }
            */
            //vsdk.toolkit.io.image.ImagePersistence.exportPPM(new java.io.File("./test" + i + ".ppm"), distanceField);

            //- Calculate Fourier coefficients for radial functions -----------
            featureVector = analyzeImageHarmonics(distanceField, i);
            if ( featureVector == null ) {
                return false;
            }
            descriptorsList.add(featureVector);

        }
        return true;
    }

    private SphericalHarmonicShapeDescriptor analyzeImageHarmonics(Image distanceField, int i)
    {
        SphericalHarmonicShapeDescriptor featureVector;
        featureVector = new SphericalHarmonicShapeDescriptor("PROJECTED_VIEW_" + i);
        double hi, hr;
        int j, k, val;
        double sphericalHarmonicsR[] = new double[16];
        double sphericalHarmonicsI[] = new double[16];
        boolean status;
        ColorRgb c;
        double r, u, v, tetha;

        for ( j = 0; j < 32; j++ ) {
            // r varies from 0 to 0.5
            r = (((double)j)/(((double)distanceFieldSide)));
            for ( k = 0; k < 64; k++ ) {
                tetha = 2*Math.PI * ((double)k) / 64.0;
                u = 0.5 + r * Math.cos(tetha);
                v = 0.5 - r * Math.sin(tetha);
                c = distanceField.getColorRgbBiLinear(u, v);
                val = (int)(((c.r + c.g + c.b)/3.0) * 255.0);
                radialFunction.putPixel(k, 32, VSDK.unsigned8BitInteger2signedByte(val));
            }
            status = SpharmonicKitWrapper.calculateSphericalHarmonics(
                radialFunction.getRawImage(), sphericalHarmonicsR, sphericalHarmonicsI);
             if ( !status ) {
                return null;
            }
            else {
                for ( k = 0; k < sphericalHarmonicsR.length; k++ ) {
                    hr = sphericalHarmonicsR[k];
                    hi = sphericalHarmonicsI[k];
                    featureVector.setFeature(j, k, hr, hi); 
                }
            }

            //ImagePersistence.exportPPM(new File("radialF"+(100+i)+":"+(100+j)+".ppm"), radialFunction);
        }
        //ImagePersistence.exportPPM(new File("field"+(100+i)+".ppm"), distanceField);

        return featureVector;
    }

    /**
    This 3D model indexing method implements the analysis technique presented
    in [FUNK2003].4.
    */
    public GeometryMetadata analyzeModel(GL gl, String filename, boolean withProjection)
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
            featureVector = new SphericalHarmonicShapeDescriptor("SPHERICAL_HARMONIC_3D");
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
            for ( i = 0; i < things.size() && withProjection; i++ ) {
                referenceBody = things.get(i);
                bodySet.getBodies().add(referenceBody);
            }

            processProjectedViews(gl, bodySet, metadata.getDescriptors());
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
    private ArrayList <String> matchModel(
        GL gl,
        String filename,
        ArrayList<GeometryMetadata> descriptorsArray,
        double tolerance)
    {
        GeometryMetadata m = analyzeModel(gl, filename, false);
        int i;
        double Ls;

        ArrayList <String> results = new ArrayList <String>();
        System.out.println("Searching for closest 3D model to " + filename);

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            Ls = m.doMinskowskiDistance(descriptorsArray.get(i), 2, "SPHERICAL_HARMONIC_3D");
            System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
                " to " + descriptorsArray.get(i).getFilename());
            if ( Ls < tolerance ) {
                results.add(descriptorsArray.get(i).getFilename());
            }
        }

        return results;
    }

    private ArrayList <String> matchSketch(
        GL gl,
        String imageFilename,
        ArrayList<GeometryMetadata> descriptorsArray,
        double tolerance)
    {
        int i, j;
        double Ls;

        ArrayList <String> results = new ArrayList <String>();
        System.out.println("Searching for sketches from image " + imageFilename);

        RGBImage img = null;
        IndexedColorImage distanceField = new IndexedColorImage();
        try {
            img = ImagePersistence.importRGB(new File(imageFilename));
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            return results;
        }

        if ( img.getXSize() != 64 || img.getYSize() != 64 ) {
            return results;
        }
        distanceField.init(64, 64);
        int x, y, val;
        RGBPixel p;
        for ( x = 0; x < img.getXSize(); x++ ) {
            for ( y = 0; y < img.getYSize(); y++ ) {
                p = img.getPixel(x, y);
                val = ((VSDK.signedByte2unsignedInteger(p.r)) +
                       (VSDK.signedByte2unsignedInteger(p.g)) +
                       (VSDK.signedByte2unsignedInteger(p.b))) / 3;
                distanceField.putPixel(x, y,
                    VSDK.unsigned8BitInteger2signedByte(val));
            }
        }

        SphericalHarmonicShapeDescriptor featureVector;
        featureVector = analyzeImageHarmonics(distanceField, 0);
        if ( featureVector == null ) {
            return results;
        }
        GeometryMetadata metadata = new GeometryMetadata();
        metadata.getDescriptors().add(featureVector);

        String name;
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            for ( j = 1; j <= 13; j++ ) {
                name = "PROJECTED_VIEW_" + j;
                featureVector.setLabel(name);
                Ls = metadata.doMinskowskiDistance(descriptorsArray.get(i), 2, name);
                System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
                    " to " + descriptorsArray.get(i).getFilename() + " : " + j);
                if ( Ls < tolerance ) {
                    results.add(descriptorsArray.get(i).getFilename() + ", view: " + j + " (" + Ls + ")");
                }
            }
        }

        return results;
    }

    private void
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
                m = ShapeDescriptorPersistence.importGeometryMetadata(reader);
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

    private void
    saveDatabase(ArrayList<GeometryMetadata> descriptorsArray)
    {
        GeometryMetadata m;
        int i;

        File fd = new File("etc/metadata.bin");
        FileOutputStream fos;
        BufferedOutputStream writer;

        try {
            fos = new FileOutputStream(fd);
            writer = new BufferedOutputStream(fos);
            for ( i = 0; i < descriptorsArray.size(); i++ ) {
                m = descriptorsArray.get(i);
                ShapeDescriptorPersistence.exportGeometryMetadata(writer, m);
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
    public void indexFiles(GL gl, String filenamesList[],
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
            m = analyzeModel(gl, filenamesList[i], true);
            if ( m != null ) {
                descriptorsArray.add(m);
            }
        }
    }

    public void runApplication(GL gl)
    {
        ArrayList<GeometryMetadata> descriptorsArray;
        descriptorsArray = new ArrayList<GeometryMetadata>();

        if ( args.length < 1 ) {
            System.err.println("Usage: java DatasetControl command files...");
            System.err.println("Commands are: add, searchModel, searchSketch");
            System.exit(0);
        }
        if ( args[0].equals("add") ) {
            readDatabase(descriptorsArray);
            indexFiles(gl, args, descriptorsArray);
            saveDatabase(descriptorsArray);
        }
        else if ( args[0].equals("searchModel") ) {
            ArrayList <String> similarModels;
            readDatabase(descriptorsArray);
            similarModels = matchModel(gl, args[1], descriptorsArray, 1);
            System.out.println("Founded " + similarModels.size() +
                " similar objects:");
            for ( int i = 0; i < similarModels.size(); i++ ) {
                System.out.println("  - " + similarModels.get(i));
            }
        }
        else if ( args[0].equals("searchSketch") ) {
            ArrayList <String> similarModels;
            readDatabase(descriptorsArray);
            similarModels = matchSketch(gl, args[1], descriptorsArray, 0.05);
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

    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }

    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height); 
    }   

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        runApplication(gl);
        System.exit(1);
    }

    public static void main(String args[])
    {
        DatasetControl instance = new DatasetControl(args);
        if ( instance.offlineRenderer.isPbufferSupported() ||
             (args.length > 0 && !args[0].equals("add")) ) {
            instance.runApplication(null);
        }
        else {
            if ( args.length > 0 && args[0].equals("add") ) {
                // Create application based GUI
                JFrame frame;
                Dimension size;

                instance.canvas = new GLCanvas();
                instance.canvas.addGLEventListener(instance);
                frame = new JFrame("VITRAL offline renderer window - do not hide");
                frame.add(instance.canvas, BorderLayout.CENTER);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                size = new Dimension(instance.distanceFieldSide*2, instance.distanceFieldSide*2);
                frame.setMinimumSize(size);
                frame.setSize(size);
                frame.setVisible(true);
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
