#include "java/io/FileInputStream.h"
#include "java/io/FileOutputStream.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "vsdk/toolkit/io/image/RGBColorPalettePersistence.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/InteractionMode.h"
#include "model/Scene.h"
#include "model/history/EditHistory.h"
#include "model/history/SceneHistory.h"
#include "application/commands/GuiEventExecutor.h"

GuiEventExecutor::GuiEventExecutor(ApplicationModel* model,
                                   Presenter* presenter)
    : model(model), presenter(presenter),
      geometryCreationCommands(model, presenter)
{
}

Scene* GuiEventExecutor::scene() const
{
    return model->getScene();
}

java::String GuiEventExecutor::message(const char* id) const
{
    // Without I18N context the identifier itself is shown, as
    // `Widget::getMessage` does for unknown messages
    Widget* context = model->getI18nContext();
    return context != nullptr ? context->getMessage(id) : java::String(id);
}

bool GuiEventExecutor::executeCommand(const java::String& label)
{
    return execute(label) == CommandResult::DONE;
}

CommandResult GuiEventExecutor::execute(
    const java::String& label)
{
    SceneHistory* history = model->getEditHistory()->getSceneHistory();
    CommandResult result;

    history->begin();
    try {
        result = executeModelCommand(label);
    }
    catch ( ... ) {
        history->end(label, false);
        throw;
    }
    history->end(label, false);
    return result;
}

CommandResult GuiEventExecutor::executeModelCommand(
    const java::String& label)
{
    Light* light;
    CommandResult result;

    //- CREATE --------------------------------------------------------
    result = geometryCreationCommands.execute(label);
    if ( result != CommandResult::NOT_HANDLED ) {
        return result;
    }

    if ( label.equals("IDC_CREATE_PROJECTED_VIEWS") ) {
        model->getDrawingArea()->setProjectedViewsDebugRequested(true);
    }
    else if ( label.equals("IDC_CREATE_OMNILIGHT") ) {
        light = model->addNewLight();
        if ( light == nullptr ) {
            Logger::reportMessage("GuiEventExecutor", Logger::WARNING,
                "execute", "No visible viewport where to create the light");
            return CommandResult::FAILED;
        }
    }
    //- RENDERING -----------------------------------------------------
    else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERIMAGE") ) {
        presenter->showStatusMessage(message("IDM_PENDING_ZBUFFER_COLOR_IMAGE"));
        model->getDrawingArea()->setColorCaptureRequested(true);
    }
    else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERDEPTHMAP") ) {
        presenter->showStatusMessage(message("IDM_PENDING_ZBUFFER_DEPTH"));
        model->getDrawingArea()->setDepthCaptureRequested(true);
    }
    else if ( label.equals("IDC_RENDERING_OBTAINCONTOURNS") ) {
        presenter->showStatusMessage(message("IDM_PENDING_CONTOURNS"));
        model->getDrawingArea()->setDepthCaptureRequested(true);
        model->getDrawingArea()->setContoursRequested(true);
    }
    //-----------------------------------------------------------------
    else if ( label.equals("IDC_OTHERS_CYCLE_BACKGROUND") ) {
        scene()->rotateBackground();
    }
    else if ( label.equals("IDC_OTHERS_TOGGLE_TEST_CORRIDOR") ) {
        scene()->showCorridor = !scene()->showCorridor;
    }
    else if ( label.equals("IDC_OTHERS_TOGGLE_GRID") ) {
        model->getDrawingArea()->toggleSelectedViewportGrid();
    }
    else if ( label.equals("IDC_OTHERS_PRINT_SCENE_ON_CONSOLE") ) {
        scene()->print();
    }
    //-----------------------------------------------------------------
    else if ( label.equals("IDC_TOOLS_CAMERA") ) {
        presenter->showStatusMessage(message("IDM_CAMERA_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::CAMERA);
    }
    else if ( label.equals("IDC_TOOLS_SELECT") ) {
        presenter->showStatusMessage(message("IDM_SELECTION_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::SELECT);
    }
    else if ( label.equals("IDC_TOOLS_TRANSLATE") ) {
        presenter->showStatusMessage(message("IDM_TRANSLATION_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::TRANSLATE);
    }
    else if ( label.equals("IDC_TOOLS_ROTATE") ) {
        presenter->showStatusMessage(message("IDM_ROTATION_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::ROTATE);
    }
    else if ( label.equals("IDC_TOOLS_SCALE") ) {
        presenter->showStatusMessage(message("IDM_SCALE_MODE"));
        model->getDrawingArea()->setInteractionMode(InteractionMode::SCALE);
    }
    else if ( label.equals("IDC_TOOLS_RAY") ) {
        model->setWithVisualDebugRay(!model->isWithVisualDebugRay());
    }
    else if ( label.equals("IDC_NEW_VIEW") ) {
        model->getDrawingArea()->addViewport();
    }
    else if ( label.equals("IDC_DEL_VIEW") ) {
        model->getDrawingArea()->removeLastViewport();
    }
    else {
        return CommandResult::NOT_HANDLED;
    }
    return CommandResult::DONE;
}

bool GuiEventExecutor::importObjects(const java::File& file)
{
    if ( !file.canRead() ) {
        return false;
    }
    SceneHistory* history = model->getEditHistory()->getSceneHistory();

    history->begin();
    try {
        EnvironmentPersistence::importEnvironment(file, scene()->scene);
    }
    catch ( ... ) {
        history->end(java::String("Import of ") + file.getName(), false);
        throw;
    }
    history->end(java::String("Import of ") + file.getName(), false);
    return true;
}

bool GuiEventExecutor::exportObjects(const java::File& file,
                                     ExportFormat format)
{
    java::FileOutputStream fos(file.getPath().c_str());
    if ( !file.canWrite() ) {
        return false;
    }

    switch ( format ) {
      case ExportFormat::OBJ:
        EnvironmentPersistence::exportEnvironmentObj(fos, scene()->scene);
        break;
      case ExportFormat::GTS:
        EnvironmentPersistence::exportEnvironmentGts(fos, scene()->scene);
        break;
      default:
        EnvironmentPersistence::exportEnvironmentVtk(fos, scene()->scene);
        break;
    }

    fos.close();
    return true;
}

bool GuiEventExecutor::loadPalette(const java::File& file)
{
    if ( !file.canRead() ) {
        return false;
    }
    java::FileInputStream source(file.getPath().c_str());
    model->setPalette(RGBColorPalettePersistence::importGimpPalette(source));
    source.close();
    return true;
}
