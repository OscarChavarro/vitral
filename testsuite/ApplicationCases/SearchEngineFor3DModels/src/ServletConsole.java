//===========================================================================

// Java basic classes
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
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
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.processing.ImageProcessing;

public class ServletConsole extends HttpServlet {

    //public static final String serverUrl = "http://10.0.0.1:8081";
    //public static final String serverUrl = "http://10.6.2.49:8080";
    public static final String serverUrl = "http://192.168.250.1:8080";
    //public static final String databaseFile = "/users/jedilink/home/LABORAL_JAVERIANA/VITRAL/vitral/testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin";
    public static final String databaseFile = "/home/jedilink/VITRAL/vitral/testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin";


    /// Warning: this code makes current implementation NOT THREAD SAFE,
    /// so will not work with multiple connections!
    private IndexedColorImage workingImage = null;
    private IndexedColorImage outline = null;
    private IndexedColorImage distanceField = null;

    private SearchEngine searchEngine;
    private ShapeDatabaseFile shapeDatabase;
    private int distanceFieldSide = 64;

    public void init(ServletConfig config)
          throws ServletException
    {
        System.out.println("Initializing ServletConsole class.");

        //-----------------------------------------------------------------
        if ( workingImage == null ) {
            workingImage = new IndexedColorImage();
        }
        workingImage.init(320, 240);
        workingImage.createTestPattern();
        if ( outline == null ) {
            outline = new IndexedColorImage();
        }
        outline.init(distanceFieldSide, distanceFieldSide);
        if ( distanceField == null ) {
            distanceField = new IndexedColorImage();
        }
        distanceField.init(distanceFieldSide, distanceFieldSide);

        //-----------------------------------------------------------------
        shapeDatabase = new ShapeDatabaseFile(databaseFile);
        searchEngine = new SearchEngine();
        searchEngine.readDatabase(shapeDatabase);
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
                if ( img.getXSize() != xSize ||
                     img.getYSize() != ySize ) {
                    System.out.printf("Initializing image to size <%d x %d>\n",
                                      xSize, ySize);
                    img.init(xSize, ySize);
                }
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

    private void processImages(
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

        while ( listaDeParametros.hasMoreElements() ) {
            cad = "" + listaDeParametros.nextElement();
            if ( cad.startsWith("image") ) {
                extractImage(workingImage, request.getParameter(cad));
                out.println("filespec: " + cad);
            }
        }
        out.println("done");

        //-----------------------------------------------------------------
        System.out.println("Log message: (ServletConsole::" + id + ").");
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

        saveImage(workingImage, "output.jpg", new PrintWriter(System.out));
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

    private void processGeneric(
                      HttpServletRequest request,
                      HttpServletResponse response,
                      String id)
        throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<body>");

        //-----------------------------------------------------------------
        String cad;

        cad = request.getParameter("param");

        out.println("Par&aacute;metro conocido: " + cad + "<P>");

        //-----------------------------------------------------------------
        Enumeration listaDeParametros;

        listaDeParametros = request.getParameterNames();

        out.println("Par&aacute;metros desconocidos:<BR><UL>");
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

        //-----------------------------------------------------------------
        ArrayList <Result> similarModels = null;
        if ( workingImage == null ) {
            out.println("Error importing distance field. Query aborted.");
        }
        else {
            extractOutlineImage(workingImage, outline);
            saveImage(outline, "outline.jpg", out);

            distanceField = new IndexedColorImage();
            distanceField.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(outline, distanceField, 1);

            similarModels = searchEngine.matchSketch(distanceField, shapeDatabase.descriptorsArray, 5);
            Collections.sort(similarModels);
            searchEngine.writeResultsAsHtml(out, similarModels, shapeDatabase.descriptorsArray, serverUrl + "/images");
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
            saveImage(distanceFieldRgb, "distanceField.jpg", out);
        }
        //-----------------------------------------------------------------
        out.println("<P><HR><P><H2>DEBUGGING INFORMATION</H2>\n");
        out.println("2D Sketch recieved from applet:<BR>\n");
        out.println("<img src=\"" + serverUrl + "/images/output.jpg\"></img>\n");
        out.println("<P>Normalized 2D Sketch to 64x64 pixels:<BR>\n");
        out.println("<img src=\"" + serverUrl + "/images/outline.jpg\"></img>\n");
        out.println("<P>Distance field (gamma corrected at factor 2 and border highlighted):<BR>\n");
        out.println("<img src=\"" + serverUrl + "/images/distanceField.jpg\"></img>\n");

        //-----------------------------------------------------------------
        out.println("</body>\n");
        out.println("</html>\n");

        //-----------------------------------------------------------------
        System.out.println("Log message: (ServletConsole::" + id + ").");
        listaDeParametros = request.getParameterNames();
        System.out.println("  - Parameters:");
        while ( listaDeParametros.hasMoreElements() ) {
            cad = "" + listaDeParametros.nextElement();
            System.out.println("    . " + cad + " = " + request.getParameter(cad));
        }
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
        if ( workingImage != null ) {
            workingImage = new IndexedColorImage();
        }

        processImages(request, response, "doPost");
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
