package models;

import java.awt.Rectangle;

import javax.swing.JFrame;

import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;

public class TangibleInterfaceGizmosModel
{
    private static final double MIN_RADIUS = 0.1;
    private static final double MAX_RADIUS = 2.0;
    private static final double RADIUS_STEP = 0.1;
    private static final double MIN_BASE_HEIGHT = 0.1;
    private static final double MAX_BASE_HEIGHT = 3.0;
    private static final double BASE_HEIGHT_STEP = 0.5;

    private GizmoNames solidModelName = GizmoNames.CUBE_PART_1;
    private final Camera camera;
    private final SimpleMaterial material;
    private final Light light1;
    private final Light light2;
    private final RendererConfiguration quality;
    private final RendererConfigurationController qualityController;
    private final CameraController cameraController;
    private PolyhedralBoundedSolid solid;
    private int faceIndex = -2;
    private boolean debugVertices = false;
    private GLCanvas canvas;
    private boolean showCoordinateSystem = true;
    private boolean errorState = false;
    private String errorMessage = "";
    private double innerRadius = 7.86/2; // 3.04/2;
    private double outerRadius = 7.86/2 + 1.6; // 3.04/2 + 1.2;
    private double baseHeight = 1.0;
    private JFrame mainFrame;
    private Rectangle windowedBounds;
    private boolean fullScreenMode = false;

    public TangibleInterfaceGizmosModel()
    {
        camera = new Camera();
        camera.setPosition(new Vector3Dd(0.75, 0.50, 2.00));
        Matrix4x4d rotationMatrix = new Matrix4x4d();
        rotationMatrix = rotationMatrix.eulerAnglesRotation(
            Math.toRadians(90), Math.toRadians(-90), 0);
        camera.setRotation(rotationMatrix);

        quality = new RendererConfiguration();
        quality.changeWires();
	quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
        qualityController = new RendererConfigurationController(quality);
        cameraController = new CameraControllerAquynza(camera);

        material = defaultMaterial();
        light1 = new PointLight(new Vector3Dd(3, -3, 2),
            new ColorRgb(1, 1, 1));
        light2 = new PointLight(new Vector3Dd(-2, 5, -2),
            new ColorRgb(0.9, 0.5, 0.5));
        light1.setId(0);
        light2.setId(1);
    }

    private SimpleMaterial defaultMaterial()
    {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(new ColorRgb(0.5, 0.5, 0.9));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        m = m.withDoubleSided(false);
        m = m.withPhongExponent(100);
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
        System.err.println("[TangibleInterfaceGizmoCreator] " + errorMessage);
    }

    public GizmoNames getSolidModelName()
    {
        return solidModelName;
    }

    public int getFaceCount()
    {
        if ( solid == null || solid.getPolygonsList() == null ) {
            return 0;
        }
        return solid.getPolygonsList().size();
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

    public void setSolidModelName(GizmoNames solidModelName)
    {
        this.solidModelName = solidModelName;
    }

    public Camera getCamera()
    {
        return camera;
    }

    public SimpleMaterial getMaterial()
    {
        return material;
    }

    public Light getLight1()
    {
        return light1;
    }

    public Light getLight2()
    {
        return light2;
    }

    public RendererConfiguration getQuality()
    {
        return quality;
    }

    public RendererConfigurationController getQualityController()
    {
        return qualityController;
    }

    public CameraController getCameraController()
    {
        return cameraController;
    }

    public PolyhedralBoundedSolid getSolid()
    {
        return solid;
    }

    public void setSolid(PolyhedralBoundedSolid solid)
    {
        this.solid = solid;
        clampFaceIndex();
    }

    public int getFaceIndex()
    {
        return faceIndex;
    }

    public void setFaceIndex(int faceIndex)
    {
        this.faceIndex = faceIndex;
    }

    public boolean notDebugVertices()
    {
        return !debugVertices;
    }

    public void setDebugVertices(boolean debugVertices)
    {
        this.debugVertices = debugVertices;
    }

    public GLCanvas getCanvas()
    {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas)
    {
        this.canvas = canvas;
    }

    public boolean isShowCoordinateSystem()
    {
        return showCoordinateSystem;
    }

    public void setShowCoordinateSystem(boolean showCoordinateSystem)
    {
        this.showCoordinateSystem = showCoordinateSystem;
    }

    public boolean isErrorState()
    {
        return errorState;
    }

    public double getInnerRadius()
    {
        return innerRadius;
    }

    public void setInnerRadius(double innerRadius)
    {
        this.innerRadius = innerRadius;
    }

    public double getOuterRadius()
    {
        return outerRadius;
    }

    public void setOuterRadius(double outerRadius)
    {
        this.outerRadius = outerRadius;
    }

    public double getBaseHeight()
    {
        return baseHeight;
    }

    public void setBaseHeight(double baseHeight)
    {
        this.baseHeight = baseHeight;
    }

    public void decreaseInnerRadius()
    {
        innerRadius = Math.max(MIN_RADIUS, innerRadius - RADIUS_STEP);
        if ( innerRadius > outerRadius) {
            innerRadius = outerRadius;
        }
    }

    public void increaseInnerRadius()
    {
        innerRadius = Math.min(MAX_RADIUS, innerRadius + RADIUS_STEP);
        if ( innerRadius > outerRadius) {
            innerRadius = outerRadius;
        }
    }

    public void decreaseOutterRadius()
    {
        outerRadius = Math.max(innerRadius, outerRadius - RADIUS_STEP);
        outerRadius = Math.min(MAX_RADIUS, outerRadius);
    }

    public void increaseOutterRadius()
    {
        outerRadius = Math.min(MAX_RADIUS, outerRadius + RADIUS_STEP);
        if ( outerRadius < innerRadius ) {
            outerRadius = innerRadius;
        }
    }

    public void decreaseBaseHeight()
    {
        baseHeight = Math.max(MIN_BASE_HEIGHT, baseHeight - BASE_HEIGHT_STEP);
    }

    public void increaseBaseHeight()
    {
        baseHeight = Math.min(MAX_BASE_HEIGHT, baseHeight + BASE_HEIGHT_STEP);
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
