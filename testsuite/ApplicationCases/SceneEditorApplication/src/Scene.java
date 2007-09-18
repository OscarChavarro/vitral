//===========================================================================
import java.io.File;
import java.util.ArrayList;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.FixedBackground;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.render.Raytracer;

public class Scene
{
    public SimpleScene scene;

    //- 1. Camera ----------------------------------------------------------
    public Camera camera;
    public Camera activeCamera;

    //- 3. Background ------------------------------------------------------
    public SimpleBackground simpleBackground;
    public CubemapBackground cubemapBackground;
    public FixedBackground fixedBackground;
    public int selectedBackground;

    //- 4. Objects ---------------------------------------------------------
    public SimpleCorridor corridor;
    public boolean showCorridor;
    public boolean showGrid;
    public ArrayList<SimpleBodyGroup> debugThingGroups;

    public SelectionSet selectedThings;
    public SelectionSet selectedDebugThingGroups;

    // Others
    public RendererConfiguration qualityTemplate;
    private int acumObject = 1;

    Scene()
    {
        scene = new SimpleScene();

        //-----------------------------------------------------------------
        debugThingGroups = new ArrayList<SimpleBodyGroup>();

        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(45), Math.toRadians(-35), 0);
        camera = new Camera();
        camera.setPosition(new Vector3D(-5, -5, 5));
        camera.setRotation(R);

        activeCamera = camera;
        selectedThings = new SelectionSet(scene.getSimpleBodies());
        selectedDebugThingGroups = new SelectionSet(debugThingGroups);

        //-----------------------------------------------------------------
        simpleBackground = new SimpleBackground();
        simpleBackground.setColor(0.49, 0.49, 0.49);

        cubemapBackground = null;
        fixedBackground = null;

        selectedBackground = 0;

        //-----------------------------------------------------------------
        corridor = new SimpleCorridor();
        showCorridor = false;
        showGrid = true;

        qualityTemplate = new RendererConfiguration();
        qualityTemplate.setSurfaces(true);
        qualityTemplate.setWires(false);
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

    public boolean
    buildFixedmap()
    {
        RGBAImage img;

        try {
            System.out.print("Loading background: ");
            img = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno0.jpg"));
            System.out.println("OK!");

            fixedBackground = new FixedBackground(camera, img);
        }
        catch (Exception e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    public Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        m.setPhongExponent(100);
        return m;
    }

    public SimpleBody addThing(Geometry g)
    {
        SimpleBody thing;

        thing = new SimpleBody();
        thing.setGeometry(g);
        thing.setPosition(new Vector3D());
        thing.setRotation(new Matrix4x4());
        thing.setRotationInverse(new Matrix4x4());
        thing.setMaterial(defaultMaterial());
        thing.setName("Geometric object " + acumObject);
        scene.getSimpleBodies().add(thing);

        acumObject++;
        selectedThings.sync();
        return thing;
    }

    public boolean doIntersection(Ray r, GeometryIntersectionInformation info)
    {
        int i;
        double nearestDistance = Float.MAX_VALUE;
        ArrayList<SimpleBody> things = scene.getSimpleBodies();
        boolean intersected = false;
        SimpleBody gi;
        GeometryIntersectionInformation ii;

        ii = new GeometryIntersectionInformation();
        for ( i = 0; i < things.size(); i++ ) {
            gi = things.get(i);
            if ( gi.doIntersection(r) && r.t < nearestDistance ) {
                gi.doExtraInformation(r, r.t, ii);
                nearestDistance = r.t;
                intersected = true;
            }
        }
        if ( intersected ) {
            r.t = nearestDistance;
            info.clone(ii);
            return true;
        }
        return false;
    }

    public void selectObjectWithMouse(int x, int y, boolean composite, Ray ro)
    {
        Ray r;
        SimpleBody gi;

        activeCamera.updateVectors();
        r = activeCamera.generateRay(x, y);

        ro.clone(r);

        double nearestDistance = Float.MAX_VALUE;

        int i;

        if ( !composite ) {
            selectedThings.unselectAll();
        }
        ArrayList<SimpleBody> things = scene.getSimpleBodies();
        for ( i = 0; i < things.size(); i++ ) {
            gi = things.get(i);
            if ( gi.doIntersection(r) && r.t < nearestDistance ) {
                nearestDistance = r.t;
                if ( !composite ) {
                    selectedThings.unselectAll();
                    selectedThings.select(i);
                }
                else {
                    selectedThings.change(i);
                }
            }
        }
    }

    public void print()
    {
        ArrayList<SimpleBody> things = scene.getSimpleBodies();
        int i;

        System.out.println("= SCENE REPORT ============================================================");
        System.out.println("Current camera:\n" +  activeCamera);
        System.out.println("Things in scene: " + things.size());
        for ( i = 0; i < things.size(); i++ ) {
            System.out.println("  - Thing[" + i + "]: " + things.get(i).getGeometry().getClass().getName());
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
          case 2:
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
          case 1:
            if ( fixedBackground == null ) {
                buildFixedmap();
            }
            if ( fixedBackground != null ) {
                activeBackground = fixedBackground;
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
        long initialTime = System.currentTimeMillis();
        visualizationEngine.execute(out_Viewport, qualityTemplate,
                                    scene.getSimpleBodies(), scene.getLights(), 
                                    activeBackground, activeCamera,
                                    reporter, null);
        long finalTime = System.currentTimeMillis();
        System.out.println("Image generated in " + (finalTime-initialTime) + " miliseconds.");

        File fd = new File("./output.jpg");

        System.out.print("Exporting result image to file: ");
        if ( !ImagePersistence.exportJPG(fd, out_Viewport) )
        {
            System.err.println("Error grabando la imagen!!");
            System.exit(1);
        }
        System.out.println(" OK!");
        System.out.println("An image has been created in the file output.jpg");

        //-----------------------------------------------------------------
        activeCamera.updateViewportResize(originalWidth, originalHeight);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
