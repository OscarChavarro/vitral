import java.util.ArrayList;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.scene.SimpleBody;

public class Scene
{
    public Camera camera;
    public Camera activeCamera;
    public ArrayList<SimpleBody> bodies;
    public ArrayList<Light> lights;
    public RendererConfiguration qualitySelection;
    public double eyeDistance;

    public Scene()
    {
        //-----------------------------------------------------------------
        camera = new Camera();
        camera.setNearPlaneDistance(1);
        camera.setFarPlaneDistance(60.0);
        Vector3D v = new Vector3D(0, -6, 0);
        Matrix4x4 R1 = new Matrix4x4();
        Matrix4x4 R2 = new Matrix4x4();
        R1.axisRotation(Math.PI/2, 0, 1, 0);
        R2.axisRotation(Math.PI/2, 0, 0, 1);
        camera.setPosition(v);
        //camera.setRotation(R2.multiply(R1));
        camera.setRotation(R2);

        activeCamera = camera;

        //-----------------------------------------------------------------
        bodies = new ArrayList<SimpleBody>();

        SimpleBody b;

        b = new SimpleBody();
        b.setGeometry(new Sphere(1.0));
        b.setMaterial(defaultMaterial(new ColorRgb(0.7, 0.7, 0.7)));
        bodies.add(b);

        //-----------------------------------------------------------------
        lights = new ArrayList<Light>();
        Light l;
        l = new Light(Light.POINT, 
                      new Vector3D(5, -5, 10), 
                      new ColorRgb(1, 1, 1));
        lights.add(l);

        //-----------------------------------------------------------------
        qualitySelection = new RendererConfiguration();
        eyeDistance = 0.06;
    }

    private Material defaultMaterial(ColorRgb d)
    {
        Material m;

        m = new Material();
        m.setAmbient(new ColorRgb(0, 0, 0));
        m.setDiffuse(new ColorRgb(d));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setPhongExponent(80);
        m.setDoubleSided(false);
        return m;
    }
}
