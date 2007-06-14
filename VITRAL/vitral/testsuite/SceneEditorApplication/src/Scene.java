import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.RGBAImageBuilder;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.geometry.RayableObject;

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
    public ArrayList<RayableObject> things;
    public int selectedThingIndex; // Negative when none selected

    Scene()
    {
        //-----------------------------------------------------------------
        things = new ArrayList<RayableObject>();
        lights = new ArrayList<Light>();
        camera = new Camera();
        activeCamera = camera;
        selectedThingIndex = -1;

        //-----------------------------------------------------------------
        simpleBackground = new SimpleBackground();
        simpleBackground.setColor(0, 0, 0);

        RGBAImage front, right, back, left, down, up;

        try {
            System.out.print("Loading background: 1");
            front = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno0_small.jpg"));
            System.out.print("2");
            right = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno1_small.jpg"));
            System.out.print("3");
            back = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno2_small.jpg"));
            System.out.print("4");
            left = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno3_small.jpg"));
            System.out.print("5");
            down = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno4_small.jpg"));
            System.out.print("6");
            up = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno5_small.jpg"));
            System.out.println(" OK!");

            cubemapBackground = 
                new CubemapBackground(camera, 
                                      front, right, back, left, down, up);
        }
        catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }

        selectedBackground = 0;

        //-----------------------------------------------------------------
        corridor = new SimpleCorridor();
        showCorridor = false;
    }

    public void selectObjectMouse(int x, int y)
    {
        Ray r;
        RayableObject gi;

        activeCamera.updateVectors();
        r = activeCamera.generateRay(x, y);

        r.t = Float.MAX_VALUE;

        Iterator i;
        int index = 0;

        selectedThingIndex = -1;
        for ( i = things.iterator(); i.hasNext(); index++ ) {
            gi = (RayableObject)i.next();
            if ( gi.doIntersection(r) ) {
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
            System.out.println("  - Thing[" + c + "]: " + ((RayableObject)i.next()).getGeometry().getClass().getName());
    }
        System.out.println("= END OF REPORT ===========================================================");
    }
}
