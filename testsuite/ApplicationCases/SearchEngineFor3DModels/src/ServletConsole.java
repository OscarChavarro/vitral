//===========================================================================

// Java basic classes
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
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
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.processing.ImageProcessing;

public class ServletConsole extends HttpServlet {

    /// Warning: this code makes current implementation NOT THREAD SAFE,
    /// so will not work with multiple connections!
    private IndexedColorImage workingImage = null;
    private IndexedColorImage outline = null;
    private IndexedColorImage distanceField = null;

    private SearchEngine searchEngine;
    private ArrayList<GeometryMetadata> descriptorsArray;
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
        descriptorsArray = new ArrayList<GeometryMetadata>();
        searchEngine = new SearchEngine();
        searchEngine.readDatabase(descriptorsArray, "/home/jedilink/VITRAL/vitral/testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin");
    }

    public void destroy()
    {
        System.out.println("Destroying ServletConsole class.");

        //- Free unused references for garbage collection -----------------
        int i;
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptorsArray.set(i, null);
        }
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptorsArray.remove(0);
        }
        descriptorsArray = null;
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
	    ImageProcessing.resize(workingImage, outline);
            File f = new File("/usr/local/apache-tomcat-6.0.13/webapps/images/outline.jpg");
            if ( f != null ) {
                out.println("<P>Internal outline in file " + f.getParentFile().getAbsolutePath() + ":<P>");
            }
            else {
                out.println("Error writing image. No file space available for servlet.");
            }
            ImagePersistence.exportJPG(f, outline);

outline = 
            distanceField = new IndexedColorImage();
            distanceField.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(outline, distanceField, 1);
            //similarModels = searchEngine.matchSketch(distanceField, descriptorsArray, 5);
            //searchEngine.writeResultsAsHtml(out, similarModels, descriptorsArray);
	}
        //-----------------------------------------------------------------
        //out.println("<img src=\"http://10.0.0.1:8080/output.jpg\"></img>\n");
        out.println("<img src=\"http://10.6.2.49:8080/images/output.jpg\"></img>\n");
        out.println("<img src=\"http://10.6.2.49:8080/images/outline.jpg\"></img>\n");

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

        //-----------------------------------------------------------------
        //File f = new File("/tmp/output.jpg");
        File f = new File("/usr/local/apache-tomcat-6.0.13/webapps/images/output.jpg");

        if ( f != null ) {
            System.out.println("Trying to write an image in " + f.getParentFile().getAbsolutePath());
        }
        else {
            System.err.println("Error writing image. No file space available for servlet.");
        }
        ImagePersistence.exportJPG(f, workingImage);

    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
