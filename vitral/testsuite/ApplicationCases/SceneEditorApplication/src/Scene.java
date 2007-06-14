import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.ProgressMonitorConsole;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.Raytracer;

public class Scene
{
    //- 1. Camera ----------------------------------------------------------
    public Camera camera;
    public Camera activeCamera;

    //- 2. Lights ----------------------------------------------------------
    public ArrayList<Light> lights;

    //- 3. Background ------------------------------------------------------
    public SimpleBackground simpleBackground;
    public CubemapBackground cubemapBackground;
    public int selectedBackground;

    //- 4. Objects ---------------------------------------------------------
    public SimpleCorridor corridor;
    public boolean showCorridor;
    public boolean showGrid;
    public ArrayList<SimpleBody> things;
    public int selectedThingIndex; // Negative when none selected

    Scene()
    {
        //-----------------------------------------------------------------
        things = new ArrayList<SimpleBody>();
        lights = new ArrayList<Light>();

        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(45), Math.toRadians(-35), 0);
        camera = new Camera();
        camera.setPosition(new Vector3D(-5, -5, 5));
        camera.setRotation(R);

        activeCamera = camera;
        selectedThingIndex = -1;

        //-----------------------------------------------------------------
        simpleBackground = new SimpleBackground();
        simpleBackground.setColor(0.49, 0.49, 0.49);

    cubemapBackground = null;

        selectedBackground = 0;

        //-----------------------------------------------------------------
        corridor = new SimpleCorridor();
        showCorridor = false;
        showGrid = true;
    }

    public boolean
    buildCubemap()
    {
        RGBAImage front, right, back, left, down, up;

        try {
            System.out.print("Loading background: 1");
            front = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno0.jpg"));
            System.out.print("2");
            right = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno1.jpg"));
            System.out.print("3");
            back = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno2.jpg"));
            System.out.print("4");
            left = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno3.jpg"));
            System.out.print("5");
            down = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno4.jpg"));
            System.out.print("6");
            up = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno5.jpg"));
            System.out.println(" OK!");

            cubemapBackground = 
                new CubemapBackground(camera, 
                                      front, right, back, left, down, up);
        }
        catch (Exception e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    public void selectObjectWithMouse(int x, int y)
    {
        Ray r;
        SimpleBody gi;

        activeCamera.updateVectors();
        r = activeCamera.generateRay(x, y);

        double nearestDistance = Float.MAX_VALUE;

        Iterator i;
        int index = 0;

        selectedThingIndex = -1;
        for ( i = things.iterator(); i.hasNext(); index++ ) {
            gi = (SimpleBody)i.next();
            if ( gi.doIntersection(r) && r.t < nearestDistance ) {
                nearestDistance = r.t;
                selectedThingIndex = index;
            }
    }
    }

    public void print()
    {
        System.out.println("= SCENE REPORT ============================================================");
        System.out.println("Current camera:\n" +  activeCamera);
        System.out.println("Things in scene: " + things.size());
        int c = 0;
        for ( Iterator i = things.iterator(); i.hasNext(); c++ ) {
            System.out.println("  - Thing[" + c + "]: " + ((SimpleBody)i.next()).getGeometry().getClass().getName());
    }
        System.out.println("= END OF REPORT ===========================================================");
    }

    public void raytrace(RGBImage out_Viewport)
    {
        int originalWidth;
        int originalHeight;

        originalWidth = (int)activeCamera.getViewportXSize();
        originalHeight = (int)activeCamera.getViewportYSize();
        activeCamera.updateViewportResize(out_Viewport.getXSize(), out_Viewport.getYSize());

        //-----------------------------------------------------------------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();        
    Raytracer visualizationEngine;

        Background activeBackground;
        switch ( selectedBackground ) {
          case 1:
        if ( cubemapBackground == null ) {
                buildCubemap();
            }
        if ( cubemapBackground != null ) {
                activeBackground = cubemapBackground;
        }
        else {
                activeBackground = simpleBackground;
        }
            break;
          case 0: default:
            activeBackground = simpleBackground;
            break;
        }

        visualizationEngine = new Raytracer();
        visualizationEngine.execute(out_Viewport, things, lights, 
                    activeBackground, activeCamera,
                                    reporter, null);

        //-----------------------------------------------------------------
        activeCamera.updateViewportResize(originalWidth, originalHeight);
    }

}
