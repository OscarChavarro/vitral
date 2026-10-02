package application.commands;

// Java basic classes
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.gui.CommandListener;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;

// Application classes
import model.ApplicationModel;
import model.InteractionMode;
import model.Scene;
import model.history.SceneHistory;

/**
Executes the commands of the GUI of the editor (identified by the `IDC_*`
names of the I18N GUI definition) that only work over the application model:
creation of objects and lights, capture requests, interaction modes, viewport
and debugging toggles. It also offers the persistence operations whose files
are chosen by the GUI. It does not depend on any GUI or rendering technology:
each GUI technology executes here what it does not present itself (file
dialogs, windows, look and feel...), and presents the status messages
requested through its `Presenter`. The creation of geometries is delegated
to `GeometryCreationCommandsExecutor`.

What the commands change in the scene (i.e. objects and lights created,
objects imported) is recorded in the scene history of the model, so it can be
undone.
*/
public class GuiEventExecutor extends CommandListener
{
    private final ApplicationModel model;
    private final Presenter presenter;
    private final GeometryCreationCommandsExecutor geometryCreationCommands;

    /**
    @param model application model the commands work over
    @param presenter presents the status messages of the commands
    */
    public GuiEventExecutor(ApplicationModel model, Presenter presenter)
    {
        this.model = model;
        this.presenter = presenter;
        this.geometryCreationCommands =
            new GeometryCreationCommandsExecutor(model, presenter);
    }

    private Scene scene() {
        return model.getScene();
    }

    /**
    @param label identifier of the command (`IDC_*`)
    @return true if the command was executed
    */
    @Override
    public boolean executeCommand(String label)
    {
        return execute(label) == CommandResult.DONE;
    }

    /**
    @param label identifier of the command (`IDC_*`)
    @return whether the command was executed, failed, or is not one of the
    commands of this class
    */
    public CommandResult execute(String label)
    {
        SceneHistory history = model.getEditHistory().getSceneHistory();
        CommandResult result;

        history.begin();
        try {
            result = executeModelCommand(label);
        }
        finally {
            history.end(label, false);
        }
        return result;
    }

    private CommandResult executeModelCommand(String label)
    {
        Light light;
        CommandResult result;

        //- CREATE --------------------------------------------------------
        result = geometryCreationCommands.execute(label);
        if ( result != CommandResult.NOT_HANDLED ) {
            return result;
        }

        if ( label.equals("IDC_CREATE_PROJECTED_VIEWS") ) {
            model.getDrawingArea().setProjectedViewsDebugRequested(true);
        }
        else if ( label.equals("IDC_CREATE_OMNILIGHT") ) {
            light = model.addNewLight();
            if ( light == null ) {
                Logger.reportMessage(this, VSDK.WARNING, "execute", "No visible viewport where to create the light");
                return CommandResult.FAILED;
            }
        }
        //- RENDERING -----------------------------------------------------
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERIMAGE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_PENDING_ZBUFFER_COLOR_IMAGE"));
            model.getDrawingArea().setColorCaptureRequested(true);
        }
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERDEPTHMAP") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_PENDING_ZBUFFER_DEPTH"));
            model.getDrawingArea().setDepthCaptureRequested(true);
        }
        else if ( label.equals("IDC_RENDERING_OBTAINCONTOURNS") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_PENDING_CONTOURNS"));
            model.getDrawingArea().setDepthCaptureRequested(true);
            model.getDrawingArea().setContoursRequested(true);
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_OTHERS_CYCLE_BACKGROUND") ) {
            scene().rotateBackground();
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_TEST_CORRIDOR") ) {
            if ( scene().showCorridor == true ) {
                scene().showCorridor = false;
            }
            else {
                scene().showCorridor = true;
            }
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_GRID") ) {
            model.getDrawingArea().toggleSelectedViewportGrid();
        }
        else if ( label.equals("IDC_OTHERS_PRINT_SCENE_ON_CONSOLE") ) {
            scene().print();
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_TOOLS_CAMERA") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_CAMERA_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.CAMERA);
        }
        else if ( label.equals("IDC_TOOLS_SELECT") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_SELECTION_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.SELECT);
        }
        else if ( label.equals("IDC_TOOLS_TRANSLATE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_TRANSLATION_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.TRANSLATE);
        }
        else if ( label.equals("IDC_TOOLS_ROTATE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_ROTATION_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.ROTATE);
        }
        else if ( label.equals("IDC_TOOLS_SCALE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_SCALE_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.SCALE);
        }
        else if ( label.equals("IDC_TOOLS_RAY") ) {
            model.setWithVisualDebugRay(
                !model.isWithVisualDebugRay());
        }
        else if ( label.equals("IDC_NEW_VIEW") ) {
            model.getDrawingArea().addViewport();
        }
        else if ( label.equals("IDC_DEL_VIEW") ) {
            model.getDrawingArea().removeLastViewport();
        }
        else {
            return CommandResult.NOT_HANDLED;
        }
        return CommandResult.DONE;
    }

    /**
    Adds the objects of a file to the scene.
    @param file 3ds, vtk, gts, obj or ply file
    @throws Exception if the file can not be read
    */
    public void importObjects(File file) throws Exception
    {
        SceneHistory history = model.getEditHistory().getSceneHistory();

        history.begin();
        try {
            EnvironmentPersistence.importEnvironment(file, scene().scene);
        }
        finally {
            history.end("Import of " + file.getName(), false);
        }
    }

    /**
    Writes the objects of the scene to a file.
    @param file destination file
    @param format format of the file
    @throws Exception if the file can not be written
    */
    public void exportObjects(File file, ExportFormat format) throws Exception
    {
        FileOutputStream fos;
        fos = new FileOutputStream(file);

        switch ( format ) {
          case OBJ:
            EnvironmentPersistence.exportEnvironmentObj(fos, scene().scene);
            break;
          case GTS:
            EnvironmentPersistence.exportEnvironmentGts(fos, scene().scene);
            break;
          default:
            EnvironmentPersistence.exportEnvironmentVtk(fos, scene().scene);
            break;
        }

        fos.close();
    }

    /**
    Replaces the palette used to present depth maps.
    @param file Gimp palette (gpl) file
    @throws Exception if the file can not be read
    */
    public void loadPalette(File file) throws Exception
    {
        model.setPalette(
            RGBColorPalettePersistence.importGimpPalette(
                new FileReader(file.getAbsolutePath())));
    }
}
