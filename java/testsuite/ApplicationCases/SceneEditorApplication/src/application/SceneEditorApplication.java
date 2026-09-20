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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;

public class SceneEditorApplication {
    // Application model
    private ApplicationModel applicationModel;

    // Application GUI
    private AwtApplicationModel awtModel;
    private AwtGuiController awtGuiController;
    private Jogl4ApplicationController jogl4Controller;

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
        List<String> languages = new ArrayList<>();
        File[] files = new File(AwtApplicationModel.GUI_LANGUAGE_FOLDER).listFiles();

        if ( files != null ) {
            for ( File file : files ) {
                String name = file.getName();
                if ( file.isFile() && name.endsWith(AwtApplicationModel.JSON_EXTENSION) ) {
                    languages.add(name.substring(0, name.length() - AwtApplicationModel.JSON_EXTENSION.length()));
                }
            }
        }
        Collections.sort(languages);
        return languages;
    }

    /**
    @return the identifier of the language currently used by the GUI
    */
    public String getCurrentGuiLanguage()
    {
        String name = new File(awtModel.getLanguageGuiFile()).getName();

        if ( name.endsWith(AwtApplicationModel.JSON_EXTENSION) ) {
            name = name.substring(0, name.length() - AwtApplicationModel.JSON_EXTENSION.length());
        }
        return name;
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
        setGuiLanguage(AwtApplicationModel.GUI_LANGUAGE_FOLDER + language + AwtApplicationModel.JSON_EXTENSION);
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
        jogl4Controller = new Jogl4ApplicationController(applicationModel);
        awtModel = new AwtApplicationModel();
        awtModel.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        awtModel.setLanguageGuiFile(AwtApplicationModel.GUI_LANGUAGE_FOLDER + "english" + AwtApplicationModel.JSON_EXTENSION);
        awtModel.setFullScreenGuiMode(false);
        awtGuiController = new AwtGuiController(this, awtModel, jogl4Controller);
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
                // Starts its own listener thread, which keeps it alive
                new VitralEditorMCP(this);
            }
        }
    }

    public void doRaytracingImage()
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

    public void closeApplication()
    {
        System.exit(0);
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

    public static void main(String[] args) {
        MainThread mt = new MainThread(args);
        SwingUtilities.invokeLater(mt);
    }
}
