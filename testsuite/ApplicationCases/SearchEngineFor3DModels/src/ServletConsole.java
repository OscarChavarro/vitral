//===========================================================================

// Java basic classes
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.StringTokenizer;

// Java servlet classes
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.processing.ImageProcessing;

public class ServletConsole extends HttpServlet {

    //public static final String serverUrl = "http://10.0.0.1:8081";
    public static final String serverUrl = "http://10.6.2.49:8080";
    //public static final String databaseFile = "/users/jedilink/home/LABORAL_JAVERIANA/VITRAL/vitral/testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin";
    public static final String databaseFile = "/home/jedilink/VITRAL/vitral/testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin";


    /// Warning: this code makes current implementation NOT THREAD SAFE,
    /// so will not work with multiple connections!
    private ArrayList<IndexedColorImage> workingImages;
    private SearchEngine searchEngine;
    private ShapeDatabaseFile shapeDatabase;
    private int distanceFieldSide = 64;

    private ArrayList<ServletSessionInformation> sessions;

    public void init(ServletConfig config)
          throws ServletException
    {
        System.out.println("Initializing ServletConsole class.");

        //-----------------------------------------------------------------
        int i;
        IndexedColorImage workingImage;

        if ( workingImages == null ) {
            workingImages = new ArrayList<IndexedColorImage>();
            for ( i = 0; i < 3; i++ ) {
                workingImage = new IndexedColorImage();
                workingImages.add(workingImage);
                workingImage.init(0, 0);
            }
        }

        //-----------------------------------------------------------------
        shapeDatabase = new ShapeDatabaseFile(databaseFile);
        searchEngine = new SearchEngine();
        searchEngine.readDatabase(shapeDatabase);

        sessions = new ArrayList<ServletSessionInformation>();
    }

    public void destroy()
    {
        System.out.println("Destroying ServletConsole class.");

        //- Free unused references for garbage collection -----------------
        shapeDatabase = null;
        searchEngine = null;
    }

    private void extractImage(IndexedColorImage img, String cad)
    {
        StringTokenizer auxStringTokenizer;
        auxStringTokenizer = new StringTokenizer(cad, " \n");
        String token;
        int pos;
        int xSize = 320;
        int ySize = 240;
        int index;
        int x, y;

        for ( pos = 0; auxStringTokenizer.hasMoreTokens(); pos++ ) {
            token = auxStringTokenizer.nextToken();
            switch ( pos ) {
              case 0:
                xSize = Integer.parseInt(token);
                break;
              case 1:
                ySize = Integer.parseInt(token);
                img.init(xSize, ySize);
                break;
              default:
                index = Integer.parseInt(token);
                if ( index < xSize*ySize ) {
                    y = index / xSize;
                    x = index % xSize;
                    img.putPixel(x, y, VSDK.unsigned8BitInteger2signedByte(255));
                }
                break;
            }
        }
    }

    private ServletSessionInformation searchSessionById(long id)
    {
        int i;
        ServletSessionInformation s;

        for ( i = 0; i < sessions.size(); i++ ) {
            s = sessions.get(i);
            if ( s.getId() == id ) {
                return s;
            }
        }
        return null;
    }

    private void receiveImages(
                      HttpServletRequest request,
                      HttpServletResponse response,
                      String id)
        throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        //-----------------------------------------------------------------
        String cad;
        Enumeration listaDeParametros;

        listaDeParametros = request.getParameterNames();
        IndexedColorImage img;
        int i;
        long sessionId = 0;
        ServletSessionInformation currentSession = null;

        for ( i = 0; i < 3; i++ ) {
            img = workingImages.get(i);
            img.init(0, 0);
        }

        while ( listaDeParametros.hasMoreElements() ) {
            cad = "" + listaDeParametros.nextElement();
            if ( cad.startsWith("session") ) {
                sessionId = Long.parseLong(request.getParameter(cad));
                System.out.println("Searching for session: " + sessionId);
                currentSession = searchSessionById(sessionId);
                if ( currentSession == null ) {
                    currentSession = new ServletSessionInformation();
                    sessions.add(currentSession);
                    System.out.println("Creating new session " + sessionId);
                }
                sessionId = currentSession.getId();
                out.println("session: " + sessionId);
            }
            else if ( cad.startsWith("image") ) {
                i = Integer.parseInt(cad.substring(5)) - 1;
                if ( i >= 0 && i < 3 ) {
                    extractImage(workingImages.get(i), request.getParameter(cad));
                }
                out.println("filespec: " + cad);
            }
        }
        out.println("done");

        //-----------------------------------------------------------------
        System.out.print("Log message: (ServletConsole::" + id + ").");
        listaDeParametros = request.getParameterNames();
        System.out.println("  - Parameters:");
        while ( listaDeParametros.hasMoreElements() ) {
            cad = "" + listaDeParametros.nextElement();
            if ( cad.startsWith("image") ) {
                System.out.println("    . Recieving an image (" + cad + ")!");
              }
              else {
                System.out.println("    . " + cad + " = " + request.getParameter(cad));
            }
        }

        for ( i = 0; currentSession != null && i < 3; i++ ) {
            currentSession.setSourceImage(workingImages.get(i), i);
        }
    }

    private void extractOutlineImage(
        IndexedColorImage source, IndexedColorImage target)
    {
        int x, y;
        int x0Roi, y0Roi, x1Roi, y1Roi;
        IndexedColorImage roi, squaredRoi, framedRoi;
        int val;

        x1Roi = 0;
        y1Roi = 0;
        x0Roi = source.getXSize();
        y0Roi = source.getYSize();
        for ( y = 0; y < source.getYSize(); y++ ) {
            for ( x = 0; x < source.getXSize(); x++ ) {
                val = source.getPixel(x, y);
                if ( val != 0 ) {
                    if ( x1Roi < x ) x1Roi = x;
                    if ( y1Roi < y ) y1Roi = y;
                    if ( x0Roi > x ) x0Roi = x;
                    if ( y0Roi > y ) y0Roi = y;
                }
            }
        }
        roi = new IndexedColorImage();
        squaredRoi = new IndexedColorImage();
        framedRoi = new IndexedColorImage();

        ImageProcessing.extractRoi(source, roi, x0Roi, y0Roi, x1Roi, y1Roi);
        ImageProcessing.squareFill(roi, squaredRoi);
        ImageProcessing.frame(squaredRoi, framedRoi,
                              squaredRoi.getXSize()/10);
        ImageProcessing.resize(framedRoi, target);
        for ( y = 0; y < target.getYSize(); y++ ) {
            for ( x = 0; x < target.getXSize(); x++ ) {
                val = target.getPixel(x, y);
                if ( val != 0 ) {
                    target.putPixel(x, y, VSDK.unsigned8BitInteger2signedByte(255));
                }
            }
        }
    }

    private void saveImage(Image img, String filename, PrintWriter out)
    {
        File f = new File("/usr/local/apache-tomcat-6.0.13/webapps/images/" + filename);
        if ( f == null ) {
            out.println("Error writing image. No file space available for servlet.");
        }
        ImagePersistence.exportJPG(f, img);
    }

    private ArrayList<Result>
    execute2DSketchQuery(ServletSessionInformation currentSession,
                         PrintWriter out)
    {
        ArrayList<Result> similarModels = new ArrayList<Result>();

        IndexedColorImage img1 = workingImages.get(0);
        IndexedColorImage img2 = workingImages.get(1);
        IndexedColorImage img3 = workingImages.get(2);

        if ( (img1 == null) && (img2 == null) && (img3 == null) ) {
            out.println("No images. Error importing distance field. Query aborted.");
            return similarModels;
        }

        //-----------------------------------------------------------------
        int i, j;
        IndexedColorImage img, outline, distanceField;

        for ( i = 0, j = 0; i < 3; i++ ) {
            img = workingImages.get(i);
            if ( img == null || img.getXSize() <= 0 || img.getYSize() <= 0 ) {
                continue;
            }
            outline = new IndexedColorImage();
            outline.init(distanceFieldSide, distanceFieldSide);
            extractOutlineImage(img, outline);
            currentSession.setOutline(outline, i);

            distanceField = new IndexedColorImage();
            distanceField.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(outline, distanceField, 1);

            if ( j == 0 ) {
                similarModels = searchEngine.matchSketch(distanceField, shapeDatabase.descriptorsArray, 5);
                j++;
            }
            else {
                similarModels = searchEngine.matchSketchRestricted(distanceField, 5, similarModels);
                j++;
            }

            //
            RGBImage distanceFieldRgb;
            ImageProcessing.gammaCorrection(distanceField, 2.0);
            distanceFieldRgb = distanceField.exportToRgbImage();
            int x, y;

            for ( x = 0; x < distanceFieldRgb.getXSize(); x++ ) {
                for ( y = 0; y < distanceFieldRgb.getYSize(); y++ ) {
                    if ( distanceField.getPixel(x, y) < 1 ) {
                        distanceFieldRgb.putPixel(x, y,
                            (byte)255, (byte)0, (byte)0);
                    }
                }
            }
            currentSession.setDistanceField(distanceFieldRgb, i);
        }

        Collections.sort(similarModels);

        return similarModels;
    }

    private void
    processInformation(HttpServletRequest request, PrintWriter out, ServletSessionInformation currentSession)
    {
        int id, i;
        id = Integer.parseInt(request.getParameter("id"));
        DecimalFormat f1 = new DecimalFormat("0000000");
        DecimalFormat f2 = new DecimalFormat("00");

        out.println("<HTML><BODY>\n");
        GeometryMetadata data;
        String imgname, basename;

        data = searchEngine.searchMetadataById(shapeDatabase.descriptorsArray, id);
        if ( data != null ) {
            // Print basic information
            out.println("<H1>GEOMETRIC MODEL DETAILS</H1>");
            out.println("<P><HR><P><H2>FOUNDED 3D MODEL INFORMATION</H2>\n");
            out.println("Basic model information\n");
            out.println("<UL>\n");
            out.println("<LI>Model filename in shape server: " + data.getFilename() + "\n");
            out.println("<LI>PENDING LINK TO DOWNLOAD MODEL\n");
            out.println("</UL><P>\n");

            // Include precomputed thumbnails
            basename = serverUrl + "/images/previews/" + f1.format(id, new StringBuffer(""), new FieldPosition(0)).toString() + "/";
            out.println("Preprocessed views:<BR>\n");
            out.println("<CENTER><TABLE WIDTH=100% CELLPADDING=3 CELLSPACING=3 BORDER=0>\n");
            out.println("<TR>\n");
            for ( i = 0; i < 8; i++ ) {
                imgname = f2.format(i, new StringBuffer(""), new FieldPosition(0)).toString();
                out.println("<TD><A HREF=\"" + basename + imgname + ".jpg" + "\"><CENTER><IMG SRC=\"" + basename + imgname + "small.jpg" + "\"></IMG></CENTER></A></TD\n");
                if ( i == 3 ) {
                    out.println("</TR><TR>");
                }
            }
            out.println("</TR></TABLE></CENTER>\n");

            // Include primitive count information
            ShapeDescriptor sd;
            double featureVector[];
            int n;

            sd = data.getDescriptorByName("PRIMITIVE_COUNT");

            out.println("Privitive count:\n");

            if ( sd != null ) {
                featureVector = sd.getFeatureVector();
                n = (int)(Math.floor(featureVector[VSDK.TRIANGLE]));
                out.println("<CENTER><TABLE BORDER=2 WIDTH=100% CELLPADDING=3 CELLSPACING=3>\n");
                out.println("<TR><TH>Triangles</TH></TR>");
                out.println("<TR><TD>" + n);
                out.println("</TD></TR>");
                out.println("</TR></TABLE></CENTER>\n");
            }
            else {
                out.println("No PRIMITIVE_COUNT shape descriptor inside metadata descriptor for model with ID " + id + ".\n");
            }
        }
        else {
            out.write("<B>No metadata descriptor for model with ID " + id + ".\n");
        }

        writeDebuggingReport(currentSession, out);

        out.println("</BODY></HTML>\n");
    }

    private void
    writeDebuggingReport(ServletSessionInformation currentSession,
                         PrintWriter out)
    {
        int i;
        IndexedColorImage img;

        out.println("<P><HR><P><H2>QUERY INFORMATION</H2>\n");
        out.println("<CENTER><TABLE BORDER=2>\n");
        out.println("<TR><TH>Image number</TH>\n");
        out.println("<TH>2D Sketch recieved from applet</TH>\n");
        out.println("<TH>Normalized 2D Sketch to 64x64 pixels</TH>\n");
        out.println("<TH>Distance field (gamma corrected at factor 2 and border highlighted)</TH></TR>\n");

        for ( i = 0; (workingImages != null) && i < 3; i++ ) {
            img = workingImages.get(i);
            if ( img != null && img.getXSize() > 0 && img.getYSize() > 0 ) {
                out.println("<TR><TD><CENTER><B>" + i + "</B></CENTER></TD>\n");
                out.println("<TD><CENTER><IMG SRC=\"ServletConsole?session=" + currentSession.getId() + "&input=retrieve_sketch&image_source=source" + i + "\"></IMG></CENTER></TD>\n");
                out.println("<TD><CENTER><IMG SRC=\"ServletConsole?session=" + currentSession.getId() + "&input=retrieve_sketch&image_source=outline" + i + "\"></IMG></CENTER></TD>\n");
                out.println("<TD><CENTER><IMG SRC=\"ServletConsole?session=" + currentSession.getId() + "&input=retrieve_sketch&image_source=distance_field" + i + "\"></IMG></CENTER></TD>\n");
                out.println("</TR>\n");
            }
        }
        out.println("</TABLE></CENTER>\n");
    }

    private void
    process2DSketchQuery(ServletSessionInformation currentSession,
                         PrintWriter out)
    {
        //-----------------------------------------------------------------
        currentSession.setSimilarModels(execute2DSketchQuery(currentSession, out));
        int similarSize = currentSession.getSimilarModels().size();
        int minSize = 16;
        if ( similarSize < minSize ) {
            minSize = similarSize;
        }
        searchEngine.writeResultsAsHtml(out, currentSession.getSimilarModels(), shapeDatabase.descriptorsArray, serverUrl + "/images", currentSession.getId(), 0, minSize);
    }

    private void retrieveSketch(ServletSessionInformation currentSession, 
                                HttpServletRequest request,
                                HttpServletResponse response, OutputStream os)
    {
        response.setContentType("image/jpeg");
        String source;
        try {
            Image img = null;
            source = request.getParameter("image_source");
            if ( source != null && source.startsWith("source") ) {
                img = currentSession.getSourceImage(Integer.parseInt(source.substring(6)));
            }
            else if ( source != null && source.startsWith("outline") ) {
                img = currentSession.getOutline(Integer.parseInt(source.substring(7)));
            }
            else if ( source != null && source.startsWith("distance_field") ) {
                img = currentSession.getDistanceField(Integer.parseInt(source.substring(14)));
            }
            else {
                img = new RGBImage();
                img.init(640, 480);
                img.createTestPattern();
            }
            ImagePersistence.exportJPG(os, img);
        }
        catch ( Exception e ) {
            System.out.println("Error sending image.");
            e.printStackTrace();
        }
    }

    private void processGeneric(
                      HttpServletRequest request,
                      HttpServletResponse response,
                      String id)
        throws IOException, ServletException
    {
        response.setContentType("text/html");
        OutputStream os = response.getOutputStream();
        PrintWriter out = new PrintWriter(os);
        //PrintWriter out = response.getWriter();

        //-----------------------------------------------------------------
        String cad;

        //-----------------------------------------------------------------
        Enumeration listaDeParametros;

        listaDeParametros = request.getParameterNames();
        String operation;

        ServletSessionInformation currentSession = null;

        cad = request.getParameter("session");
        if ( cad != null ) {
            currentSession = currentSession = searchSessionById(Long.parseLong(cad));
        }

        operation = request.getParameter("input");

        System.out.println("Log message: (ServletConsole::" + id + ")->");

        if ( currentSession == null ) {
            // JOB
            out.println("No session ID specified!<P>Parameters given where:<BR><UL>");
            while ( listaDeParametros.hasMoreElements() ) {
                cad = "" + listaDeParametros.nextElement();
                out.println(
                    "<LI>" +
                    cad +
                    " = " +
                    request.getParameter(cad)
                );
            }
            out.println("</UL>"); 
            // LOG
            System.out.println("No session ID specified!\nParameters given where:");
            listaDeParametros = request.getParameterNames();
            System.out.println("  - Parameters:");
            while ( listaDeParametros.hasMoreElements() ) {
                cad = "" + listaDeParametros.nextElement();
                System.out.println("    . " + cad + " = " + request.getParameter(cad));
            }
        }
        else if ( operation == null || operation.length() < 1 ) {
            // JOB
            out.println("No input parameter specifying operation found on request parameters!<P>Parameters given where:<BR><UL>");
            while ( listaDeParametros.hasMoreElements() ) {
                cad = "" + listaDeParametros.nextElement();
                out.println(
                    "<LI>" +
                    cad +
                    " = " +
                    request.getParameter(cad)
                );
            }
            out.println("</UL>"); 
            // LOG
            System.out.println("No input parameter specifying operation found on request parameters!\nParameters given where:");
            listaDeParametros = request.getParameterNames();
            System.out.println("  - Parameters:");
            while ( listaDeParametros.hasMoreElements() ) {
                cad = "" + listaDeParametros.nextElement();
                System.out.println("    . " + cad + " = " + request.getParameter(cad));
            }
          }
          else if ( operation.equals("text_2d") ) {
            // JOB
            process2DSketchQuery(currentSession, out);
            // LOG
            System.out.println("request for sketch search.");
          }
          else if ( operation.equals("model_detail") ) {
            // JOB
            processInformation(request, out, currentSession);
            // LOG
            System.out.println("request for model detail.");
          }
          else if ( operation.equals("retrieve_sketch") ) {
            // JOB
            retrieveSketch(currentSession, request, response, os);
            // LOG
            System.out.println("request for sketch search.");
          }
          else if ( operation.equals("show_cached_results") ) {
            // JOB
            cad = request.getParameter("start");
            if ( cad == null ) {
                out.printf("Showing cached results for session " + currentSession.getId() + ".");
                out.printf("Error: no start range specified.");
            }
            else {
                searchEngine.writeResultsAsHtml(out, currentSession.getSimilarModels(), shapeDatabase.descriptorsArray, serverUrl + "/images", currentSession.getId(), Integer.parseInt(cad), 16);
            }
            // LOG
            System.out.println("showing cached results.");
          }
          else {
            // JOB
            out.println("Unknown operation <B>" + operation + "</B>!<P>Parameters given where:<BR><UL>");
            while ( listaDeParametros.hasMoreElements() ) {
                cad = "" + listaDeParametros.nextElement();
                out.println(
                    "<LI>" +
                    cad +
                    " = " +
                    request.getParameter(cad)
                );
            }
            out.println("</UL>"); 
            // LOG
            System.out.println("Unknown operation " + operation + "!\nParameters given where:");
            listaDeParametros = request.getParameterNames();
            System.out.println("  - Parameters:");
            while ( listaDeParametros.hasMoreElements() ) {
                cad = "" + listaDeParametros.nextElement();
                System.out.println("    . " + cad + " = " + request.getParameter(cad));
            }
        }
        out.flush();
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        processGeneric(request, response, "doGet");
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        receiveImages(request, response, "doPost");
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
