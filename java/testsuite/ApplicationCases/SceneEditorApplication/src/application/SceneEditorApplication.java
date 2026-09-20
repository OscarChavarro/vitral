package application;

// VSDK Classes
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.processing.ImageProcessing;

// Application classes
import application.framework.Scene;
import application.model.ApplicationModel;
import application.gui.AwtApplicationModel;
import application.gui.AwtGuiController;
import application.net.VitralEditorMCP;
import javax.swing.SwingUtilities;

public class SceneEditorApplication {
    // Application model
    private ApplicationModel applicationModel;

    // Application GUI
    private AwtApplicationModel awtModel;
    private AwtGuiController awtGuiController;
    private Jogl4ApplicationController jogl4Controller;

    // Networking
    private VitralEditorMCP networkServer;

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
        jogl4Controller = new Jogl4ApplicationController(applicationModel);
        awtModel = new AwtApplicationModel();
        awtModel.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        awtModel.setLanguageGuiFile(AwtApplicationModel.GUI_LANGUAGE_FOLDER + "english.json");
        awtModel.setFullScreenGuiMode(false);
        awtGuiController = new AwtGuiController(this, awtModel, jogl4Controller);

        networkServer = null;
    }

    public final void createGUI()
    {
        awtGuiController.createGUI();
    }

    public void destroyGUI()
    {
        awtGuiController.destroyGUI();
    }

    public SceneEditorApplication(String[] args) {
        createModel();
        createGUI();

        int i;
        for ( i = 0; i < args.length; i++ ) {
            if ( args[i].equals("-s") ) {
                networkServer = new VitralEditorMCP(this);
            }
        }
    }

    public void doRaytracedImage()
    {
        applicationModel.getRaytracedImage().init(
            applicationModel.getRaytracedImageWidth(),
            applicationModel.getRaytracedImageHeight());
        if ( applicationModel.getScene().selectedBackground == 1 ) {
            ImageProcessing.resize(
                applicationModel.getScene().fixedBackground.getImage(),
                applicationModel.getRaytracedImage());
        }
        applicationModel.getScene().raytrace(applicationModel.getRaytracedImage());
    }

    public void switchVoiceCommandClient()
    {
        awtGuiController.switchVoiceCommandClient();
    }

    public void closeApplication()
    {
        awtGuiController.closeApplication();
        System.exit(0);
    }

    public void externalCommand(String label)
    {
        boolean b = awtModel.getGuiEventExecutor().executeCommand(label);
    }

    public ApplicationModel getApplicationModel()
    {
        return applicationModel;
    }

    public Jogl4ApplicationController getJogl4Controller()
    {
        return jogl4Controller;
    }

    public AwtApplicationModel getAwtModel()
    {
        return awtModel;
    }

    public AwtGuiController getAwtGuiController()
    {
        return awtGuiController;
    }

    public static void main(String[] args) {
        MainThread mt = new MainThread(args);
        SwingUtilities.invokeLater(mt);
    }
}
