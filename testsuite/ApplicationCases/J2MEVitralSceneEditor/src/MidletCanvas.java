// J2ME classes
import javax.microedition.lcdui.Canvas;         // Platform specific elements
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
/*
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.ServiceRecord;
*/

// J2MEPolish classes
import de.enough.polish.util.ArrayList;

// VitralSDK classes
import vsdk.toolkit.common.Matrix4x4;                   // Model linealAlgebra.elements
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.CameraSnapshot;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.media.Calligraphic2DBuffer;         // I/O artifacts
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.render.j2me.J2meRGBImageUncompressedRenderer;   // View elements
import vsdk.toolkit.render.j2me.J2meCalligraphic2DBufferRenderer;
import vsdk.toolkit.render.WireframeRenderer;           // Processing elements
import vsdk.toolkit.render.SimpleRaytracer;
import vsdk.toolkit.gui.J2meSystem;                     // Controller elements
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.environment.LightType;

public class MidletCanvas extends Canvas /*implements DiscoveryListener*/ {
    // Platform specific elements
    private Font font;
    private boolean isColor;

    // Model elements
    private Camera camera;
    private SimpleScene scene;
    private RGBImageUncompressed img;
    private Calligraphic2DBuffer lineSet;
    private CameraController cameraController;

    // Control elements
    int renderingMode;

    // Other elements
    String message;
    //ArrayList <RemoteDevice> remoteDevices;

    public MidletCanvas(boolean isColor, int numColors) {
        int width = getWidth();
        int height = getHeight();

        this.isColor = isColor;

        font = Font.getDefaultFont();

        int label_h = font.getHeight();

        if ( label_h > (height / 6) ) {
            font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            label_h = font.getHeight();
        }

        message = "" + width + "x" + height;
        //remoteDevices = new ArrayList <RemoteDevice>();

        //-----------------------------------------------------------------
        renderingMode = 1;
        createModel();
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();

        camera = new Camera();
        camera.setPosition(new Vector3D(7, -4, 4));
        R = R.eulerAnglesRotation(Math.toRadians(140), Math.toRadians(-30), 0);
        camera.setNearPlaneDistance(0.001);
        camera.setFarPlaneDistance(100);
        camera.setRotation(R);

        //-----------------------------------------------------------------
        SimpleBackground background;
        Light light;

        background = new SimpleBackground();
        background.setColor(0.5, 0.5, 0.9);

        light = new Light(LightType.POINT, new Vector3D(5, -5, 5), new ColorRgb(1, 1, 1));

        scene = new SimpleScene();
        scene.addBackground(background);
        scene.addLight(light);

        //-----------------------------------------------------------------
        SimpleBody b;
        Box box;
        Sphere sphere;

        b = new SimpleBody();
        box = new Box(4, 4, 4);
        b.setGeometry(box);
        b.setPosition(new Vector3D(0, 0, 0));
        scene.addBody(b);

        b = new SimpleBody();
        sphere = new Sphere(2);
        b.setGeometry(sphere);
        b.setPosition(new Vector3D(0, 0, 0));
        scene.addBody(b);

        //-----------------------------------------------------------------
        cameraController = new CameraControllerAquynza(camera);
        lineSet = new Calligraphic2DBuffer();
        img = new RGBImageUncompressed();
    }

    /**
    The canvas is being displayed.  Compute the
    relative placement of items the depend on the screen size.
    */
    protected void showNotify() {
    }

    /*
    Paint the canvas with the current color and controls to change it.
    */
    protected void paint(Graphics g) {
        if ( isColor ) {
            colorPaint(g);
        } else {
            grayPaint(g);
        }
    }

    private void colorPaint(Graphics g) {
        //- (1/5): Platform specific frame initialization -----------------
        int width = getWidth();
        int height = getHeight();
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);

        //- (3/4): Visualization process ----------------------------------
        if ( renderingMode == 3 ) {
            RendererConfiguration q = new RendererConfiguration();
            SimpleRaytracer visualizationEngine = new SimpleRaytracer();
            CameraSnapshot cameraSnapshot =
                camera.exportToCameraSnapshot(width, height);
            SimpleSceneSnapshot sceneSnapshot =
                scene.exportToSimpleSceneSnapshot(
                    cameraSnapshot,
                    scene.getBackgrounds().get(0));

            img.init(width, height);
            visualizationEngine.execute(img, q, sceneSnapshot, null, null);

            J2meRGBImageUncompressedRenderer.draw(g, img, 0, 0);
        }
        else if ( renderingMode == 2 ) {
            camera.updateViewportResize(width, height);
            WireframeRenderer.execute(
                lineSet, scene.getSimpleBodies(), camera);
            img.init(width, height);
            img.createTestPattern();
            lineSet.exportRgbImage(img);
            J2meRGBImageUncompressedRenderer.draw(g, img, 0, 0);
        }
        else if ( renderingMode == 1 ) {
            camera.updateViewportResize(width, height);
            WireframeRenderer.execute(
                lineSet, scene.getSimpleBodies(), camera);
            J2meCalligraphic2DBufferRenderer.draw(
                g, lineSet, 0, 0, width, height);
        }

        //- (4/4): End of frame -------------------------------------------
        lineSet.init();

        //-----------------------------------------------------------------
        if ( message != null ) {
            g.setColor(255, 255, 255);
            g.fillRect(5, 5, width/3 - 10, height/4 - 10);
            g.setFont(font);
            g.setColor(0, 0, 0);
            g.drawString(message, 10, 10, Graphics.TOP | Graphics.LEFT);
            message = null;
        }
    }

    private void grayPaint(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        // Fill the background
        g.setGrayScale(0xff);
        g.fillRect(0, 0, width, height);

        // Fill in the gray sample
        g.setGrayScale(0x80);
        g.fillRect(0, 0, 10, 10);
        g.setGrayScale(0);
        g.drawRect(1, 10, 20, 20);

        g.drawString("B & W mode not developed!", 0, 0, Graphics.BOTTOM | Graphics.RIGHT);
    }

    public void keyRepeated(int key) {
        keyPressed(key);
    }

    protected void keyPressed(int keycode) {
        KeyEvent vsdke = J2meSystem.j2me2vsdkEvent(this, keycode);
        Vector3D p;

        if ( cameraController.processKeyPressedEvent(vsdke) ) {
            repaint();
        }

        p = scene.getSimpleBodies().get(0).getPosition();

        switch ( vsdke.keycode ) {
          case KeyEvent.KEY_4:
            p.x -= 0.1;
            break;
          case KeyEvent.KEY_6:
            p.x += 0.1;
            break;
          case KeyEvent.KEY_2:
            p.y += 0.1;
            break;
          case KeyEvent.KEY_8:
            p.y -= 0.1;
            break;
          case KeyEvent.KEY_1:
            p.z += 0.1;
            break;
          case KeyEvent.KEY_7:
            p.z -= 0.1;
            break;
          case KeyEvent.KEY_0:
            renderingMode++;
            if ( renderingMode > 3 ) {
                renderingMode = 1;
            }
            break;
          case KeyEvent.KEY_5:
            connectBlueTooth();
            break;
        }

        scene.getSimpleBodies().get(0).setPosition(p);
        repaint();

    }

    //=================================================================
    //= Bluetooth stuff. Should be located at another class           =
    //=================================================================
/*
    public void inquiryCompleted(int discType) {
            if (discType == DiscoveryListener.INQUIRY_COMPLETED) {
                    message = " \nInquiry completed";
            } else if (discType == DiscoveryListener.INQUIRY_TERMINATED) {
                    message = "\nInquiry terminated";
            } else if (discType == DiscoveryListener.INQUIRY_ERROR) {
                    message = "\nInquiry error";
            }
    }

    public void serviceSearchCompleted(int transID, int respCode) {}

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord){}

    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass cod) {
        try{
            remoteDevices.add(remoteDevice);
            message = "" + remoteDevices.size() + remoteDevice.getFriendlyName(true);
          }
          catch(Exception e) {
            message = "Bluetooth error (1)";
        }
        repaint();
    }
*/

    private void connectBlueTooth()
    {
        try {
            Class.forName("javax.bluetooth.LocalDevice");
            message = "Bluetooth API available!";
/*
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            DiscoveryAgent discoveryAgent;
            discoveryAgent = localDevice.getDiscoveryAgent();
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
*/
        }
        catch ( Exception e ) {
            message = "Bluetooth not found.";
        }
    }

}
