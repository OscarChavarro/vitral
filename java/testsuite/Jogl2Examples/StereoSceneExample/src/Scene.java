import java.util.ArrayList;
import java.io.File;

import vsdk.toolkit.common.color.ColorRgb;                     // Model elements
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;  // Persistence elements
import vsdk.toolkit.environment.light.PointLight;

public class Scene
{
    public Camera camera;
    public Camera activeCamera;
    public ArrayList<SimpleBody> bodies;
    public ArrayList<Light> lights;
    public RendererConfiguration qualitySelection;
    public double eyeDistance;
    public double eyeTorsionAngle;

    public Scene()
    {
        //-----------------------------------------------------------------
        camera = new Camera();
        camera.setNearPlaneDistance(1);
        camera.setFarPlaneDistance(60.0);
        Vector3Dd v = new Vector3Dd(0, -6, 0);
        Matrix4x4d R1 = new Matrix4x4d();
        Matrix4x4d R2 = new Matrix4x4d();
        R1 = R1.axisRotation(Math.PI/2, 0, 1, 0);
        R2 = R2.axisRotation(Math.PI/2, 0, 0, 1);
        camera.setPosition(v);
        //camera.setRotation(R2.multiply(R1));
        camera.setRotation(R2);

        activeCamera = camera;

        //-----------------------------------------------------------------
        bodies = new ArrayList<SimpleBody>();

        SimpleBody b;

        SimpleScene scene = new SimpleScene();
        try {
            File file = new File("../../../../etc/geometry/cow.obj");
            EnvironmentPersistence.importEnvironment(file, scene);
            int i;
            for ( i = 0; i < scene.getSimpleBodies().size(); i++ ) {
                b = scene.getSimpleBodies().get(i);
                b.setMaterial(b.getMaterial().withDoubleSided(false));
                b.setScale(new Vector3Dd(0.5, 0.5, 0.5));
                bodies.add(b);
            }
        }
        catch ( Exception ex ) {
            System.err.println("Failed to read file.");
            ex.printStackTrace();
            System.exit(0);
        }

        //-----------------------------------------------------------------
        lights = new ArrayList<Light>();
        Light l;
        l = new PointLight(
                      new Vector3Dd(5, -5, 10),
                      new ColorRgb(1, 1, 1));
        lights.add(l);

        //-----------------------------------------------------------------
        qualitySelection = new RendererConfiguration();
        eyeDistance = 0.06;
        eyeTorsionAngle = 0.00;
    }

    private SimpleMaterial defaultMaterial(ColorRgb d)
    {
        SimpleMaterial m;

        m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0, 0, 0));
        m = m.withDiffuse(new ColorRgb(d));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        m = m.withPhongExponent(80);
        m = m.withDoubleSided(false);
        return m;
    }
}
