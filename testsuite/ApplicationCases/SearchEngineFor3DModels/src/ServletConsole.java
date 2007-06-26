//===========================================================================

// Java basic classes
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Enumeration;

// Java servlet classes
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletConsole extends HttpServlet {
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
        out.println("</body>");
        out.println("</html>");

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
        processGeneric(request, response, "doPost");
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
