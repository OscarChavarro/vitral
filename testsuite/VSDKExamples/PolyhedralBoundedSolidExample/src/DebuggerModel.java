import java.awt.Rectangle;
import javax.swing.JFrame;

import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;

public class DebuggerModel
{
    public Camera camera;
    public Material material;
    public Light light1;
    public Light light2;
    public PolyhedralBoundedSolid solid;
    public int faceIndex = -2;
    public int edgeIndex = -2;
    public boolean debugVertices = false;

    public RendererConfiguration quality;
    public RendererConfigurationController qualityController;
    public CameraController cameraController;
    public GLCanvas canvas;
    public int solidType = 23;
    public int csgOperation = 0;
    public int csgSample = 5;
    public boolean debugEdges = false;
    public boolean showCoordinateSystem = true;
    public boolean debugCsg = false;

    public JFrame mainFrame;
    public Rectangle windowedBounds;
    public boolean fullScreenMode = false;

    public DebuggerModel()
    {
        camera = new Camera();
        camera.setPosition(new Vector3D(2, -1, 2));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(135), Math.toRadians(-35), 0);
        camera.setRotation(R);

        quality = new RendererConfiguration();
        qualityController = new RendererConfigurationController(quality);
        cameraController = new CameraControllerAquynza(camera);

        material = defaultMaterial();
        light1 = new Light(Light.POINT, new Vector3D(3, -3, 2), new ColorRgb(1, 1, 1));
        light2 = new Light(Light.POINT, new Vector3D(-2, 5, -2), new ColorRgb(0.9, 0.5, 0.5));
    }

    private Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.5, 0.9));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        m.setPhongExponent(100);
        return m;
    }
}
