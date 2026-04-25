import java.awt.Rectangle;
import javax.swing.JFrame;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.processing.polygonClipper._Polygon2DWA;
import vsdk.toolkit.environment.LightType;

public class PolygonClippingDebuggerModel
{
    private Camera camera;
    private Light light;
    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private CameraController cameraController;

    private Polygon2D clipPolygon;
    private Polygon2D subjectPolygon;
    private Polygon2D innerPolygon;
    private Polygon2D outerPolygon;
    private _Polygon2DWA clipPolygonWA;
    private _Polygon2DWA subjectPolygonWA;

    private int testIndex;
    private boolean showReferenceFrame;
    private boolean showClipPolygon;
    private boolean showSubjectPolygon;
    private boolean showInnerPolygon;
    private boolean showOuterPolygon;
    private boolean showIntersections;
    private boolean showFilledPolygons;
    private boolean takeSnapshot;
    private int snapshotNumber;

    private boolean errorState;
    private String errorMessage;

    private JFrame mainFrame;
    private Rectangle windowedBounds;
    private boolean fullScreenMode;

    public PolygonClippingDebuggerModel()
    {
        camera = new Camera();
        camera.setPosition(new Vector3D(14, -18, 12));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3D(6, 0, 4));
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.setOrthogonalZoom(camera.getOrthogonalZoom() / 16.0);

        quality = new RendererConfiguration();
        quality.changeWires();
        qualityController = new RendererConfigurationController(quality);
        cameraController = new CameraControllerOrbiter(camera);

        light = new Light(LightType.POINT, new Vector3D(10, -20, 50),
            new ColorRgb(1, 1, 1));

        clipPolygon = null;
        subjectPolygon = null;
        innerPolygon = null;
        outerPolygon = null;
        clipPolygonWA = null;
        subjectPolygonWA = null;

        testIndex = 0;
        showReferenceFrame = true;
        showClipPolygon = true;
        showSubjectPolygon = true;
        showInnerPolygon = true;
        showOuterPolygon = true;
        showIntersections = true;
        showFilledPolygons = true;
        takeSnapshot = false;
        snapshotNumber = 1;

        errorState = false;
        errorMessage = "";

        mainFrame = null;
        windowedBounds = null;
        fullScreenMode = false;
    }

    public PolygonClippingTestCase getCurrentTestCase()
    {
        return PolygonClippingTestCases.CASES[testIndex];
    }

    public int getTotalTestCases()
    {
        return PolygonClippingTestCases.CASES.length;
    }

    public void stepTest(int delta)
    {
        int total = getTotalTestCases();
        testIndex = (testIndex + delta) % total;
        if ( testIndex < 0 ) {
            testIndex += total;
        }
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
        System.err.println("[PolygonClippingExample] " + errorMessage);
    }

    public Camera getCamera()
    {
        return camera;
    }

    public void setCamera(Camera camera)
    {
        this.camera = camera;
    }

    public Light getLight()
    {
        return light;
    }

    public void setLight(Light light)
    {
        this.light = light;
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

    public Polygon2D getClipPolygon()
    {
        return clipPolygon;
    }

    public void setClipPolygon(Polygon2D clipPolygon)
    {
        this.clipPolygon = clipPolygon;
    }

    public Polygon2D getSubjectPolygon()
    {
        return subjectPolygon;
    }

    public void setSubjectPolygon(Polygon2D subjectPolygon)
    {
        this.subjectPolygon = subjectPolygon;
    }

    public Polygon2D getInnerPolygon()
    {
        return innerPolygon;
    }

    public void setInnerPolygon(Polygon2D innerPolygon)
    {
        this.innerPolygon = innerPolygon;
    }

    public Polygon2D getOuterPolygon()
    {
        return outerPolygon;
    }

    public void setOuterPolygon(Polygon2D outerPolygon)
    {
        this.outerPolygon = outerPolygon;
    }

    public _Polygon2DWA getClipPolygonWA()
    {
        return clipPolygonWA;
    }

    public void setClipPolygonWA(_Polygon2DWA clipPolygonWA)
    {
        this.clipPolygonWA = clipPolygonWA;
    }

    public _Polygon2DWA getSubjectPolygonWA()
    {
        return subjectPolygonWA;
    }

    public void setSubjectPolygonWA(_Polygon2DWA subjectPolygonWA)
    {
        this.subjectPolygonWA = subjectPolygonWA;
    }

    public int getTestIndex()
    {
        return testIndex;
    }

    public void setTestIndex(int testIndex)
    {
        this.testIndex = testIndex;
    }

    public boolean isShowReferenceFrame()
    {
        return showReferenceFrame;
    }

    public void setShowReferenceFrame(boolean showReferenceFrame)
    {
        this.showReferenceFrame = showReferenceFrame;
    }

    public boolean isShowClipPolygon()
    {
        return showClipPolygon;
    }

    public void setShowClipPolygon(boolean showClipPolygon)
    {
        this.showClipPolygon = showClipPolygon;
    }

    public boolean isShowSubjectPolygon()
    {
        return showSubjectPolygon;
    }

    public void setShowSubjectPolygon(boolean showSubjectPolygon)
    {
        this.showSubjectPolygon = showSubjectPolygon;
    }

    public boolean isShowInnerPolygon()
    {
        return showInnerPolygon;
    }

    public void setShowInnerPolygon(boolean showInnerPolygon)
    {
        this.showInnerPolygon = showInnerPolygon;
    }

    public boolean isShowOuterPolygon()
    {
        return showOuterPolygon;
    }

    public void setShowOuterPolygon(boolean showOuterPolygon)
    {
        this.showOuterPolygon = showOuterPolygon;
    }

    public boolean isShowIntersections()
    {
        return showIntersections;
    }

    public void setShowIntersections(boolean showIntersections)
    {
        this.showIntersections = showIntersections;
    }

    public boolean isShowFilledPolygons()
    {
        return showFilledPolygons;
    }

    public void setShowFilledPolygons(boolean showFilledPolygons)
    {
        this.showFilledPolygons = showFilledPolygons;
    }

    public boolean isTakeSnapshot()
    {
        return takeSnapshot;
    }

    public void setTakeSnapshot(boolean takeSnapshot)
    {
        this.takeSnapshot = takeSnapshot;
    }

    public int getSnapshotNumber()
    {
        return snapshotNumber;
    }

    public void setSnapshotNumber(int snapshotNumber)
    {
        this.snapshotNumber = snapshotNumber;
    }

    public boolean isErrorState()
    {
        return errorState;
    }

    public void setErrorState(boolean errorState)
    {
        this.errorState = errorState;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
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
