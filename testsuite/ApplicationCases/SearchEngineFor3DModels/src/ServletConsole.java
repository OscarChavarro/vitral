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
    public static final String serverUrl = "http://10.6.2.49:8080";
    //public static final String databaseFile = "/users/jedilink/home/LABORAL_JAVERIANA/VITRAL/vitral/testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin";
    public static final String databaseFile = "/home/jedilink/VITRAL/vitral/testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin";


    /// Warning: this code makes current implementation NOT THREAD SAFE,
    /// so will not work with multiple connections!
    private ArrayList<IndexedColorImage> workingImages;
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
        IndexedColorImage img;
        int i;
        for ( i = 0; i < 3; i++ ) {
            img = workingImages.get(i);
            img.init(0, 0);
        }

        while ( listaDeParametros.hasMoreElements() ) {
            cad = "" + listaDeParametros.nextElement();
            if ( cad.startsWith("image") ) {
                i = Integer.parseInt(cad.substring(5)) - 1;
                if ( i >= 0 && i < 3 ) {
                    extractImage(workingImages.get(i), request.getParameter(cad));
                }
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

        for ( i = 0; i < 3; i++ ) {
            saveImage(workingImages.get(i), "output" + i + ".jpg", new PrintWriter(System.out));
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
    processImages(PrintWriter out)
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
        int i;
        IndexedColorImage img;
        for ( i = 0; i < 3; i++ ) {
            img = workingImages.get(i);
            if ( img == null || img.getXSize() <= 0 || img.getYSize() <= 0 ) {
                continue;
            }
            extractOutlineImage(img, outline);
            saveImage(outline, "outline" + i + ".jpg", out);

            distanceField = new IndexedColorImage();
            distanceField.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(outline, distanceField, 1);

            if ( i == 0 ) {
                similarModels = searchEngine.matchSketch(distanceField, shapeDatabase.descriptorsArray, 5);
                Collections.sort(similarModels);
                searchEngine.writeResultsAsHtml(out, similarModels, shapeDatabase.descriptorsArray, serverUrl + "/images");
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
            saveImage(distanceFieldRgb, "distanceField" + i + ".jpg", out);
        }
        return similarModels;
    }

    private void processGeneric(
                      HttpServletRequest request,
                      HttpServletResponse response,
                      String id)
        throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        //-----------------------------------------------------------------
        String cad;

        //-----------------------------------------------------------------
        Enumeration listaDeParametros;

        listaDeParametros = request.getParameterNames();

/*
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
*/

        //-----------------------------------------------------------------
        ArrayList <Result> similarModels = null;
        similarModels = processImages(out);
        //-----------------------------------------------------------------
        int i;
        IndexedColorImage img;

        out.println("<P><HR><P><H2>DEBUGGING INFORMATION</H2>\n");
        out.println("<CENTER><TABLE BORDER=2>\n");
        out.println("<TR><TH>Image number</TH>\n");
        out.println("<TH>2D Sketch recieved from applet</TH>\n");
        out.println("<TH>Normalized 2D Sketch to 64x64 pixels</TH>\n");
        out.println("<TH>Distance field (gamma corrected at factor 2 and border highlighted)</TH></TR>\n");

        for ( i = 0; (workingImages != null) && i < 3; i++ ) {
            img = workingImages.get(i);
            if ( img != null && img.getXSize() > 0 && img.getYSize() > 0 ) {
                out.println("<TR><TD><CENTER><B>" + i + "</B></CENTER></TD>\n");
                out.println("<TD><CENTER><IMG SRC=\"" + serverUrl + "/images/output" + i + ".jpg\"></IMG></CENTER></TD>\n");
                out.println("<TD><CENTER><IMG SRC=\"" + serverUrl + "/images/outline" + i + ".jpg\"></IMG></CENTER></TD>\n");
                out.println("<TD><CENTER><IMG SRC=\"" + serverUrl + "/images/distanceField" + i + ".jpg\"></IMG></CENTER></TD>\n");
                out.println("</TR>\n");
            }
        }
        out.println("</TABLE></CENTER>\n");

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
        processImages(request, response, "doPost");
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
