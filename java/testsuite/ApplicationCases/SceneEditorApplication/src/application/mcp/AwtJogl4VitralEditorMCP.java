package application.mcp;

import java.net.ServerSocket;
import java.net.Socket;

import application.AwtJogl4SceneEditorApplication;

/**
Automation service of the editor (MCP-like JSON-RPC over TCP, see `MCP.md`):
listens on a local TCP port and serves each connection in its own thread with
an `AwtJogl4VitralEditorMCPProtocol`.
*/
public class AwtJogl4VitralEditorMCP implements Runnable
{
    private final AwtJogl4SceneEditorApplication parent;
    private final int tcpPort;

    public AwtJogl4VitralEditorMCP(AwtJogl4SceneEditorApplication parent)
    {
        this.parent = parent;
        tcpPort = 1234;
        Thread networkThread = new Thread(this);
        networkThread.setName("AwtJogl4VitralEditorMCP");
        networkThread.start();
    }

    @Override
    public void run()
    {
        System.out.println("Waiting for MCP connections on TCP port " + tcpPort);

        try ( ServerSocket serverSocket = new ServerSocket(tcpPort) ) {
            while ( true ) {
                Socket clientSocket = serverSocket.accept();
                AwtJogl4VitralEditorMCPProtocol listener =
                    new AwtJogl4VitralEditorMCPProtocol(parent, clientSocket);
                Thread listenerThread = new Thread(listener);
                listenerThread.setName("VitralEditorMCPClient");
                listenerThread.start();
            }
        }
        catch ( Exception e ) {
            System.err.println("Error in AwtJogl4VitralEditorMCP communications!");
            System.err.println(e);
        }
    }
}
