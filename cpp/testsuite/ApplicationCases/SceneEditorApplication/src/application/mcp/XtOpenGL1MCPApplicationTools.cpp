#include <cmath>
#include <stdexcept>
#include <string>

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

namespace {

/**
Closes the application once the response of `app.exit` was sent.
*/
class CloseApplication : public java::Runnable {
private:
    XtOpenGL1SceneEditorApplication* parent;

public:
    explicit CloseApplication(XtOpenGL1SceneEditorApplication* parent)
        : parent(parent) {}

    virtual void run() override
    {
        parent->closeApplication();
    }
};

/**
@return the message of the Java `IndexOutOfBoundsException` of lists
*/
std::string indexOutOfBounds(int index, int length)
{
    return "Index " + std::to_string(index) + " out of bounds for length " +
        std::to_string(length);
}

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
        throw std::logic_error("The drawing area has not been created");
    }
    return controller;
}

//= Drawing area ==========================================================

void XtOpenGL1MCPApplicationTools::injectMouse(const java::String& request)
{
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    java::String type = MCPJson::stringProperty(request, "type", "move");
    int x = (int)std::round(MCPJson::numberProperty(request, "x", 0));
    int y = (int)std::round(MCPJson::numberProperty(request, "y", 0));
    int button = (int)MCPJson::numberProperty(request, "button", 1);

    drawingArea->injectMouseEvent(type, x, y, button);
}

void XtOpenGL1MCPApplicationTools::injectKey(const java::String& request)
{
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    java::String key = MCPJson::stringProperty(request, "key", "");
    bool shift = false;
    bool ctrl = false;

    MCPJson::booleanProperty(request, "shift", shift);
    MCPJson::booleanProperty(request, "ctrl", ctrl);
    drawingArea->injectKeyEvent(key, shift, ctrl);
}

java::String XtOpenGL1MCPApplicationTools::projectSelectedBody(
    const java::String& request)
{
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    Scene* scene = parent->getApplicationModel()->getScene();
    ViewportSet* set = parent->getApplicationModel()->getActiveViewportSet();
    int viewportIndex = (int)MCPJson::numberProperty(request, "viewport", 0);
    int selected = scene->selectedThings->firstSelected();

    if ( selected < 0 ) {
        throw std::logic_error("No body is selected");
    }
    if ( viewportIndex < 0 || viewportIndex >= set->getViewportCount() ) {
        throw std::out_of_range(indexOutOfBounds(viewportIndex,
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
    java::StringBuilder sb;

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

java::String XtOpenGL1MCPApplicationTools::raytracePng(
    const java::String& request)
{
    java::String path = MCPJson::stringProperty(request, "path",
                                                "./mcp-raytrace.png");
    int width = (int)MCPJson::numberProperty(request, "width", 640);
    int height = (int)MCPJson::numberProperty(request, "height", 480);
    parent->getApplicationModel()->setRaytracedImageWidth(width);
    parent->getApplicationModel()->setRaytracedImageHeight(height);
    parent->doRaytracingImage();
    java::File out(path);
    if ( !ImagePersistence::exportPNG(out,
             parent->getApplicationModel()->getRaytracedImage()) ) {
        throw std::runtime_error((java::String("Can not write ") +
            out.getAbsolutePath()).c_str());
    }
    return MCPJson::writtenFile(out.getAbsolutePath());
}

java::String XtOpenGL1MCPApplicationTools::exportViewportJpg(
    const java::String& request)
{
    java::String path = MCPJson::stringProperty(request, "path",
                                                "./outputSelectedViewport.jpg");
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    java::File out(path);
    drawingArea->exportViewportJpg(out);
    return MCPJson::writtenFile(out.getAbsolutePath());
}

java::String XtOpenGL1MCPApplicationTools::exportWorkspaceJpg(
    const java::String& request)
{
    java::String path = MCPJson::stringProperty(request, "path",
                                                "./outputViewport.jpg");
    XtOpenGL1ApplicationController* drawingArea = getDrawingAreaController();
    java::File out(path);
    drawingArea->exportWorkspaceJpg(out);
    return MCPJson::writtenFile(out.getAbsolutePath());
}

//= Application ===========================================================

java::String XtOpenGL1MCPApplicationTools::listLanguages()
{
    java::StringBuilder sb;
    java::String current = parent->getCurrentGuiLanguage();
    java::ArrayList<java::String> languages = parent->getGuiLanguages();
    bool first = true;

    sb.append("{\"current\":\"").append(MCPJson::escape(current))
        .append("\",\"languages\":[");
    for ( long i = 0; i < languages.size(); i++ ) {
        const java::String& language = languages.get(i);
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

java::String XtOpenGL1MCPApplicationTools::setLanguage(
    const java::String& request)
{
    java::String language = MCPJson::stringProperty(request, "language", "");

    if ( !parent->setGuiLanguageById(language) ) {
        // As Java `List.toString`
        java::ArrayList<java::String> languages = parent->getGuiLanguages();
        java::StringBuilder available;
        available.append('[');
        for ( long i = 0; i < languages.size(); i++ ) {
            if ( i > 0 ) {
                available.append(", ");
            }
            available.append(languages.get(i));
        }
        available.append(']');
        throw std::invalid_argument((java::String("Unknown language \"") +
            language + "\". Available languages: " +
            available.toString()).c_str());
    }
    return java::String("{\"ok\":true,\"language\":\"") +
        MCPJson::escape(language) + "\"}";
}

java::String XtOpenGL1MCPApplicationTools::exitApplication()
{
    XtEventQueue::invokeLater(new CloseApplication(parent), 300);
    return "{\"ok\":true,\"message\":\"The application is closing\"}";
}
