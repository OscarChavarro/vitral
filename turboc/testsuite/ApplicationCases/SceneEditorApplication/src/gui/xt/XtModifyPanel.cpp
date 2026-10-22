#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/editor/XtGenericEditor.h"
#include "gui/xt/XtModifyPanel.h"
#include "gui/xt/XtModifyPanelHost.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "gui/xt/editor/XtModifyPanelForFunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"

XtModifyPanel::XtModifyPanel(XtModifyPanelHost* parent, Widget container,
                             int width)
    : parent(parent), target(NULL), container(container), width(width),
      functionalExplicitSurfaceEditor(NULL), genericEditor(NULL),
      activeEditor(NULL)
{
    notifyTargetEndEdit();
}

XtModifyPanel::~XtModifyPanel()
{
    delete functionalExplicitSurfaceEditor;
    delete genericEditor;
}

SimpleBody* XtModifyPanel::getTarget()
{
    return target;
}

void XtModifyPanel::notifyTargetBeginEdit(SimpleBody* target)
{
    this->target = target;
    XtWidgetSupport::removeAll(container);
    activeEditor = NULL;

    if ((target->getGeometry() != NULL && target->getGeometry()->instanceOf(FunctionalExplicitSurface::CLASS))) {
        if (genericEditor != NULL) {
            genericEditor->detach();
        }
        if (functionalExplicitSurfaceEditor == NULL) {
            functionalExplicitSurfaceEditor =
                new XtModifyPanelForFunctionalExplicitSurface(parent);
        }
        functionalExplicitSurfaceEditor->notifyTargetBeginEdit(
            target, container, width);
        activeEditor = functionalExplicitSurfaceEditor;
    }
    else {
        if (genericEditor == NULL) {
            genericEditor = new XtGenericEditor(
                parent->getPanelWidgets(), container,
                parent->getPanelFontSet(), width);
            genericEditor->setListener(this);
        }
        genericEditor->build(target->getGeometry());
    }
}

void XtModifyPanel::notifyTargetEndEdit()
{
    target = NULL;
    activeEditor = NULL;
    if (container == NULL) {
        // An editor building into the container of its owner panel
        return;
    }
    if (genericEditor != NULL && genericEditor->isEntityDeleted()) {
        // Keep the "Entity deleted" message the editor is showing
        return;
    }
    if (genericEditor != NULL) {
        genericEditor->detach();
    }
    XtWidgetSupport::removeAll(container);
    parent->getPanelWidgets()->createLabel(
        container, "No selected object for modifying.",
        parent->getPanelFontSet(), XtPanelWidgets::LEFT, 8, 8, width - 16, 24);
}

ArrayList<RenderPrimitive> XtModifyPanel::buildEditFeedback()
{
    if (activeEditor != NULL) {
        return activeEditor->buildEditFeedback();
    }
    return ArrayList<RenderPrimitive>();
}

void XtModifyPanel::notifyEntityChanged(Entity*)
{
    parent->repaintDrawingArea();
}
