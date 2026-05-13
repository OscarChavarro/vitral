// Java basic classes
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class NetworkAttendingThread implements Runnable
{
    private Socket socket;
    private ServerDaemon parent;

    public NetworkAttendingThread(Socket socket, ServerDaemon parent)
    {
        this.socket = socket;
        this.parent = parent;
    }

    public void run()
    {
        System.out.println("Processing communications!");
        boolean terminate = false;
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                    socket.getInputStream()));
            String inputLine, outputLine = "";
            StringTokenizer auxStringTokenizer;
            String token;

            out.println("Connection ready");
            int n, i;

            String args[];
            do {
                inputLine = in.readLine();
                if ( inputLine.equals("close") ) {
                    outputLine = "disconnect";
                    terminate = true;
                }
                else if ( inputLine.equals("quit") ) {
                    outputLine = "disconnect";
                    parent.terminate = true;
                    terminate = true;
                }
                else if ( inputLine.startsWith("add") ||
                          inputLine.startsWith("search") ||
                          inputLine.startsWith("export") ) {
                    auxStringTokenizer = new StringTokenizer(inputLine, " ");
                    for ( n = 0; auxStringTokenizer.hasMoreTokens(); n++ ) {
                        token = auxStringTokenizer.nextToken();
                    };
                    auxStringTokenizer = new StringTokenizer(inputLine, " ");
                    args = new String[n];
                    for ( i = 0; i < n; i++ ) {
                        args[i] = new String(auxStringTokenizer.nextToken());
                    }
                    parent.command = args;
                }
                else {
                    outputLine = "Unknown command " + inputLine + ", ignoring it.";
                }
                out.println(outputLine);
            } while ( !terminate );

            out.close();
            in.close();
            socket.close();

          }
          catch ( Exception e ) {
            e.printStackTrace();
        }
        System.out.println("Closing communications thread!");
    }
}
