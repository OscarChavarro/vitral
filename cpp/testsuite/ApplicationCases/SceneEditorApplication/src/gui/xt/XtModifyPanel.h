#ifndef __XT_MODIFY_PANEL__
#define __XT_MODIFY_PANEL__

#include <X11/Intrinsic.h>

#include "render/BodyEditFeedbackProvider.h"
#include "vsdk/toolkit/gui/editor/GenericEditorListener.h"

class SimpleBody;
class XtGenericEditor;
class XtModifyPanelForFunctionalExplicitSurface;
class XtModifyPanelHost;

/**
Page of the side panel that edits the selected body, as `AwtModifyPanel`
does for Swing: with an editor specific to its geometry (subclasses of this
panel, i.e. `XtModifyPanelForFunctionalExplicitSurface`) or, for any other
geometry, with a `XtGenericEditor` built from the control specifications
and accessors the geometry class declared. It does not draw: renderers ask
it, through `BodyEditFeedbackProvider`, for the feedback geometry to
present over the body under edition.
*/
class XtModifyPanel :
    public BodyEditFeedbackProvider,
    private GenericEditorListener {
protected:
    XtModifyPanelHost* parent;
    SimpleBody* target;
    /// Panel holding the widgets of the panel, or null for editors that
    /// build into the container of the panel that owns them
    Widget container;
    int width;

private:
    // Implementations
    XtModifyPanelForFunctionalExplicitSurface* functionalExplicitSurfaceEditor;
    /// Fallback editor, built from the control specifications of the geometry
    XtGenericEditor* genericEditor;
    /// Editor for the current target, or null if there is none
    XtModifyPanel* activeEditor;

    XtModifyPanel(const XtModifyPanel& other);
    XtModifyPanel& operator=(const XtModifyPanel& other);

    virtual void notifyEntityChanged(Entity* entity) override;

public:
    /**
    @param parent application services (referenced)
    @param container panel (see `XtPanelWidgets::createPanel`) where this
    one places its widgets
    @param width width of the container
    */
    XtModifyPanel(XtModifyPanelHost* parent, Widget container, int width);
    virtual ~XtModifyPanel();

    virtual SimpleBody* getTarget() override;

    void notifyTargetBeginEdit(SimpleBody* target);
    void notifyTargetEndEdit();

    /**
    Editors for specific geometries override this method to present their
    feedback (handles, bounds...) over the target. This one delegates to the
    editor of the current target.
    @return the feedback geometry to present over the target, in world
    coordinates (empty if there is none)
    */
    virtual java::ArrayList<RenderPrimitive> buildEditFeedback() override;
};

#endif
