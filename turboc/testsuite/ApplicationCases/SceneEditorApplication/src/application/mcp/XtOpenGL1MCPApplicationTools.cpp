#include <math.h>
#include "java/lang/String.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

#include "java/io/File.h"
#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/XtEventQueue.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "application/XtOpenGL1ApplicationController.h"
#include "application/XtOpenGL1SceneEditorApplication.h"
#include "application/mcp/MCPJson.h"
#include "application/mcp/XtOpenGL1MCPApplicationTools.h"
#include "model/ApplicationModel.h"
#include "model/Scene.h"
#include "model/selection/SelectionSet.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/IndexOutOfBoundsException.h"
#include "java/lang/RuntimeException.h"


/**
Closes the application once the response of `app.exit` was sent.
*/
class CloseApplication : public Runnable {
private:
    XtOpenGL1SceneEditorApplication* parent;

public:
    explicit CloseApplication(XtOpenGL1SceneEditorApplication* parent)
        : parent(parent) {}

    virtual void run()
    {
        parent->closeApplication();
    }
};

/**
@return the message of the Java `IndexOutOfBoundsException` of lists
*/
static String indexOutOfBounds(int index, int length)
{
    return "Index " + Cpp98Support::toString(index) + " out of bounds for length " +
        Cpp98Support::toString(length);
}


XtOpenGL1MCPApplicationTools::XtOpenGL1MCPApplicationTools(
    XtOpenGL1SceneEditorApplication* parent)
    : parent(parent)
{
}

void XtOpenGL1MCPApplicationTools::repaint()
{
    parent->getOpenGL1Controller()->repaint();
}

XtOpenGL1ApplicationController*
XtOpenGL1MCPApplicationTools::getDrawingAreaController()
{
    XtOpenGL1ApplicationController* controller =
        parent->getOpenGL1Controller();

    if ( !controller->isDrawingAreaCreated() ) {
        throw IllegalStateException("The drawing area has not been created");
    }
    return controller;
}

//= Drawing area ==========================================================

void XtOpenGL1MCPApplicationTools::injectMouse(const String& request)
{
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    String type = MCPJson::stringProperty(request, "type", "move");
    int x = (int)Cpp98Support::round(MCPJson::numberProperty(request, "x", 0));
    int y = (int)Cpp98Support::round(MCPJson::numberProperty(request, "y", 0));
    int button = (int)MCPJson::numberProperty(request, "button", 1);

    drawingArea->injectMouseEvent(type, x, y, button);
}

void XtOpenGL1MCPApplicationTools::injectKey(const String& request)
{
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    String key = MCPJson::stringProperty(request, "key", "");
    bool shift = false;
    bool ctrl = false;

    MCPJson::booleanProperty(request, "shift", shift);
    MCPJson::booleanProperty(request, "ctrl", ctrl);
    drawingArea->injectKeyEvent(key, shift, ctrl);
}

String XtOpenGL1MCPApplicationTools::showImportDialog()
{
    if ( parent->getApplicationModel() == NULL ) {
        throw IllegalStateException("The windowed editor GUI has not been created");
    }
    return parent->showImportSceneDialog() ? "{\"ok\":true}" : "{\"ok\":false}";
}

String XtOpenGL1MCPApplicationTools::projectSelectedBody(
    const String& request)
{
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    Scene* scene = parent->getApplicationModel()->getScene();
    ViewportSet* set = parent->getApplicationModel()->getActiveViewportSet();
    int viewportIndex = (int)MCPJson::numberProperty(request, "viewport", 0);
    int selected = scene->selectedThings->firstSelected();

    if ( selected < 0 ) {
        throw IllegalStateException("No body is selected");
    }
    if ( viewportIndex < 0 || viewportIndex >= set->getViewportCount() ) {
        throw IndexOutOfBoundsException(indexOutOfBounds(viewportIndex,
                                                 set->getViewportCount()));
    }
    Viewport* viewport = set->getViewport(viewportIndex);
    Vector3Dd p = scene->scene->getSimpleBodies().get(selected)->getPosition();
    const char* names[] = {"origin", "x", "y", "z"};
    Vector3Dd points[] = {
        p,
        p.add(Vector3Dd(1, 0, 0)),
        p.add(Vector3Dd(0, 1, 0)),
        p.add(Vector3Dd(0, 0, 1))
    };
    StringBuilder sb;

    sb.append("{\"viewport\":\"").append(MCPJson::escape(viewport->getTitle()))
        .append('"');
    for ( int i = 0; i < 4; i++ ) {
        double pixel[2];

        sb.append(",\"").append(names[i]).append("\":");
        if ( drawingArea->projectToCanvas(viewport, points[i], pixel) ) {
            sb.append('[').append(pixel[0]).append(',').append(pixel[1])
                .append(']');
        }
        else {
            sb.append("null");
        }
    }
    sb.append('}');
    return sb.toString();
}

//= Images ================================================================

String XtOpenGL1MCPApplicationTools::raytracePng(
    const String& request)
{
    String path = MCPJson::stringProperty(request, "path",
                                                "./mcp-raytrace.png");
    int width = (int)MCPJson::numberProperty(request, "width", 640);
    int height = (int)MCPJson::numberProperty(request, "height", 480);
    parent->getApplicationModel()->setRaytracedImageWidth(width);
    parent->getApplicationModel()->setRaytracedImageHeight(height);
    parent->doRaytracingImage();
    File out(path);
    if ( !ImagePersistence::exportPNG(out,
             parent->getApplicationModel()->getRaytracedImage()) ) {
        throw RuntimeException((String("Can not write ") +
            out.getAbsolutePath()).c_str());
    }
    return MCPJson::writtenFile(out.getAbsolutePath());
}

String XtOpenGL1MCPApplicationTools::exportViewportJpg(
    const String& request)
{
    String path = MCPJson::stringProperty(request, "path",
                                                "./outputSelectedViewport.jpg");
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    File out(path);
    drawingArea->exportViewportJpg(out);
    return MCPJson::writtenFile(out.getAbsolutePath());
}

String XtOpenGL1MCPApplicationTools::exportWorkspaceJpg(
    const String& request)
{
    String path = MCPJson::stringProperty(request, "path",
                                                "./outputViewport.jpg");
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    File out(path);
    drawingArea->exportWorkspaceJpg(out);
    return MCPJson::writtenFile(out.getAbsolutePath());
}

//= Dialogs ===============================================================

void XtOpenGL1MCPApplicationTools::openSelector()
{
    parent->openSelectorDialog();
}

String XtOpenGL1MCPApplicationTools::selectBodyInDialog(
    const String& request)
{
    if ( !parent->isSelectorDialogVisible() ) {
        throw IllegalStateException("Selector is not open");
    }
    parent->selectorSelectBody(
        (int)MCPJson::numberProperty(request, "index", -1));
    return String("{\"ok\":true,\"selectedBody\":") +
        String::valueOf(parent->getApplicationModel()->getScene()
                              ->selectedThings->firstSelected()) + "}";
}

String XtOpenGL1MCPApplicationTools::selectorAction(
    const String& request)
{
    if ( !parent->isSelectorDialogVisible() ) {
        throw IllegalStateException("Selector is not open");
    }
    parent->selectorAction(MCPJson::stringProperty(request, "action", ""));
    return "{\"ok\":true}";
}

void XtOpenGL1MCPApplicationTools::openTangibleDialog()
{
    parent->openTangibleInterfaceDialog();
}

String XtOpenGL1MCPApplicationTools::dialogState()
{
    StringBuilder sb;
    sb.append("{\"dialogs\":[");
    bool first = true;
    if ( parent->isSelectorDialogVisible() ) {
        sb.append("{\"id\":\"scene.selector\"}");
        first = false;
    }
    if ( parent->isTangibleInterfaceDialogVisible() ) {
        if ( !first ) {
            sb.append(',');
        }
        sb.append("{\"id\":\"tangible.connect\",\"serviceUrl\":\"")
            .append(MCPJson::escape(parent->getTangibleInterfaceDialogServiceUrl()))
            .append("\",\"connectLabel\":\"")
            .append(MCPJson::escape(parent->getTangibleInterfaceDialogConnectLabel()))
            .append("\"}");
    }
    sb.append("]}");
    return sb.toString();
}

String XtOpenGL1MCPApplicationTools::tangibleAction(
    const String& request)
{
    if ( !parent->isTangibleInterfaceDialogVisible() ) {
        throw IllegalStateException("Tangible dialog is not open");
    }
    String action = MCPJson::stringProperty(request, "action", "cancel");
    String serviceUrl;
    bool withServiceUrl = action.equals("connect") &&
        MCPJson::hasProperty(request, "serviceUrl");
    if ( withServiceUrl ) {
        serviceUrl = MCPJson::stringProperty(request, "serviceUrl", "");
    }
    if ( !parent->tangibleInterfaceDialogAction(action,
             withServiceUrl ? &serviceUrl : NULL) ) {
        throw IllegalArgumentException(
            (String("Unknown dialog action: ") + action).c_str());
    }
    return dialogState();
}

//= Application ===========================================================

String XtOpenGL1MCPApplicationTools::listLanguages()
{
    StringBuilder sb;
    String current = parent->getCurrentGuiLanguage();
    ArrayList<String> languages = parent->getGuiLanguages();
    bool first = true;

    sb.append("{\"current\":\"").append(MCPJson::escape(current))
        .append("\",\"languages\":[");
    for ( long i = 0; i < languages.size(); i++ ) {
        const String& language = languages.get(i);
        if ( !first ) {
            sb.append(',');
        }
        first = false;
        sb.append("{\"id\":\"").append(MCPJson::escape(language)).append('"')
            .append(",\"current\":").append(language.equals(current)).append('}');
    }
    sb.append("]}");
    return sb.toString();
}

String XtOpenGL1MCPApplicationTools::setLanguage(
    const String& request)
{
    String language = MCPJson::stringProperty(request, "language", "");

    if ( !parent->setGuiLanguageById(language) ) {
        // As Java `List.toString`
        ArrayList<String> languages = parent->getGuiLanguages();
        StringBuilder available;
        available.append('[');
        for ( long i = 0; i < languages.size(); i++ ) {
            if ( i > 0 ) {
                available.append(", ");
            }
            available.append(languages.get(i));
        }
        available.append(']');
        throw IllegalArgumentException((String("Unknown language \"") +
            language + "\". Available languages: " +
            available.toString()).c_str());
    }
    return String("{\"ok\":true,\"language\":\"") +
        MCPJson::escape(language) + "\"}";
}

String XtOpenGL1MCPApplicationTools::exitApplication()
{
    XtEventQueue::invokeLater(new CloseApplication(parent), 300);
    return "{\"ok\":true,\"message\":\"The application is closing\"}";
}
