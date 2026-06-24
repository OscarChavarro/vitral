package vitral.application;

// Java classes
import java.util.ArrayList;
import java.util.Random;

// VSDK classes
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.camera.CameraSnapshot;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.render.raytracing.SimpleRaytracer;
import vsdk.toolkit.gui.feedback.ProgressMonitorConsole;
import vsdk.toolkit.environment.background.SimpleBackground;
import vsdk.toolkit.environment.background.Background;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
This should evolve to the point of being equivalent to "Scene" class used at
"SceneEditorApplication" version of application for desktop computers.
*/
public class Scene
{
    public Camera camera;
    public SimpleScene scene;
    public int selectedObjectIndex = -1;
    public SimpleBackground simpleBackground;
    private Random randomNumberGenerator;

    // Others
    private int acumObject = 1;

    public Scene()
    {
        scene = new SimpleScene();

        randomNumberGenerator = new Random(System.currentTimeMillis());
        simpleBackground = new SimpleBackground();
        simpleBackground.setColor(0.49, 0.49, 0.49);

        Vector3Dd p = new Vector3Dd(0, -5, 5);
        Matrix4x4d R = new Matrix4x4d();

        R = R.eulerAnglesRotation(Math.toRadians(90.0), Math.toRadians(-45.0), 0);

        camera = new Camera();
        camera.setPosition(p);
        camera.setRotation(R);
        camera.setFov(45.0);
    }

    public void selectObjectWithMouse(int x, int y)
    {
        Ray r;
        SimpleBody gi;

        camera.updateVectors();
        r = camera.generateRay(x, y);

        double nearestDistance = Float.MAX_VALUE;

        int i;

        selectedObjectIndex = -1;
        ArrayList<SimpleBody> things = scene.getSimpleBodies();

        for ( i = 0; i < things.size(); i++ ) {
            gi = things.get(i);
            Ray hit = gi.doIntersectionFirstHit(r);
            if ( hit != null && hit.getT() < nearestDistance ) {
                nearestDistance = hit.getT();
                r = hit;
                selectedObjectIndex = i;
            }
        }
    }

    public void insertSphereWithMouse(int x, int y)
    {
        Ray r;

        camera.updateVectors();
        r = camera.generateRay(x, y);

        Vector3Dd p;

        p = r.getOrigin().add(r.getDirection().multiply(10.0));

        SimpleBody b;
        Sphere s;

        s = new Sphere(0.3);
        b = addThing(s, randomColor());
        b.setPosition(p);
    }

    private Vector3Dd randomPosition()
    {
        Vector3Dd p;

        p = new Vector3Dd();
/*
        p.x = (randomNumberGenerator.nextDouble() * 2.0) - 1.0;
        p.y = (randomNumberGenerator.nextDouble() * 2.0) - 1.0;
        p.z = (randomNumberGenerator.nextDouble() * 2.0) - 1.0;
*/
        double phi;
        double theta;
        double r;

        phi = (randomNumberGenerator.nextDouble()) * Math.PI;
        theta = (randomNumberGenerator.nextDouble()*2.0) * Math.PI;
        r = (randomNumberGenerator.nextDouble()*0.5) + 1.2;

        p.setSphericalCoordinates(r, theta, phi);

        return p;
    }

    private ColorRgb randomColor()
    {
        ColorRgb c;
        c = new ColorRgb();
        c.r = (randomNumberGenerator.nextDouble() / 2.0) + 0.5;
        c.g = (randomNumberGenerator.nextDouble() / 2.0) + 0.5;
        c.b = (randomNumberGenerator.nextDouble() / 2.0) + 0.5;
        return c;
    }

    public void addRandomSphere()
    {
        SimpleBody b;

        b = addThing(new Sphere(0.4), randomColor());
        b.setPosition(randomPosition());
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

    public SimpleBody addThing(Geometry g, ColorRgb c)
    {
        SimpleBody thing;
        thing = addThing(g);
        thing.setMaterial(thing.getMaterial().withDiffuse(c));
        return thing;
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
        //selectedThings.sync();
        return thing;
    }

    public void raytrace(RGBImageUncompressed out_Viewport, RendererConfiguration quality)
    {
        int originalWidth;
        int originalHeight;

        originalWidth = (int)camera.getViewportXSize();
        originalHeight = (int)camera.getViewportYSize();
        CameraSnapshot cameraSnapshot = camera.exportToCameraSnapshot(
            out_Viewport.getXSize(), out_Viewport.getYSize());

        //-----------------------------------------------------------------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();        
        SimpleRaytracer visualizationEngine;

        Background activeBackground;
/*
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
*/
        activeBackground = simpleBackground;
        visualizationEngine = new SimpleRaytracer();
        SimpleSceneSnapshot sceneSnapshot =
            scene.exportToSimpleSceneSnapshot(cameraSnapshot, activeBackground);
        long initialTime = System.currentTimeMillis();
        visualizationEngine.execute(out_Viewport, quality,
                                    sceneSnapshot, reporter, null);
        long finalTime = System.currentTimeMillis();
        System.out.println("Image generated in " + (finalTime-initialTime) + " miliseconds.");

/*
        File fd = new File("./output.jpg");

        System.out.print("Exporting result image to file: ");
        if ( !ImagePersistence.exportJPG(fd, out_Viewport) )
        {
            System.err.println("Error grabando la imagen!!");
            System.exit(1);
        }
        System.out.println(" OK!");
        System.out.println("An image has been created in the file output.jpg");
*/
        //-----------------------------------------------------------------
        camera.updateViewportResize(originalWidth, originalHeight);
    }

}
