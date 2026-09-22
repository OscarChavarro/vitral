package application;

// Java classes
import java.util.List;
import javax.swing.SwingUtilities;

// VSDK Classes
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.processing.ImageProcessing;

// Application classes
import model.GuiState;
import model.Scene;
import model.ApplicationModel;
import gui.awt.AwtApplicationHost;
import gui.awt.AwtApplicationModel;

public class AwtJogl4SceneEditorApplication implements AwtApplicationHost {
    // Application model
    private ApplicationModel applicationModel;

    // Application GUI
    private AwtApplicationModel awtModel;
    private AwtJogl4GuiController awtGuiController;
    private AwtJogl4ApplicationController jogl4Controller;

    public void setLookAndFeel(String lookAndFeel)
    {
        awtGuiController.setLookAndFeel(lookAndFeel);
    }

    /**
    Could be better: if the Swing GUI is not destroyed, but all labels are
    renamed... but ... what if language files are not exactly equal?
    */
    public void setGuiLanguage(String lang)
    {
        awtGuiController.setGuiLanguage(lang);
    }

    /**
    @return the identifiers of the languages available for the GUI, sorted:
    the names (without extension) of the JSON files in the GUI language folder
    */
    public List<String> getGuiLanguages()
    {
        return GuiState.listLanguages();
    }

    /**
    @return the identifier of the language currently used by the GUI
    */
    public String getCurrentGuiLanguage()
    {
        return applicationModel.getGuiState().getCurrentLanguage();
    }

    /**
    Changes the language of the GUI, rebuilding it (and so, propagating the new
    messages to the application model).
    @param language one of the identifiers given by `getGuiLanguages`
    @return true if the language exists and was selected
    */
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

    public AwtJogl4SceneEditorApplication(String[] args) {
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
        applicationModel.getScene().raytraceViewport(applicationModel.getRaytracedImage());
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

    /**
    Requests to draw again the drawing area, whatever the rendering
    technology presenting it.
    */
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
        AwtJogl4MainThread mt = new AwtJogl4MainThread(args);
        SwingUtilities.invokeLater(mt);
    }
}
