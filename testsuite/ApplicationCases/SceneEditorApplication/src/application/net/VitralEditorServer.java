//===========================================================================

package application.net;

// Java basic classes
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Quaternion;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.io.PersistenceElement;

// Application classes
import application.SceneEditorApplication;
import application.render.jogl.JoglAwtViewportWindow;

class VitralEditorServerProtocol implements Runnable
{
    private SceneEditorApplication parent;
    private Socket socket;
    public VitralEditorServerProtocol(SceneEditorApplication parent, Socket socket)
    {
        this.parent = parent;
        this.socket = socket;
    }

    /**
    This method implements current protocol.
    The input array is a petition string. Current supported petition strings
    are:
      - "getSelectedPosition"
      - "getSelectedRotation"
    Current selected data is extracted from selected object (if one object is
    selected) or from current camera if no object is selected.

    The output array is a codified binary answer containing two parts: a byte
    identifying the data type returned and a series of bytes containing
    the data itself.  The current supported packets are:
      - 1 - empty answer
      - 2 - double answer followed by 4 bytes of big endian double number
      - 3 - Vector3D answer followed by 12 bytes of big endian vector data
      - 4 - Quaternion answer followed by 16 bytes of big endian quaternion data
    */
    private byte[] servePetition(String in)
    {
        //-----------------------------------------------------------------
        byte[] out = null;
        Vector3D pos;
        Quaternion q;
        Matrix4x4 R;
        Camera camera;
        int firstThingSelected = parent.theScene.selectedThings.firstSelected();

        if ( firstThingSelected >= 0 && 
             firstThingSelected < parent.theScene.scene.getSimpleBodies().size() ) 
        {
            pos = parent.theScene.scene.getSimpleBodies().get(firstThingSelected).getPosition();
            R = parent.theScene.scene.getSimpleBodies().get(firstThingSelected).getRotation();
        }
        else {
            camera = ((JoglAwtViewportWindow)(parent.drawingArea.viewOrganizer.getViews().get(parent.drawingArea.viewOrganizer.getSelectedViewIndex()))).getCamera();
            pos = camera.getPosition();
            R = camera.getRotation();
        }
        q = R.exportToQuaternion();

        //-----------------------------------------------------------------
        if ( in.equals("getSelectedPosition") ) {
            //System.out.println("Processing position vector " + pos);
            out = new byte[13];
            out[0] = 3;
            PersistenceElement.float2byteArrayBE(out, 1, (float)pos.x);
            PersistenceElement.float2byteArrayBE(out, 5, (float)pos.y);
            PersistenceElement.float2byteArrayBE(out, 9, (float)pos.z);
        }
        else if ( in.equals("getSelectedRotation") ) {
            //System.out.println("Processing rotation quaternion " + q);
            out = new byte[17];
            out[0] = 4;
            PersistenceElement.float2byteArrayBE(out, 1, (float)q.direction.x);
            PersistenceElement.float2byteArrayBE(out, 5, (float)q.direction.y);
            PersistenceElement.float2byteArrayBE(out, 9, (float)q.direction.z);
            PersistenceElement.float2byteArrayBE(out, 13, (float)q.magnitude);
        }
        else {
            out = new byte[1];
            out[0] = 1;
        }

        return out;
    }

    public void run()
    {
        InputStream is;
        OutputStream os;
        String msg;

        System.out.println("Creating new connection with TCP client!");

        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            while ( true ) {
                msg = PersistenceElement.readAsciiString(is);
                if ( msg != null && !msg.equals("") ) {
                    os.write(servePetition(msg));
                }
                else {
                    System.out.println("Connection dropped!");
                    return;
                }
            }
        }
        catch ( Exception e ) {
            System.err.println("Error on VitralEditorServerProtocol!");
            System.err.println(e);
        }
    }
}

public class VitralEditorServer implements Runnable
{
    private SceneEditorApplication parent;
    private int tcpPort;

    public VitralEditorServer(SceneEditorApplication parent)
    {
        this.parent = parent;
        tcpPort = 1234;
        Thread networkThread = new Thread(this);
        networkThread.start();
    }

    public void run()
    {
        ServerSocket ss;
        Socket cs;
        VitralEditorServerProtocol listener;

        System.out.println("Waiting for connections on TCP port " + tcpPort);

        try {
            ss = new ServerSocket(tcpPort);
            while ( true ) {
                cs = ss.accept();
                listener = new VitralEditorServerProtocol(parent, cs);
                Thread listenerThread;
                listenerThread = new Thread(listener);
                listenerThread.start();
            }
        }
        catch ( Exception e ) {
            System.err.println("Error in VitralEditorServer communications!");
            System.err.println(e);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
