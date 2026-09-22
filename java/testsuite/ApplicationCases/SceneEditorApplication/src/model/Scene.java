package model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.gui.feedback.ProgressMonitorConsole;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.camera.CameraSnapshot;
import vsdk.toolkit.environment.background.Background;
import vsdk.toolkit.environment.background.SimpleBackground;
import vsdk.toolkit.environment.background.CubemapBackground;
import vsdk.toolkit.environment.background.FixedBackground;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.render.jogl.Jogl4LightRenderer;
import vsdk.toolkit.render.raytracing.SimpleRaytracer;

// Application classes
import vsdk.toolkit.fixtures.Jogl2SimpleCorridorSample;

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
    public Jogl2SimpleCorridorSample corridor;
    public boolean showCorridor;
    public ArrayList<SimpleBodyGroup> debugThingGroups;

    public SelectionSet selectedThings;
    public SelectionSet selectedLights;
    public SelectionSet selectedDebugThingGroups;

    // Others
    public RendererConfiguration qualityTemplate;
    private int acumObject = 1;

    public Scene()
    {
        scene = new SimpleScene();

        //-----------------------------------------------------------------
        debugThingGroups = new ArrayList<SimpleBodyGroup>();

        Matrix4x4d R = new Matrix4x4d();
        camera = new Camera();

        R = R.eulerAnglesRotation(Math.toRadians(45), Math.toRadians(-35), 0);
        camera.setPosition(new Vector3Dd(-5, -5, 5));
        camera.setRotation(R);

        activeCamera = camera;
        selectedThings = new SelectionSet(scene.getSimpleBodies());
        selectedLights = new SelectionSet(scene.getLights());
        selectedDebugThingGroups = new SelectionSet(debugThingGroups);

        //-----------------------------------------------------------------
        simpleBackground = new SimpleBackground();
        simpleBackground.setColor(0.49, 0.49, 0.49);

        cubemapBackground = null;
        fixedBackground = null;

        selectedBackground = 0;

        //-----------------------------------------------------------------
        corridor = new Jogl2SimpleCorridorSample();
        showCorridor = false;

        qualityTemplate = new RendererConfiguration();
        qualityTemplate.setSurfaces(true);
        qualityTemplate.setWires(false);
    }

    public boolean
    buildCubemap()
    {
        RGBAImageUncompressed front, right, back, left, down, up;

        try {
            System.out.print("Loading background: 1");
            front = ImagePersistence.importRGBA(
                        new File("../../../../etc/cubemaps/dorise1/entorno0.jpg"));
            System.out.print("2");
            right = ImagePersistence.importRGBA(
                        new File("../../../../etc/cubemaps/dorise1/entorno1.jpg"));
            System.out.print("3");
            back = ImagePersistence.importRGBA(
                        new File("../../../../etc/cubemaps/dorise1/entorno2.jpg"));
            System.out.print("4");
            left = ImagePersistence.importRGBA(
                        new File("../../../../etc/cubemaps/dorise1/entorno3.jpg"));
            System.out.print("5");
            down = ImagePersistence.importRGBA(
                        new File("../../../../etc/cubemaps/dorise1/entorno4.jpg"));
            System.out.print("6");
            up = ImagePersistence.importRGBA(
                        new File("../../../../etc/cubemaps/dorise1/entorno5.jpg"));
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
        RGBAImageUncompressed img;

        try {
            System.out.print("Loading background: ");
            img = ImagePersistence.importRGBA(
                        new File("../../../../etc/cubemaps/dorise1/entorno0.jpg"));
            System.out.println("OK!");

            fixedBackground = new FixedBackground(camera, img);
        }
        catch (Exception e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    public SimpleMaterial defaultMaterial()
    {
        SimpleMaterial m = new SimpleMaterial();

/*
        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        m = m.withDoubleSided(false);
        m = m.withPhongExponent(100.0);
*/

        m = m.withAmbient(new ColorRgb(0, 0, 0));
        m = m.withDiffuse(new ColorRgb(1, 1, 1));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        m = m.withDoubleSided(false);
        m = m.withPhongExponent(40.0);


        return m;
    }

    public SimpleBody addThing(Geometry g)
    {
        SimpleBody thing;

        thing = new SimpleBody();
        thing.setGeometry(g);
        thing.setPosition(new Vector3Dd());
        thing.setRotation(new Matrix4x4d());
        thing.setRotationInverse(new Matrix4x4d());
        thing.setMaterial(defaultMaterial());
        thing.setName("Geometric object " + acumObject);
        scene.getSimpleBodies().add(thing);

        acumObject++;
        selectedThings.sync();
        return thing;
    }

    public boolean doIntersectionFirstHit(Ray r, RayHit info)
    {
        int i;
        double nearestDistance = Float.MAX_VALUE;
        List<SimpleBody> things = scene.getSimpleBodies();
        boolean intersected = false;
        SimpleBody gi;
        RayHit ii = new RayHit();
        for ( i = 0; i < things.size(); i++ ) {
            gi = things.get(i);
            RayHit hit = new RayHit();
            if ( gi.doIntersectionFirstHit(r, hit) && hit.ray().getT() < nearestDistance ) {
                ii.clone(hit);
                nearestDistance = hit.ray().getT();
                r = hit.ray();
                intersected = true;
            }
        }
        if ( intersected ) {
            r = r.withT(nearestDistance);
            info.clone(ii);
            return true;
        }
        return false;
    }

    /**
    Selects the nearest thing (body or light) under a pixel of the active
    camera viewport. Without `composite` the previous selection is discarded,
    with it the picked thing changes its selection state.
    Bodies are picked with their geometry and lights with a sphere of the size
    of their gizmo (see `LightPicker`).
    @param x pixel column in the viewport
    @param y pixel row in the viewport
    @param composite true to modify the current selection
    @param ro ray to be replaced
    @return the ray fired through the pixel
    */
    public Ray selectObjectWithMouse(int x, int y, boolean composite, Ray ro)
    {
        Ray r;
        SimpleBody gi;

        activeCamera.updateVectors();
        r = activeCamera.generateRay(x, y);

        Ray selectedRay = Ray.copyOf(r);

        double nearestDistance = Float.MAX_VALUE;
        int nearestBody = -1;
        int nearestLight = -1;

        int i;

        selectedThings.sync();
        selectedLights.sync();

        List<SimpleBody> things = scene.getSimpleBodies();
        for ( i = 0; i < things.size(); i++ ) {
            gi = things.get(i);
            Ray hit = gi.doIntersectionFirstHit(r);
            if ( hit != null && hit.getT() < nearestDistance ) {
                nearestDistance = hit.getT();
                nearestBody = i;
            }
        }

        List<Light> lights = scene.getLights();
        for ( i = 0; i < lights.size(); i++ ) {
            double t = LightPicker.pick(r, activeCamera, lights.get(i),
                Jogl4LightRenderer.getScale());
            if ( t >= 0 && t < nearestDistance ) {
                nearestDistance = t;
                nearestBody = -1;
                nearestLight = i;
            }
        }

        if ( !composite ) {
            selectedThings.unselectAll();
            selectedLights.unselectAll();
            selectedThings.select(nearestBody);
            selectedLights.select(nearestLight);
        }
        else {
            selectedThings.change(nearestBody);
            selectedLights.change(nearestLight);
        }
        return selectedRay;
    }

    /**
    Selects the next background, cycling over the three available ones.
    */
    public void rotateBackground()
    {
        selectedBackground++;
        if ( selectedBackground > 2 ) {
            selectedBackground = 0;
        }
    }

    public void activateSelectedBackground()
    {
        Background currentBackground;

        currentBackground = simpleBackground;
        switch ( selectedBackground ) {
          case 2:
            if ( cubemapBackground == null ) {
                buildCubemap();
            }
            if ( cubemapBackground != null ) {
                cubemapBackground.setCamera(activeCamera);
                currentBackground = cubemapBackground;
            }
            break;
          case 1:
            if ( fixedBackground == null ) {
                buildFixedmap();
            }
            if ( fixedBackground != null ) {
                currentBackground = fixedBackground;
            }
            break;
        }

        ArrayList<Background> list;
        list = scene.getBackgrounds();
        if ( list.size() < 1 ) {
            list.add(currentBackground);
        }
        else {
            list.remove(0);
            list.add(0, currentBackground);
        }
        scene.setActiveBackgroundIndex(0);
    }

    public void print()
    {
        List<SimpleBody> things = scene.getSimpleBodies();
        int i;

        System.out.println("= SCENE REPORT ============================================================");
        System.out.println("Current camera:\n" +  activeCamera);
        System.out.println("Things in scene: " + things.size());
        for ( i = 0; i < things.size(); i++ ) {
            System.out.println("  - Thing[" + i + "]: " + things.get(i).getGeometry().getClass().getName());
        }
        System.out.println("= END OF REPORT ===========================================================");
    }

    public void raytrace(RGBImageUncompressed out_Viewport)
    {
        int originalWidth;
        int originalHeight;

        originalWidth = (int)activeCamera.getViewportXSize();
        originalHeight = (int)activeCamera.getViewportYSize();
        CameraSnapshot cameraSnapshot = activeCamera.exportToCameraSnapshot(
            out_Viewport.getXSize(), out_Viewport.getYSize());

        //-----------------------------------------------------------------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();        
        SimpleRaytracer visualizationEngine;

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

        visualizationEngine = new SimpleRaytracer();
        SimpleSceneSnapshot sceneSnapshot =
            scene.exportToSimpleSceneSnapshot(cameraSnapshot, activeBackground);
        long initialTime = System.currentTimeMillis();
        visualizationEngine.execute(out_Viewport, qualityTemplate,
                                    sceneSnapshot, reporter, null);
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
