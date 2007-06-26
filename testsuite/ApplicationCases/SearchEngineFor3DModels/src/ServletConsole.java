//===========================================================================

// Java basic classes
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
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
import vsdk.toolkit.io.image.ImagePersistence;

public class ServletConsole extends HttpServlet {

    /// Warning: this code makes current implementation NOT THREAD SAFE,
    /// so will not work with multiple connections!
    private IndexedColorImage workingImage = null;

    public void init(ServletConfig config)
          throws ServletException
    {
        System.out.println("Initializing ServletConsole class.");
	if ( workingImage == null ) {
            workingImage = new IndexedColorImage();
	}
        workingImage.init(320, 240);
        workingImage.createTestPattern();
    }

    public void destroy()
    {
        System.out.println("Destroying ServletConsole class.");
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
        out.println("<img src=\"http://10.0.0.1:8080/output.jpg\"></img>\n");

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
        File f = new File("/tmp/output.jpg");

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
