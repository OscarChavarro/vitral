#ifndef __APPLICATION_MODEL__
#define __APPLICATION_MODEL__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "model/DrawingArea.h"
#include "model/GuiState.h"
#include "model/SceneLightFactory.h"
#include "model/history/EditHistory.h"

class Camera;
class Light;
class PointLight;
class RGBColorPalette;
class RGBImageUncompressed;
class Scene;
class SimpleBody;
class ViewportSet;
class Widget;
class ZBuffer;

/**
Model of the scene editor application: the scene, the viewport sets (one per
display), the drawing area, the I18N context, the state of the GUI, the
edit history and the images produced by the renderers.

C++ port note: the model owns everything it references (scene, viewport
sets, I18N context, palette and images); replacing one of them deletes the
former one.
*/
class ApplicationModel {
private:
    Scene* scene;
    RGBImageUncompressed* raytracedImage;
    /// OpenGL depth of `raytracedImage` when it is shown in a viewport
    ZBuffer* raytracedDepth;
    RGBImageUncompressed* zbufferImage;
    int raytracedImageWidth;
    int raytracedImageHeight;
    RGBColorPalette* palette;
    bool withVisualDebugRay;
    bool hasVisualDebugRay;
    Ray visualDebugRay;
    int visualDebugRayLevels;
    java::ArrayList<ViewportSet*> viewportSets;
    int activeViewportSetIndex;
    Widget* i18nContext;
    SceneLightFactory lightFactory;
    DrawingArea* drawingArea;
    GuiState guiState;
    EditHistory editHistory;

    ApplicationModel(const ApplicationModel& other);
    ApplicationModel& operator=(const ApplicationModel& other);

public:
    /**
    Creates the model with one standard `ViewportSet`. More sets can be
    added to support users working with several displays.
    */
    ApplicationModel();
    virtual ~ApplicationModel();

    /**
    @return state of the GUI that does not depend on the GUI technology
    */
    GuiState* getGuiState();

    /**
    @return the drawing area presenting the viewport set of the first
    display
    */
    DrawingArea* getDrawingArea() const;

    /**
    @return a read-only view of the viewport sets, one for each display
    */
    const java::ArrayList<ViewportSet*>& getViewportSets() const;

    /**
    @param viewportSet set to add, taking its ownership
    */
    void addViewportSet(ViewportSet* viewportSet);

    /**
    @return the I18N context (GUI definition in the current language) shared
    by the viewport sets, or null if there is none yet
    */
    Widget* getI18nContext() const;

    /**
    Sets the I18N context and propagates it to every viewport set, so they
    are presented with the messages of the language currently selected by
    the user. It must be called each time the GUI definition is loaded.
    @param i18nContext new context, owned by the model
    */
    void setI18nContext(Widget* i18nContext);
    int getActiveViewportSetIndex() const;
    void setActiveViewportSetIndex(int activeViewportSetIndex);

    /**
    @return the viewport set of the display currently receiving interaction
    */
    ViewportSet* getActiveViewportSet() const;
    Scene* getScene() const;

    /**
    Replaces the edited scene. The operations of the scene history belong to
    the former scene, so they are forgotten.
    @param scene the new scene, owned by the model
    */
    void setScene(Scene* scene);

    /**
    @return undo/redo history of the scene and of the views of the viewports
    */
    EditHistory* getEditHistory();
    Camera* getCamera() const;
    Camera* getActiveCamera() const;
    void setActiveCamera(Camera* activeCamera);
    java::ArrayList<Light*>& getLights();

    /**
    Adds a new point light to the scene, placed inside the view volume of a
    camera of the active viewport set (see `SceneLightFactory`).
    @return the added light (owned by the scene), or null if no viewport is
    visible
    */
    PointLight* addNewLight();
    java::ArrayList<SimpleBody*>& getSimpleBodies();

    /**
    @return window space (OpenGL) depth of each pixel of the raytraced image
    of a viewport in CPU render mode, or null if there is none
    */
    ZBuffer* getRaytracedDepth() const;
    void setRaytracedDepth(ZBuffer* raytracedDepth);
    RGBImageUncompressed* getRaytracedImage() const;
    void setRaytracedImage(RGBImageUncompressed* raytracedImage);
    RGBImageUncompressed* getZbufferImage() const;
    void setZbufferImage(RGBImageUncompressed* zbufferImage);
    int getRaytracedImageWidth() const;
    void setRaytracedImageWidth(int raytracedImageWidth);
    int getRaytracedImageHeight() const;
    void setRaytracedImageHeight(int raytracedImageHeight);
    RGBColorPalette* getPalette() const;
    void setPalette(RGBColorPalette* palette);
    bool isWithVisualDebugRay() const;
    void setWithVisualDebugRay(bool withVisualDebugRay);

    /**
    @return the ray to debug visually, or null if there is none
    */
    const Ray* getVisualDebugRay() const;

    /**
    @param visualDebugRay ray to debug visually (copied), or null
    */
    void setVisualDebugRay(const Ray* visualDebugRay);
    int getVisualDebugRayLevels() const;
    void setVisualDebugRayLevels(int visualDebugRayLevels);
};

#endif
