//===========================================================================
//= A 3D Search Engine based on Vitral SDK toolkit platform                 =
//=-------------------------------------------------------------------------=
//= Oscar Chavarro, May 23 2007                                             =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003] Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,   =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//= [MIN2003] Min, Patrick. Halderman, John A. Kazhdan, Michael.            =
//=     Funkhouser, Thoimas A. "Early Experiences with a 3D Model Search    =
//=     Engine".                                                            =
//===========================================================================

// Java basic classes
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

// VSDK Classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.VoxelVolume;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.metadata.ShapeDescriptorPersistence;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.FourierShapeDescriptor;
import vsdk.toolkit.media.PrimitiveCountShapeDescriptor;
import vsdk.framework.shapeMatching.ShapeDescriptor2DGenerator;
import vsdk.framework.shapeMatching.ShapeDescriptor3DGenerator;
import vsdk.framework.shapeMatching.JoglShapeMatchingOfflineRenderer;
import vsdk.framework.shapeMatching.JoglProjectedViewRenderer;

public class SearchEngine
{
    /**
    This 3D model indexing method implements the analysis technique presented
    in [FUNK2003].4.
    */
    private GeometryMetadata analyzeModel(GL gl, String filename, boolean withProjection, boolean withPreviews, int distanceFieldSide, GLCanvas canvas, JoglShapeMatchingOfflineRenderer offlineRenderer, JoglProjectedViewRenderer projectedViewRenderer, HashMap<String, TimeReport> times)
    {
        //- Variables -----------------------------------------------------
        ArrayList<SimpleBody> things;
        int i, R;
        VoxelVolume vv;
        Geometry referenceGeometry;
        SimpleBodyGroup bodySet;

        vv = new VoxelVolume();
        // Spherical harmonic bandwitdh config. as defined at [FUNK2003].4.1.
        R = 32;
        // Voxel grid config. as defined at [FUNK2003].4.1.
        vv.init(2*R, 2*R, 2*R);

        GeometryMetadata metadata = new GeometryMetadata();
        things = new ArrayList<SimpleBody>();
        System.out.println("Analyzing model id [" + metadata.getId() + "], filename: " + filename);
        try {
            //- Load scene from specified size and configure its body group ---
            times.get("READ_MODEL").start();
            File fd = new File(filename);
            System.out.print("  - Reading file... ");
            EnvironmentPersistence.importEnvironment(fd,
                things, null, null, null);
            System.out.println("Ok.");

            bodySet = new SimpleBodyGroup();
            for ( i = 0; i < things.size(); i++ ) {
                bodySet.getBodies().add(things.get(i));
            }
            if ( bodySet.getBodies().size() < 1 ) {
                System.err.println("Warning: no geometries found inside " + filename + ", skipping shape analysis.");
                return null;
            }
            times.get("READ_MODEL").stop();
            System.out.println("  - Processing " + bodySet.getBodies().size() +
                " geometries:");

            ShapeDescriptor3DGenerator component = new ShapeDescriptor3DGenerator();

            //- Calculate spherical harmonics shape descriptor ----------------
            System.out.print("    . Primitive count shape descriptor ... ");
            times.get("PRIMITIVE_COUNT").start();
            PrimitiveCountShapeDescriptor primitiveCountShapeDescriptor;
            primitiveCountShapeDescriptor =
                component.calculatePrimitiveCountShapeDescriptor(bodySet,
                    "PRIMITIVE_COUNT");
            metadata.getDescriptors().add(primitiveCountShapeDescriptor);
            times.get("PRIMITIVE_COUNT").stop();
            System.out.println("Ok.");

            //- Prepare voxelized version of scene for descriptors requiring it
            times.get("VOXELIZE").start();
            for ( i = 0; i < bodySet.getBodies().size(); i++ ) {
                referenceGeometry = bodySet.getBodies().get(i).getGeometry();
                System.out.print("    . Voxelizing a " +
                    referenceGeometry.getClass().getName() + " ... ");

                //- Calculate transform matrix --------------------------------
                double minmax[] = referenceGeometry.getMinMax();
                // Transform from voxelspace to geometry minmax space
                Matrix4x4 M;
                M = VoxelVolume.getTransformFromVoxelFrameToMinMax(minmax);

                //- Primitive rasterization ([FUNK2003].4.1.) -----------------
                ProgressMonitorConsole reporter = new ProgressMonitorConsole();
                referenceGeometry.doVoxelization(vv, M, reporter);

                System.out.println("Ok.");

                // Keep reference clean for garbage collector
                referenceGeometry = null;
            }
            times.get("VOXELIZE").stop();

            //- Calculate spherical harmonics shape descriptor ----------------
            System.out.print("    . Spherical harmonics shape descriptor for voxelized geometry ... ");
            times.get("SPHERICAL_HARMONICS").start();
            FourierShapeDescriptor fourierShapeDescriptor;
            fourierShapeDescriptor =
                component.calculateSphericalHarmonicsShapeDescriptor(vv,
                    "SPHERICAL_HARMONIC_3D");
            metadata.getDescriptors().add(fourierShapeDescriptor);
            times.get("SPHERICAL_HARMONICS").stop();
            System.out.println("Ok.");

            //- Projection views image descriptor extraction [FUNK2003] ---
            if ( withProjection ) {
                System.out.print("    . Projected views image descriptors (cube 13 setup) ... ");
                times.get("CUBE13_PROJECTIONS").start();
                component.calculateCube13ProjectedViewsShapeDescriptors(gl, bodySet, metadata.getDescriptors(), distanceFieldSide, projectedViewRenderer, offlineRenderer);
                times.get("CUBE13_PROJECTIONS").stop();
                System.out.println("Ok.");
            }

            //- Image preview generation [MIN2003] ------------------------
            JoglPreviewGenerator component2 = new JoglPreviewGenerator();
            if ( withPreviews ) {
                System.out.print("    . Previews ... ");
                times.get("PREVIEWS").start();
                System.out.print(" for model " + metadata.getId() + " ... ");

                GLCanvas mycanvas = null;
                if ( !offlineRenderer.isPbufferSupported() ) {
                    mycanvas = canvas;
                }

                component2.calculatePreviews(gl, bodySet, metadata.getId(), 640, 480, mycanvas);

                times.get("PREVIEWS").stop();
                System.out.println("Ok.");
            }

            //-----------------------------------------------------------------
            if ( (withProjection || withPreviews ) && !offlineRenderer.isPbufferSupported() ) {
                canvas.swapBuffers();
            }

            //- Free memory (unlink references for garbage collector gathering)
            for ( i = 0; i < things.size(); i++ ) {
                things.set(i, null);
            }
            while ( things.size() > 0 ) {
                things.remove(0);
            }
            bodySet = null;
        }
        catch ( Exception e ) {
            System.err.println("ERROR: Can not analyze file " + filename);
            e.printStackTrace();
            return null;
        }
        metadata.setFilename(filename);
        things = null;

        return metadata;
    }

    /**
    This method implements the matching step as described in [FUNK2003].3.2.
    @todo: at a negative tolerance, this method should not use "tolerance" but
    return the -tolerance closest models. For example, for an equal behavior
    to the search engine described in [FUNK2003], this value should be -16.0.
    */
    public ArrayList <Result> matchModel(
        GL gl,
        String filename,
        ArrayList<GeometryMetadata> descriptorsArray,
        double tolerance, int distanceFieldSide, GLCanvas canvas,
        JoglShapeMatchingOfflineRenderer offlineRenderer,
        JoglProjectedViewRenderer projectedViewRenderer,
        HashMap<String, TimeReport> times)
    {
        ArrayList <Result> results = new ArrayList <Result>();
        GeometryMetadata m = analyzeModel(gl, filename, false, false, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer, times);

        if ( m == null ) {
            System.err.println("Error analyzing reference model. No results reported.");
            return results;
        }

        int i;
        double Ls;

        Result result;
        System.out.println("Searching for closest 3D model to " + filename);

        times.get("SEARCH_MODEL").start();

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            Ls = m.doMinskowskiDistance(descriptorsArray.get(i), 2, "SPHERICAL_HARMONIC_3D");
            //System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
            //    " to " + descriptorsArray.get(i).getFilename());
            if ( Ls < tolerance ) {
                result = new Result(descriptorsArray.get(i).getFilename(), Ls);
                results.add(result);
            }
        }

        times.get("SEARCH_MODEL").stop();

        return results;
    }

    public ArrayList <Result> matchSketch(
        GL gl,
        IndexedColorImage distanceField,
        ArrayList<GeometryMetadata> descriptorsArray,
        double tolerance, HashMap<String, TimeReport> times)
    {
        int i, j;
        double Ls;

        ArrayList <Result> results = new ArrayList <Result>();
        Result result;

        ShapeDescriptor2DGenerator component = new ShapeDescriptor2DGenerator();
        FourierShapeDescriptor fourierShapeDescriptor;
        fourierShapeDescriptor = component.calculateCircularHarmonicsShapeDescriptor(distanceField, "PROJECTED_VIEW_CUBE_0");
        if ( fourierShapeDescriptor == null ) {
            return results;
        }
        GeometryMetadata metadata = new GeometryMetadata();
        metadata.getDescriptors().add(fourierShapeDescriptor);

        String name;

        times.get("SEARCH_MODEL").start();

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            for ( j = 1; j <= 13; j++ ) {
                name = "PROJECTED_VIEW_CUBE_" + j;
                fourierShapeDescriptor.setLabel(name);
                Ls = metadata.doMinskowskiDistance(descriptorsArray.get(i), 2, name);
                //System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
                //    " to " + descriptorsArray.get(i).getFilename() + " : " + j);
                if ( Ls < tolerance ) {
                    result = new Result(descriptorsArray.get(i).getFilename() + ", view: " + j, Ls);
                    results.add(result);
                }
            }
        }

        times.get("SEARCH_MODEL").stop();

        return results;
    }

    public void
    readDatabase(ArrayList<GeometryMetadata> descriptorsArray, HashMap<String, TimeReport> times)
    {
        File fd = new File("etc/metadata.bin");
        FileInputStream fis;
        BufferedInputStream reader;
        GeometryMetadata m;

        System.out.print("Reading database ... ");
        times.get("READ_DATABASE").start();

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
        times.get("READ_DATABASE").stop();
        System.out.println(descriptorsArray.size() + " entries, Ok. ");
    }

    public void
    saveDatabase(ArrayList<GeometryMetadata> descriptorsArray, HashMap<String, TimeReport> times)
    {
        GeometryMetadata m;
        int i;

        System.out.print("Writing database ... ");
        times.get("WRITE_DATABASE").start();

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
        times.get("WRITE_DATABASE").stop();

        System.out.println(descriptorsArray.size() + " entries, Ok. ");
    }

    /**
    This method implements the indexing step as described in [FUNK2003].3.2.
    */
    public void indexFiles(GL gl, String filenamesList[],
                           ArrayList<GeometryMetadata> descriptorsArray,
                           int distanceFieldSide, GLCanvas canvas,
                           JoglShapeMatchingOfflineRenderer offlineRenderer,
                           JoglProjectedViewRenderer projectedViewRenderer,
                           HashMap<String, TimeReport> times)
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
            m = analyzeModel(gl, filenamesList[i], true, true, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer, times);
            if ( m != null ) {
                descriptorsArray.add(m);
            }
            System.gc();
        }
    }

    public IndexedColorImage
    readIndexedColorImage(String imageFilename, int distanceFieldSide)
    {
        System.out.println("Searching for sketches from image (distance field) " + imageFilename);
        RGBImage img = null;
        IndexedColorImage distanceField = new IndexedColorImage();
        try {
            img = ImagePersistence.importRGB(new File(imageFilename));
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            return null;
        }

        if ( img.getXSize() != distanceFieldSide ||
             img.getYSize() != distanceFieldSide ) {
            return null;
        }

        distanceField.init(distanceFieldSide, distanceFieldSide);
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

        return distanceField;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
