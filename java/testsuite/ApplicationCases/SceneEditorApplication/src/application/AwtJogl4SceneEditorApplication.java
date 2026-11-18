package application;

// Java classes
import java.util.List;
import java.awt.GraphicsEnvironment;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

// VSDK Classes
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.processing.ImageProcessing;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent2RayGizmoMapper;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceNetworkClient;

// Application classes
import model.GuiState;
import model.Scene;
import model.ApplicationModel;
import model.RenderTechnology;
import render.RaytracedViewportFrames;
import gui.awt.AwtApplicationHost;
import gui.awt.AwtApplicationModel;
import application.mcp.AwtJogl4VitralEditorMCP;

public class AwtJogl4SceneEditorApplication implements AwtApplicationHost {
    // Application model
    private ApplicationModel applicationModel;
    /// Raytraced images of the viewports in CPU render mode
    private final RaytracedViewportFrames raytracedViewportFrames =
        new RaytracedViewportFrames();

    // Application GUI
    private AwtApplicationModel awtModel;
    private AwtJogl4GuiController awtGuiController;
    private AwtJogl4ApplicationController jogl4Controller;
    private static final String DEFAULT_TANGIBLE_SERVICE_URL = "ws://127.0.0.1:8090/v1/values";
    private static final String RAY_CUBE_TANGIBLE_ELEMENT_ID = "rayCube1";
    private String tangibleServiceUrl = DEFAULT_TANGIBLE_SERVICE_URL;
    private TangibleInterfaceNetworkClient tangibleInterfaceClient;

    void setLookAndFeel(String lookAndFeel)
    {
        awtGuiController.setLookAndFeel(lookAndFeel);
    }

    void setGuiLanguage(String lang)
    {
        awtGuiController.setGuiLanguage(lang);
    }

    public List<String> getGuiLanguages()
    {
        return GuiState.listLanguages();
    }

    public String getCurrentGuiLanguage()
    {
        return applicationModel.getGuiState().getCurrentLanguage();
    }

    public boolean setGuiLanguageById(String language)
    {
        if ( language == null || !getGuiLanguages().contains(language) ) {
            return false;
        }
        setGuiLanguage(GuiState.languageFile(language));
        return true;
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        applicationModel = new ApplicationModel();
        applicationModel.setScene(new Scene());

        applicationModel.setRaytracedImage(new RGBImageUncompressed());
        applicationModel.setRaytracedImageWidth(320);
        applicationModel.setRaytracedImageHeight(240);

        applicationModel.setPalette(null);
        try {
            applicationModel.setPalette(
                RGBColorPalettePersistence.importGimpPalette(
                    new java.io.FileReader("../../../../etc/palettes/Cranes.gpl")));
        }
        catch ( Exception e ) {
            System.err.println(e);
            System.exit(0);
        }

        applicationModel.setVisualDebugRay(new Ray(new Vector3Dd(0, -3, 0), new Vector3Dd(0, 1, 0)));
        applicationModel.setVisualDebugRayLevels(2);
        applicationModel.setWithVisualDebugRay(false);
        jogl4Controller = new AwtJogl4ApplicationController(applicationModel);
        awtModel = new AwtApplicationModel();
        awtModel.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        awtGuiController = new AwtJogl4GuiController(this, awtModel, jogl4Controller);
    }

    @Override
    public final void createGUI()
    {
        awtGuiController.createGUI();
    }

    @Override
    public void destroyGUI()
    {
        awtGuiController.destroyGUI();
    }

    AwtJogl4SceneEditorApplication(String[] args) {
        createModel();
        processArguments(args);
        createGUI();

        // The automation service is always started (the former "-s" argument
        // is still accepted, but no longer needed). It starts its own
        // listener thread, which keeps it alive
        new AwtJogl4VitralEditorMCP(this);
    }

    private void processArguments(String[] args) {
        if ( args == null ) {
            return;
        }
        for ( int i = 0; i < args.length; i++ ) {
            if ( "-tangibleServer".equals(args[i]) && i + 1 < args.length ) {
                setTangibleServiceUrl(args[++i]);
            }
        }
    }

    public String getTangibleServiceUrl() {
        return tangibleServiceUrl;
    }

    public void connectTangibleInterface(String serviceUrl,
        Consumer<Boolean> connectionListener) {
        setTangibleServiceUrl(serviceUrl);
        if ( tangibleInterfaceClient != null ) {
            tangibleInterfaceClient.disconnect();
        }
        tangibleInterfaceClient = new TangibleInterfaceNetworkClient(tangibleServiceUrl);
        tangibleInterfaceClient.addListener(this::tangibleInterfaceEventReceived);
        tangibleInterfaceClient.run(connectionListener);
    }

    private void setTangibleServiceUrl(String serviceUrl) {
        if ( serviceUrl != null && !serviceUrl.trim().isEmpty() ) {
            tangibleServiceUrl = serviceUrl.trim();
        }
    }

    private void tangibleInterfaceEventReceived(TangibleInterfaceEvent event) {
        if ( event == null || !RAY_CUBE_TANGIBLE_ELEMENT_ID.equals(event.getId()) ) {
            return;
        }
        applicationModel.setWithVisualDebugRay(true);
        applicationModel.getRayGizmo().setRollIndicatorEnabled(true);
        new TangibleInterfaceEvent2RayGizmoMapper(applicationModel.getActiveCamera())
            .map(event, applicationModel.getRayGizmo());
        SwingUtilities.invokeLater(this::repaintDrawingArea);
    }

    private void prepareRaytracedImage()
    {
        applicationModel.getRaytracedImage().init(
            applicationModel.getRaytracedImageWidth(),
            applicationModel.getRaytracedImageHeight());
        if ( applicationModel.getScene().selectedBackground == 1 ) {
            ImageProcessing.resize(
                applicationModel.getScene().fixedBackground.getImage(),
                applicationModel.getRaytracedImage());
        }
    }

    @Override
    public void doRaytracingImage()
    {
        // The buffers shared with the viewports in CPU mode are overwritten
        raytracedViewportFrames.invalidate();
        prepareRaytracedImage();
        applicationModel.getScene().raytrace(applicationModel.getRaytracedImage());
    }

    /**
    Raytraces the viewport being drawn (the active camera of the scene) into
    the raytraced image and depth of the model, which are allocated again
    only when the size of the viewport changes (they are megabytes, raytraced
    tens of times per second). See `RaytracedViewportFrames` for the work
    it avoids.
    */
    @Override
    public void doViewportRaytracingImage()
    {
        int width = applicationModel.getRaytracedImageWidth();
        int height = applicationModel.getRaytracedImageHeight();
        RGBImageUncompressed image = applicationModel.getRaytracedImage();
        ZBuffer depth = applicationModel.getRaytracedDepth();

        if ( image.getXSize() != width || image.getYSize() != height ) {
            image.init(width, height);
            raytracedViewportFrames.invalidate();
        }
        if ( depth == null || depth.getXSize() != width ||
             depth.getYSize() != height ) {
            depth = new ZBuffer(width, height);
            applicationModel.setRaytracedDepth(depth);
            raytracedViewportFrames.invalidate();
        }
        raytracedViewportFrames.raytrace(applicationModel.getScene(),
            applicationModel.getDrawingArea(), image, depth);
    }

    @Override
    public void setRenderTechnology(RenderTechnology technology)
    {
        if ( technology == null ||
             technology == applicationModel.getDrawingArea().getRenderTechnology() ) {
            return;
        }
        applicationModel.getDrawingArea().setRenderTechnology(technology);
        // After the menu that requested it has finished with the old canvas
        SwingUtilities.invokeLater(() -> {
            destroyGUI();
            createGUI();
        });
    }

    /**
    @return the technology presenting the viewport set
    */
    public RenderTechnology getRenderTechnology()
    {
        return applicationModel.getDrawingArea().getRenderTechnology();
    }

    @Override
    public void closeApplication()
    {
        System.exit(0);
    }

    @Override
    public ApplicationModel getApplicationModel()
    {
        return applicationModel;
    }

    public AwtJogl4ApplicationController getJogl4Controller()
    {
        return jogl4Controller;
    }

    @Override
    public void repaintDrawingArea()
    {
        if ( jogl4Controller != null ) {
            jogl4Controller.repaint();
        }
    }

    @Override
    public void reportTargetToModifyPanel()
    {
        if ( jogl4Controller != null ) {
            jogl4Controller.reportTargetToModifyPanel();
        }
    }

    @Override
    public AwtApplicationModel getAwtModel()
    {
        return awtModel;
    }

    /**
    The editor needs a graphical display: without one (i.e. on Unix with no
    DISPLAY variable defined), the reason is reported in a short message,
    instead of the stack trace of the `HeadlessException` AWT would throw
    when creating the first window, and the application ends.
    */
    private static void exitIfNoDisplayIsAvailable()
    {
        if ( !GraphicsEnvironment.isHeadless() ) {
            return;
        }
        String display = System.getenv("DISPLAY");

        if ( display == null || display.isEmpty() ) {
            System.err.println("ERROR: No graphical display is available: the " +
                "DISPLAY environment variable is not defined.");
            System.err.println("The scene editor needs a display. Define DISPLAY " +
                "(i.e. export DISPLAY=:0), connect with X11 forwarding " +
                "(ssh -X), or use a virtual display (i.e. xvfb-run ./run.sh).");
        }
        else {
            System.err.println("ERROR: No graphical display is available (Java " +
                "is running in headless mode, i.e. with -Djava.awt.headless=true).");
            System.err.println("The scene editor needs a display: run it " +
                "without the headless mode.");
        }
        System.exit(1);
    }

    public static void main(String[] args) {
        exitIfNoDisplayIsAvailable();
        AwtJogl4MainThread mainThread = new AwtJogl4MainThread(args);
        SwingUtilities.invokeLater(mainThread);
    }
}
