// Awt / swing classes
import java.awt.Graphics;                       // Platform specific elements
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JPanel;
//import java.awt.event.KeyEvent;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;    // Model elements
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.CameraSnapshot;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.media.Calligraphic2DBuffer;         // I/O artifacts
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;     // View elements
import vsdk.toolkit.render.awt.AwtCalligraphic2DBufferRenderer;
import vsdk.toolkit.render.WireframeRenderer;           // Processing elements
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.gui.AwtSystem;                      // Controller elements
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.ProgressMonitorConsole;

public class SwingCanvas extends JPanel implements
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener
{
    // Platform specific elements
    private boolean appletMode;

    // Model elements
    private Camera camera;
    private SimpleScene scene;
    private RGBImage img;
    private Calligraphic2DBuffer lineSet;
    private CameraController cameraController;

    // Control elements
    int renderingMode;

    public SwingCanvas(boolean appletMode)
    {
        this.appletMode = appletMode;
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);

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
        camera.setNearPlaneDistance(1);
        camera.setFarPlaneDistance(100);
        camera.setRotation(R);

        //-----------------------------------------------------------------
        SimpleBackground background;
        Light light;

        background = new SimpleBackground();
        background.setColor(0.5, 0.5, 0.9);

        light = new Light(Light.POINT, new Vector3D(5, -5, 5), new ColorRgb(1, 1, 1));

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
        img = new RGBImage();
    }

    public void paint(Graphics g)
    {
        //- (1/4): Platform specific frame initialization -----------------
        super.paint(g);
        Rectangle r = getBounds();
        int width = r.width;
        int height = r.height;

        //- (3/4): Visualization process ----------------------------------
        if ( renderingMode == 3 ) {
            ProgressMonitorConsole reporter = new ProgressMonitorConsole();
            RendererConfiguration q = new RendererConfiguration();
            Raytracer visualizationEngine = new Raytracer();
            CameraSnapshot cameraSnapshot =
                camera.exportToCameraSnapshot(width, height);
            SimpleSceneSnapshot sceneSnapshot =
                scene.exportToSimpleSceneSnapshot(
                    cameraSnapshot,
                    scene.getBackgrounds().get(0));

            long initialTime = System.currentTimeMillis();
            img.init(width, height);
            visualizationEngine.execute(img, q, sceneSnapshot, reporter, null);

            long finalTime = System.currentTimeMillis();
            System.out.println("Image generated in " + (finalTime-initialTime) + " miliseconds.");
            AwtRGBImageRenderer.draw(g, img, 0, 0);
        }
        else if ( renderingMode == 2 ) {
            camera.updateViewportResize(width, height);
            WireframeRenderer.execute(
                lineSet, scene.getSimpleBodies(), camera);
            img.init(width, height);
            img.createTestPattern();
            lineSet.exportRgbImage(img);
            AwtRGBImageRenderer.draw(g, img, 0, 0);
        }
        else if ( renderingMode == 1 ) {
            camera.updateViewportResize(width, height);
            WireframeRenderer.execute(
                lineSet, scene.getSimpleBodies(), camera);
            AwtCalligraphic2DBufferRenderer.draw(
                g, lineSet, 0, 0, width, height);
        }

        //- (4/4): End of frame -------------------------------------------
        lineSet.init();
    }

    public void keyPressed(java.awt.event.KeyEvent e) {
        KeyEvent vsdke = AwtSystem.awt2vsdkEvent(e);
        Vector3D p;

        if ( vsdke.keycode == KeyEvent.KEY_ESC ) {
            if ( !appletMode ) {
                System.exit(0);
            }
        }

        if ( cameraController.processKeyPressedEvent(vsdke) ) {
            repaint();
        }
        switch ( vsdke.keycode ) {
          case KeyEvent.KEY_1:
            p = scene.getSimpleBodies().get(0).getPosition();
            p = p.withX(p.x() + 0.1);
            scene.getSimpleBodies().get(0).setPosition(p);
            repaint();
            break;
          case KeyEvent.KEY_0:
            renderingMode++;
            if ( renderingMode > 3 ) {
                renderingMode = 1;
            }
            repaint();
            break;
        }
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
        if ( cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            repaint();
        }
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method
    */
    public void keyTyped(java.awt.event.KeyEvent e) {
        ;
    }

    public void mouseEntered(MouseEvent e) {
        requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
      //System.out.println("Mouse exited");
    }

    public void mousePressed(MouseEvent e) {
        if ( cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            repaint();
        }
    }

    /**
    WARNING: It is not working... check pending
    */
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            repaint();
        }
    }
}
