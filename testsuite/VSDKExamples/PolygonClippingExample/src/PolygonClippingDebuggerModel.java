import java.awt.Rectangle;
import javax.swing.JFrame;

import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Polygon2D;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.processing._Polygon2DWA;

public class PolygonClippingDebuggerModel
{
    public final Camera camera;
    public final Light light;
    public final RendererConfiguration quality;
    public final RendererConfigurationController qualityController;
    public final CameraController cameraController;

    public GLCanvas canvas;
    public Polygon2D clipPolygon;
    public Polygon2D subjectPolygon;
    public Polygon2D innerPolygon;
    public Polygon2D outerPolygon;
    public _Polygon2DWA clipPolygonWA;
    public _Polygon2DWA subjectPolygonWA;

    public int testIndex;
    public boolean showReferenceFrame;
    public boolean showClipPolygon;
    public boolean showSubjectPolygon;
    public boolean showInnerPolygon;
    public boolean showOuterPolygon;
    public boolean showIntersections;
    public boolean showFilledPolygons;
    public boolean takeSnapshot;
    public int snapshotNumber;

    public boolean errorState;
    public String errorMessage;

    public JFrame mainFrame;
    public Rectangle windowedBounds;
    public boolean fullScreenMode;

    public PolygonClippingDebuggerModel()
    {
        camera = new Camera();
        camera.setPosition(new Vector3D(14, -18, 12));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3D(6, 0, 4));

        quality = new RendererConfiguration();
        quality.changeWires();
        qualityController = new RendererConfigurationController(quality);
        cameraController = new CameraControllerAquynza(camera);

        light = new Light(Light.POINT, new Vector3D(10, -20, 50),
            new ColorRgb(1, 1, 1));

        canvas = null;
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
}
