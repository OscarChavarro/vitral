package application;

// Java classes
import java.util.List;
import javax.swing.SwingUtilities;

// VSDK Classes
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.processing.ImageProcessing;

// Application classes
import model.GuiState;
import model.Scene;
import model.ApplicationModel;
import gui.awt.AwtApplicationHost;
import gui.awt.AwtApplicationModel;
import application.mcp.AwtJogl4VitralEditorMCP;

public class AwtJogl4SceneEditorApplication implements AwtApplicationHost {
    // Application model
    private ApplicationModel applicationModel;

    // Application GUI
    private AwtApplicationModel awtModel;
    private AwtJogl4GuiController awtGuiController;
    private AwtJogl4ApplicationController jogl4Controller;

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
        createGUI();

        int i;
        for ( i = 0; i < args.length; i++ ) {
            if ( args[i].equals("-s") ) {
                // Starts its own listener thread, which keeps it alive
                new AwtJogl4VitralEditorMCP(this);
            }
        }
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
        prepareRaytracedImage();
        applicationModel.getScene().raytrace(applicationModel.getRaytracedImage());
    }

    @Override
    public void doViewportRaytracingImage()
    {
        prepareRaytracedImage();
        ZBuffer depth = applicationModel.getRaytracedDepth();
        RGBImageUncompressed image = applicationModel.getRaytracedImage();
        if ( depth == null || depth.getXSize() != image.getXSize() ||
             depth.getYSize() != image.getYSize() ) {
            depth = new ZBuffer(image.getXSize(), image.getYSize());
            applicationModel.setRaytracedDepth(depth);
        }
        applicationModel.getScene().raytraceViewport(image, depth);
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

    public static void main(String[] args) {
        AwtJogl4MainThread mainThread = new AwtJogl4MainThread(args);
        SwingUtilities.invokeLater(mainThread);
    }
}
