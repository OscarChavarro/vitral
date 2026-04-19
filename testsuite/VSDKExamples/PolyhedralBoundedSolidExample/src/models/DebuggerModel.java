package models;

// AWT/Swing classes
import java.awt.Rectangle;
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.awt.GLCanvas;

// Vitral classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.RendererConfigurationController;

public class DebuggerModel
{
    private static final int MIN_SUBDIVISION_CIRCUMFERENCE = 3;
    private static final int MIN_SUBDIVISION_HEIGHT = 1;

    private SolidModelNames solidModelName = SolidModelNames.CSG_LAMP_SHELL;
    private int subdivisionCircumference = 16;
    private int subdivisionHeight = 8;

    private Camera camera;
    private Material material;
    private Light light1;
    private Light light2;
    private PolyhedralBoundedSolid solid;
    private int faceIndex = -2;
    private int edgeIndex = -2;
    private boolean debugVertices = false;

    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private CameraController cameraController;
    private GLCanvas canvas;
    private CsgOperationNames csgOperation = CsgOperationNames.UNION;
    private CsgSampleNames csgSample = CsgSampleNames.MANT1988_15_2_HOLED;
    private boolean debugEdges = false;
    private boolean showCoordinateSystem = true;
    private boolean debugCsg = false;
    private boolean errorState = false;
    private String errorMessage = "";

    private JFrame mainFrame;
    private Rectangle windowedBounds;
    private boolean fullScreenMode = false;

    public DebuggerModel()
    {
        camera = new Camera();
        camera.setPosition(new Vector3D(2, -1, 2));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(135), Math.toRadians(-35), 0);
        camera.setRotation(R);

        quality = new RendererConfiguration();
        quality.changeWires();
        qualityController = new RendererConfigurationController(quality);
        cameraController = new CameraControllerOrbiter(camera);

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

    public void clearErrorState()
    {
        errorState = false;
        errorMessage = "";
    }

    public void setErrorState(String message)
    {
        errorState = true;
        errorMessage = message;
        System.err.println("[PolyhedralBoundedSolidExample] " + errorMessage);
    }

    public void clampSubdivisions()
    {
        if ( subdivisionCircumference < MIN_SUBDIVISION_CIRCUMFERENCE ) {
            subdivisionCircumference = MIN_SUBDIVISION_CIRCUMFERENCE;
        }
        if ( subdivisionHeight < MIN_SUBDIVISION_HEIGHT ) {
            subdivisionHeight = MIN_SUBDIVISION_HEIGHT;
        }
    }

    public int getFaceCount()
    {
        if ( solid == null || solid.polygonsList == null ) {
            return 0;
        }
        return solid.polygonsList.size();
    }

    public void clampFaceIndex()
    {
        if ( faceIndex < -2 ) {
            faceIndex = -2;
            return;
        }

        int totalFaces = getFaceCount();
        int maxFaceIndex = totalFaces - 1;
        if ( faceIndex > maxFaceIndex ) {
            faceIndex = maxFaceIndex;
        }
    }

    public SolidModelNames getSolidModelName()
    {
        return solidModelName;
    }

    public void setSolidModelName(SolidModelNames solidModelName)
    {
        this.solidModelName = solidModelName;
    }

    public int getSubdivisionCircumference()
    {
        return subdivisionCircumference;
    }

    public void setSubdivisionCircumference(int subdivisionCircumference)
    {
        this.subdivisionCircumference = subdivisionCircumference;
    }

    public int getSubdivisionHeight()
    {
        return subdivisionHeight;
    }

    public void setSubdivisionHeight(int subdivisionHeight)
    {
        this.subdivisionHeight = subdivisionHeight;
    }

    public Camera getCamera()
    {
        return camera;
    }

    public void setCamera(Camera camera)
    {
        this.camera = camera;
    }

    public Material getMaterial()
    {
        return material;
    }

    public void setMaterial(Material material)
    {
        this.material = material;
    }

    public Light getLight1()
    {
        return light1;
    }

    public void setLight1(Light light1)
    {
        this.light1 = light1;
    }

    public Light getLight2()
    {
        return light2;
    }

    public void setLight2(Light light2)
    {
        this.light2 = light2;
    }

    public PolyhedralBoundedSolid getSolid()
    {
        return solid;
    }

    public void setSolid(PolyhedralBoundedSolid solid)
    {
        this.solid = solid;
    }

    public int getFaceIndex()
    {
        return faceIndex;
    }

    public void setFaceIndex(int faceIndex)
    {
        this.faceIndex = faceIndex;
    }

    public int getEdgeIndex()
    {
        return edgeIndex;
    }

    public void setEdgeIndex(int edgeIndex)
    {
        this.edgeIndex = edgeIndex;
    }

    public boolean isDebugVertices()
    {
        return debugVertices;
    }

    public void setDebugVertices(boolean debugVertices)
    {
        this.debugVertices = debugVertices;
    }

    public RendererConfiguration getQuality()
    {
        return quality;
    }

    public void setQuality(RendererConfiguration quality)
    {
        this.quality = quality;
    }

    public RendererConfigurationController getQualityController()
    {
        return qualityController;
    }

    public void setQualityController(RendererConfigurationController qualityController)
    {
        this.qualityController = qualityController;
    }

    public CameraController getCameraController()
    {
        return cameraController;
    }

    public void setCameraController(CameraController cameraController)
    {
        this.cameraController = cameraController;
    }

    public GLCanvas getCanvas()
    {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas)
    {
        this.canvas = canvas;
    }

    public CsgOperationNames getCsgOperation()
    {
        return csgOperation;
    }

    public void setCsgOperation(CsgOperationNames csgOperation)
    {
        this.csgOperation = csgOperation;
    }

    public CsgSampleNames getCsgSample()
    {
        return csgSample;
    }

    public void setCsgSample(CsgSampleNames csgSample)
    {
        this.csgSample = csgSample;
    }

    public boolean isDebugEdges()
    {
        return debugEdges;
    }

    public void setDebugEdges(boolean debugEdges)
    {
        this.debugEdges = debugEdges;
    }

    public boolean isShowCoordinateSystem()
    {
        return showCoordinateSystem;
    }

    public void setShowCoordinateSystem(boolean showCoordinateSystem)
    {
        this.showCoordinateSystem = showCoordinateSystem;
    }

    public boolean isDebugCsg()
    {
        return debugCsg;
    }

    public void setDebugCsg(boolean debugCsg)
    {
        this.debugCsg = debugCsg;
    }

    public boolean isErrorState()
    {
        return errorState;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public JFrame getMainFrame()
    {
        return mainFrame;
    }

    public void setMainFrame(JFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }

    public Rectangle getWindowedBounds()
    {
        return windowedBounds;
    }

    public void setWindowedBounds(Rectangle windowedBounds)
    {
        this.windowedBounds = windowedBounds;
    }

    public boolean isFullScreenMode()
    {
        return fullScreenMode;
    }

    public void setFullScreenMode(boolean fullScreenMode)
    {
        this.fullScreenMode = fullScreenMode;
    }
}
