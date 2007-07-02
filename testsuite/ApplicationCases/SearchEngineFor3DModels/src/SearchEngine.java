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
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

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
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.metadata.ShapeDescriptorPersistence;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.FourierShapeDescriptor;
import vsdk.toolkit.media.PrimitiveCountShapeDescriptor;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.processing.ImageProcessing;
import vsdk.framework.shapeMatching.ShapeDescriptor2DGenerator;
import vsdk.framework.shapeMatching.ShapeDescriptor3DGenerator;
import vsdk.framework.shapeMatching.JoglShapeMatchingOfflineRenderer;
import vsdk.framework.shapeMatching.JoglProjectedViewRenderer;

/**
Class `SearchEngine` stablishes a Fachade design pattern role in which any
application can ask for high level functional operations of a Vitral based
search engine for 3D models.

Vitral based search engine for 3D models follows the functional operations
ideas of other systems, like the one proposed in [FUNK2003] and [MIN2003].
*/
public class SearchEngine
{
    private HashMap<String, TimeReport> timers;

    private int searchLastSlash(String cad)
    {
        int i;
        for ( i = cad.length()-1; i > 0; i-- ) {
            if ( cad.charAt(i) == '/' ) {
                return i+1;
            }
        }
        return 0;
    }

    /**
    Search for a result with the given id inside the list. If not found
    returns null.
    */
    private Result searchResult(ArrayList <Result> results, long id)
    {
        int i;
        Result r;

        for ( i = 0; i < results.size(); i++ ) {
            r = results.get(i);
            if ( r.getId() == id ) {
                return r;
            }
        }
        return null;
    }

    public GeometryMetadata searchMetadataById(ArrayList<GeometryMetadata> descriptorsArray, long id)
    {
        GeometryMetadata ri;

        int i;
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            ri = descriptorsArray.get(i);
            if ( ri.getId() == id ) {
                return ri;
            }
        }
        return null;
    }

    /**
    This 3D model indexing method implements the analysis technique presented
    in [FUNK2003].4.
    */
    private GeometryMetadata analyzeModel(GL gl, String filename, boolean withProjection, boolean withPreviews, int distanceFieldSide, GLCanvas canvas, JoglShapeMatchingOfflineRenderer offlineRenderer, JoglProjectedViewRenderer projectedViewRenderer)
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
            timers.get("READ_MODEL").start();
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
            timers.get("READ_MODEL").stop();
            System.out.println("  - Processing " + bodySet.getBodies().size() +
                " geometries:");

            ShapeDescriptor3DGenerator component = new ShapeDescriptor3DGenerator();

            //- Calculate spherical harmonics shape descriptor ----------------
            System.out.print("    . Primitive count shape descriptor ... ");
            timers.get("PRIMITIVE_COUNT").start();
            PrimitiveCountShapeDescriptor primitiveCountShapeDescriptor;
            primitiveCountShapeDescriptor =
                component.calculatePrimitiveCountShapeDescriptor(bodySet,
                    "PRIMITIVE_COUNT");
            metadata.getDescriptors().add(primitiveCountShapeDescriptor);
            timers.get("PRIMITIVE_COUNT").stop();
            System.out.println("Ok.");

            //- Prepare voxelized version of scene for descriptors requiring it
            timers.get("VOXELIZE").start();
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
            timers.get("VOXELIZE").stop();

            //- Calculate spherical harmonics shape descriptor ----------------
            System.out.print("    . Spherical harmonics shape descriptor for voxelized geometry ... ");
            timers.get("SPHERICAL_HARMONICS").start();
            FourierShapeDescriptor fourierShapeDescriptor;
            fourierShapeDescriptor =
                component.calculateSphericalHarmonicsShapeDescriptor(vv,
                    "SPHERICAL_HARMONIC_3D");
            metadata.getDescriptors().add(fourierShapeDescriptor);
            timers.get("SPHERICAL_HARMONICS").stop();
            System.out.println("Ok.");

            //- Projection views image descriptor extraction [FUNK2003] ---
            if ( withProjection ) {
                System.out.print("    . Projected views image descriptors (cube 13 setup) ... ");
                timers.get("CUBE13_PROJECTIONS").start();
                component.calculateCube13ProjectedViewsShapeDescriptors(gl, bodySet, metadata.getDescriptors(), distanceFieldSide, projectedViewRenderer);
                timers.get("CUBE13_PROJECTIONS").stop();
                System.out.println("Ok.");
            }

            //- Image preview generation [MIN2003] ------------------------
            JoglPreviewGenerator component2 = new JoglPreviewGenerator();
            if ( withPreviews ) {
                System.out.print("    . Previews ... ");
                timers.get("PREVIEWS").start();
                GLCanvas mycanvas = null;
                if ( !offlineRenderer.isPbufferSupported() ) {
                    mycanvas = canvas;
                }

                component2.calculatePreviews(gl, component.calculateUnitCubePosing(bodySet), metadata.getId(), 640, 480, mycanvas);

                timers.get("PREVIEWS").stop();
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

    public SearchEngine()
    {
        timers = createTimers();
    }

    public HashMap<String, TimeReport> getTimers()
    {
        return timers;
    }

    private HashMap<String, TimeReport> createTimers()
    {
        HashMap<String, TimeReport> timers;
        timers = new HashMap<String, TimeReport>();
        timers.put("TOTAL", new TimeReport("TOTAL"));
        timers.put("VOXELIZE", new TimeReport("VOXELIZE"));
        timers.put("READ_MODEL", new TimeReport("READ_MODEL"));
        timers.put("READ_DATABASE", new TimeReport("READ_DATABASE"));
        timers.put("WRITE_DATABASE", new TimeReport("WRITE_DATABASE"));
        timers.put("SEARCH_MODEL", new TimeReport("SEARCH_MODEL"));
        timers.put("SPHERICAL_HARMONICS", new TimeReport("SPHERICAL_HARMONICS"));
        timers.put("PRIMITIVE_COUNT", new TimeReport("PRIMITIVE_COUNT"));
        timers.put("CUBE13_PROJECTIONS", new TimeReport("CUBE13_PROJECTIONS"));
        timers.put("PREVIEWS", new TimeReport("PREVIEWS"));
        return timers;
    }

    /**
    This method implements the matching step as described in [FUNK2003].3.2.
    @todo: at a negative tolerance, this method should not use "tolerance" but
    return the -tolerance closest models. For example, for an equal behavior
    to the search engine described in [FUNK2003], this value should be -16.0.
    */
    public ArrayList <Result> matchModelSphericalHarmonics(
        String filename,
        ArrayList<GeometryMetadata> descriptorsArray,
        double tolerance, int distanceFieldSide)
    {
        ArrayList <Result> results = new ArrayList <Result>();
        GeometryMetadata m = analyzeModel(null, filename, false, false, distanceFieldSide, null, null, null);
        GeometryMetadata n;

        if ( m == null ) {
            System.err.println("Error analyzing reference model. No results reported.");
            return results;
        }

        int i;
        double Ls;

        Result result;
        System.out.println("Searching for closest 3D model to " + filename);

        timers.get("SEARCH_MODEL").start();

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            Ls = m.doMinskowskiDistance(descriptorsArray.get(i), 2, "SPHERICAL_HARMONIC_3D");
            //System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
            //    " to " + descriptorsArray.get(i).getFilename());
            if ( Ls < tolerance ) {
                n = descriptorsArray.get(i);
                result = searchResult(results, n.getId());
                if ( result == null ) {
                    result = new Result(n.getFilename(), n.getId(),
                        new ResultSource(ResultSource.SPHERICAL_HARMONIC, Ls));
                    result.setDescriptor(n);
                    results.add(result);
                }
                else {
                    result.addSource(new ResultSource(ResultSource.SPHERICAL_HARMONIC, Ls));
                }
            }
        }

        timers.get("SEARCH_MODEL").stop();

        return results;
    }

    public ArrayList <Result> matchSketch(
        IndexedColorImage distanceField,
        ArrayList<GeometryMetadata> descriptorsArray,
        double tolerance)
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
        GeometryMetadata n;
        GeometryMetadata descriptor;

        metadata.getDescriptors().add(fourierShapeDescriptor);

        String name;

        timers.get("SEARCH_MODEL").start();

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptor = descriptorsArray.get(i);
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            for ( j = 1; j <= 13; j++ ) {
                name = "PROJECTED_VIEW_CUBE_" + j;
                fourierShapeDescriptor.setLabel(name);
                Ls = metadata.doMinskowskiDistance(descriptor, 2, name);
                //System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
                //    " to " + descriptor.getFilename() + " : " + j);
                if ( Ls < tolerance ) {
                    n = descriptor;
                    result = searchResult(results, n.getId());
                    if ( result == null ) {
                        result = new Result(n.getFilename(), n.getId(),
                            new ResultSource(ResultSource.CUBE13VIEW+j, Ls));
                        result.setDescriptor(n);
                        results.add(result);
                    }
                    else {
                        result.addSource(
                            new ResultSource(ResultSource.CUBE13VIEW+j, Ls));
                    }
                }
            }
        }

        timers.get("SEARCH_MODEL").stop();

        return results;
    }

    public ArrayList <Result> matchSketchRestricted(
        IndexedColorImage distanceField,
        double tolerance, ArrayList <Result> prevSearch)
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
        GeometryMetadata n;
        GeometryMetadata descriptor;

        metadata.getDescriptors().add(fourierShapeDescriptor);

        String name;

        timers.get("SEARCH_MODEL").start();

        for ( i = 0; i < prevSearch.size(); i++ ) {
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            descriptor = prevSearch.get(i).getDescriptor();
            for ( j = 1; descriptor != null && j <= 13; j++ ) {
                name = "PROJECTED_VIEW_CUBE_" + j;
                fourierShapeDescriptor.setLabel(name);
                Ls = metadata.doMinskowskiDistance(descriptor, 2, name);
                //System.out.println(" - Distance " + VSDK.formatDouble(Ls) +
                //    " to " + descriptor.getFilename() + " : " + j);
                if ( Ls < tolerance ) {
                    n = descriptor;
                    result = searchResult(results, n.getId());
                    if ( result == null ) {
                        result = new Result(n.getFilename(), n.getId(),
                            new ResultSource(ResultSource.CUBE13VIEW+j, Ls));
                        results.add(result);
                    }
                    else {
                        result.addSource(
                            new ResultSource(ResultSource.CUBE13VIEW+j, Ls));
                    }
                }
            }
        }

        timers.get("SEARCH_MODEL").stop();

        return results;
    }

    public void
    readDatabase(ShapeDatabase database)
    {
        timers.get("READ_DATABASE").start();
        database.connect();
        timers.get("READ_DATABASE").stop();
    }

    public void
    saveDatabase(ArrayList<GeometryMetadata> descriptorsArray)
    {
        GeometryMetadata m;
        int i;

        System.out.print("Writing database ... ");
        timers.get("WRITE_DATABASE").start();

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
        timers.get("WRITE_DATABASE").stop();

        System.out.println(descriptorsArray.size() + " entries, Ok. ");
    }

    /**
    This method implements the indexing step as described in [FUNK2003].3.2.
    */
    public void indexFiles(GL gl, String filenamesList[],
                           ArrayList<GeometryMetadata> descriptorsArray,
                           int distanceFieldSide, GLCanvas canvas,
                           JoglShapeMatchingOfflineRenderer offlineRenderer,
                           JoglProjectedViewRenderer projectedViewRenderer)
    {
        int i, j;
        GeometryMetadata m, other;

        if ( gl == null || offlineRenderer == null ||
             projectedViewRenderer == null ) {
            VSDK.reportMessage(this, VSDK.WARNING,
                               "indexFiles",
            "Graphical contexts needed for indexing models not available.\n" +
            "Aborting file indexing.");
            return;
        }

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
            m = analyzeModel(gl, filenamesList[i], true, true, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer);
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

    public void writeResultsAsHtml(PrintWriter out, ArrayList <Result> similarModels, ArrayList<GeometryMetadata> descriptorsArray, String dir)
    {
        out.write("<HTML>\n");
        out.write("<H1>VITRAL BASED 3D MODEL SEARCH ENGINE</H1>\n");
        out.write("<B>Search results in database [test]</B>, " + descriptorsArray.size() + " models &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; <FONT SIZE=\"-1\">(click on a thumbnail for more information on that model)</FONT><P>\n");
        out.write("<FONT SIZE=\"-1\">results: " + similarModels.size() + "</FONT>\n");

        out.write("<CENTER>\n");
        out.write("<TABLE BORDER=0 WIDTH=100% CELLPADDING=3 CELLSPACING=3>\n");

        int i;
        GeometryMetadata data;
        DecimalFormat f1 = new DecimalFormat("0000000");
        DecimalFormat f2 = new DecimalFormat("00");
        long modelId;
        String filename, fullpath;

        for ( i = 0; i < similarModels.size(); i++ ) {
            modelId = similarModels.get(i).getId();

            if ( i % 4 == 0 ) {
                out.write("<TR>\n");
            }

            out.write("<TD WIDTH=25% VALIGN=CENTER ALIGN=CENTER>\n");

            data = searchMetadataById(descriptorsArray, modelId);
            if ( data != null ) {
                out.write("<A HREF=\"ServletConsole?input=model_detail&id=" + modelId + "\">\n");
                out.write("<IMG BORDER=1 SRC=\"" + dir + "/previews/" + f1.format(modelId, new StringBuffer(""), new FieldPosition(0)).toString() + "/" + f2.format(0, new StringBuffer(""), new FieldPosition(0)).toString() + "small.jpg" + "\"></IMG>\n");
            }
            else {
                out.write("<B>No metadata descriptor for ID " + similarModels.get(i).getId() + "\n");
            }

            fullpath = similarModels.get(i).getFilename().toString();
            filename = fullpath.substring(searchLastSlash(fullpath));
            out.write("</A><BR>"+ (i+1) + ". &nbsp;<B>" + filename + "</B>");
            out.write("<BR>Distance: "
                      + VSDK.formatDouble(similarModels.get(i).getDistance()) +
                      "</TD>\n");

            if ( i % 4 == 3 ) {
                out.write("</TR>\n");
            }
        }
        if ( i % 4 != 0 ) {
            out.write("</TR>\n");
        }

        out.write("</TABLE>\n");
        out.write("</CENTER>\n");

        out.write("This page was automatically generated by VITRAL\n");
        out.write("More information available at <A HREF=\"http://sophia.javeriana.edu.co/~ochavarr\">http://sophia.javeriana.edu.co/~ochavarr</A>\n");
        out.write("</HTML>\n");
    }

    public void reportTimers()
    {
        int i;
        TimeReport r;
        double totalTime = 0;
        double totalSum = 0;

        System.out.println("= TIMERS REPORT ===========================================================");
        System.out.println("Following a report of different stage timers for program operations:");
        Set<String> s = timers.keySet();
        for ( String e : s ) {
            r = timers.get(e);
            if ( r.getLabel().equals("TOTAL") ) {
                totalTime = r.getTime();
            }
            else {
                System.out.println("  - " + r);
                totalSum += r.getTime();
            }
        }
        System.out.println("  - " + timers.get("TOTAL"));
        System.out.printf("  - Other timers (%f total - %f summed): %f\n",
                          totalTime, totalSum, totalTime - totalSum);
        System.out.println("Other timers stand for:");
        System.out.println("  - Garbage collection");
        System.out.println("  - Errors in time metrics");
        System.out.println("  - Control operations");
        System.out.println("  - Time used for console to scroll text");
        System.out.println("  - Time used for redirected console to save log files");
        System.out.println("Note that current figures stands for ELLAPSED (no CPU) times (java limitation)");
    }

    public ArrayList <Result> runCommand(
        GL gl,
        JoglShapeMatchingOfflineRenderer offlineRenderer,
        GLCanvas canvas,
        JoglProjectedViewRenderer projectedViewRenderer,
        String command[],
        ArrayList<GeometryMetadata> descriptorsArray,
        int distanceFieldSide)
    {
        ArrayList <Result> similarModels = null;
        int i, j, k;

        timers.get("TOTAL").start();
        //-----------------------------------------------------------------
        if ( command[0].equals("add") ) {
            System.out.println("Processing command add...");
            indexFiles(gl, command, descriptorsArray, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer);
            saveDatabase(descriptorsArray);
        }
        //-----------------------------------------------------------------
        else if ( command[0].equals("addList") ) {
            String list[] = null;
            try {
                File fd;
                FileReader fr;
                BufferedInputStream bis;
                BufferedReader reader;
                InputStreamReader isr;
                int tokenType;
                i = 0;

                //- Count filenames ------------------------------------------
                int count = 0;
                fd = new File(command[1]);
                fr = new FileReader(fd);
                reader = new BufferedReader(fr);
                String line;

                do {
                    line = reader.readLine();
                    if ( line == null ) break;
                    count++;
                } while ( true );

                fr.close();

                //- Load filenames -------------------------------------------
                System.out.println("Processing " + count + " files.");
                list = new String[count];

                fd = new File(command[1]);
                fr = new FileReader(fd);
                reader = new BufferedReader(fr);
                i = 0;

                do {
                    line = reader.readLine();
                    if ( line == null ) break;
                    list[i] = new String(line);
                    i++;
                } while ( true );
                fr.close();
            }
            catch ( Exception e ) {
                System.err.println("Error building file list.");
                e.printStackTrace();
                System.exit(1);
            }
            indexFiles(gl, list, descriptorsArray, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer);
            saveDatabase(descriptorsArray);
        }
        //-----------------------------------------------------------------
        else if ( command[0].equals("searchModel") ) {
            similarModels = matchModelSphericalHarmonics(command[1], descriptorsArray, 1, distanceFieldSide);
        }
        //-----------------------------------------------------------------
        else if ( command[0].equals("searchDistanceField") ) {
            IndexedColorImage distanceField = readIndexedColorImage(command[1], distanceFieldSide);
            if ( distanceField == null ) {
                System.err.println("Error importing distance field. Program aborted.");
                System.exit(1);
            }
            similarModels = matchSketch(distanceField, descriptorsArray, 5);
        }
        //-----------------------------------------------------------------
        else if ( command[0].equals("searchSketch") ) {
            IndexedColorImage outline = readIndexedColorImage(command[1], distanceFieldSide);
            if ( outline == null ) {
                System.err.println("Error importing distance field. Program aborted.");
                System.exit(1);
            }
            IndexedColorImage distanceField;
            distanceField = new IndexedColorImage();
            distanceField.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(outline, distanceField, 1);
            similarModels = matchSketch(distanceField, descriptorsArray, 5);
        }
        //-----------------------------------------------------------------
        else if ( command[0].equals("exportDatabase") ) {
            if ( command.length != 2 ) {
                System.err.println("ERROR: exportDatabase command must specified a text filename to export.");
                System.err.println("Database exporting aborted.");
            }
            else {
                System.out.println("Processing command exportDatabase ...");
                timers.get("WRITE_DATABASE").start();
                System.out.println("Exporting database with " + descriptorsArray.size() + " fields!");
                System.out.println("Depending on database size, this could take some time...");
                GeometryMetadata m;
                ArrayList<ShapeDescriptor> d;
                ShapeDescriptor s;
                double arr[];
                String stringSegment;

                try {
                    //---------------------------------------------------------
                    File fd = new File(command[1]);
                    FileOutputStream fos;
                    BufferedOutputStream writer;

                    fd = new File(command[1]);
                    fos = new FileOutputStream(fd);
                    writer = new BufferedOutputStream(fos);

                    //---------------------------------------------------------
                    byte barr[];
                    ProgressMonitorConsole reporter;
                    reporter = new ProgressMonitorConsole();

                    reporter.begin();
                    for ( i = 0; i < descriptorsArray.size(); i++ ) {
                        m = descriptorsArray.get(i);
                        d = m.getDescriptors();
                        stringSegment = "[" + m.getId() + "]\t" + m.getFilename() + "\t";
                        for ( j = 0; j < d.size(); j++ ) {
                            s = d.get(j);
                            stringSegment += s.getLabel() + "\t";
                            barr = stringSegment.getBytes();
                            writer.write(barr, 0, barr.length);

                            arr = s.getFeatureVector();
                            for ( k = 0; k < arr.length; k++ ) {
                                stringSegment = arr[k] + "\t";
                                barr = stringSegment.getBytes();
                                writer.write(barr, 0, barr.length);
                            }
                        }
                        stringSegment = "EOL\n";
                        barr = stringSegment.getBytes();
                        writer.write(barr, 0, barr.length);

                        reporter.update(0, descriptorsArray.size(), i);

                    }
                    reporter.end();

                    writer.close();
                    fos.close();
                }
                catch ( Exception e ) {
                    System.out.println("Error exporting database!");
                }
                timers.get("WRITE_DATABASE").stop();
                System.out.println("Done exporting database.");
            }
        }
        //-----------------------------------------------------------------
        else {
            System.err.println("Invalid command [" + command[0] + "]");
        }

        //-----------------------------------------------------------------
        timers.get("TOTAL").stop();
        return similarModels;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
