package gui.awt;

// Application classes
import model.ApplicationModel;

/**
Services that the AWT/Swing GUI classes need from the application hosting
them. It keeps those classes independent of the rendering technology used by
the application (see `application.AwtJogl4SceneEditorApplication`).
*/
public interface AwtApplicationHost
{
    /**
    @return technology independent model of the application
    */
    ApplicationModel getApplicationModel();

    /**
    @return model of the AWT/Swing GUI
    */
    AwtApplicationModel getAwtModel();

    /**
    Requests a new frame in the drawing area.
    */
    void repaintDrawingArea();

    /**
    Tells the modify panel which is the body currently selected.
    */
    void reportTargetToModifyPanel();

    /**
    Ray traces the scene into the raytraced image of the application model.
    */
    void doRaytracingImage();

    /**
    Ends the application.
    */
    void closeApplication();

    /**
    Destroys the main window of the GUI.
    */
    void destroyGUI();

    /**
    Creates the main window of the GUI.
    */
    void createGUI();
}
