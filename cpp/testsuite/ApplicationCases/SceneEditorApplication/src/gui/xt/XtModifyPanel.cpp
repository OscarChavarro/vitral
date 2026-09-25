#include "java/util/ArrayList.txx"
#include "gui/xt/XtGenericEditor.h"
#include "gui/xt/XtModifyPanel.h"
#include "gui/xt/XtModifyPanelHost.h"
#include "gui/xt/XtPanelWidgets.h"
#include "gui/xt/editor/XtModifyPanelForFunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"

XtModifyPanel::XtModifyPanel(XtModifyPanelHost* parent, Widget container,
                             int width)
    : parent(parent), target(nullptr), container(container), width(width),
      functionalExplicitSurfaceEditor(nullptr), genericEditor(nullptr),
      activeEditor(nullptr)
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
    XtPanelWidgets::removeAll(container);
    activeEditor = nullptr;

    if (dynamic_cast<FunctionalExplicitSurface*>(target->getGeometry()) != nullptr) {
        if (genericEditor != nullptr) {
            genericEditor->detach();
        }
        if (functionalExplicitSurfaceEditor == nullptr) {
            functionalExplicitSurfaceEditor =
                new XtModifyPanelForFunctionalExplicitSurface(parent);
        }
        functionalExplicitSurfaceEditor->notifyTargetBeginEdit(
            target, container, width);
        activeEditor = functionalExplicitSurfaceEditor;
    }
    else {
        if (genericEditor == nullptr) {
            genericEditor = new XtGenericEditor(
                container, parent->getPanelFontSet(), width);
            genericEditor->setListener(this);
        }
        genericEditor->build(target->getGeometry());
    }
}

void XtModifyPanel::notifyTargetEndEdit()
{
    target = nullptr;
    activeEditor = nullptr;
    if (container == nullptr) {
        // An editor building into the container of its owner panel
        return;
    }
    if (genericEditor != nullptr && genericEditor->isEntityDeleted()) {
        // Keep the "Entity deleted" message the editor is showing
        return;
    }
    if (genericEditor != nullptr) {
        genericEditor->detach();
    }
    XtPanelWidgets::removeAll(container);
    XtPanelWidgets::createLabel(container, "No selected object for modifying.",
                                parent->getPanelFontSet(),
                                XtPanelWidgets::LEFT, 8, 8, width - 16, 24);
}

java::ArrayList<RenderPrimitive> XtModifyPanel::buildEditFeedback()
{
    if (activeEditor != nullptr) {
        return activeEditor->buildEditFeedback();
    }
    return java::ArrayList<RenderPrimitive>();
}

void XtModifyPanel::notifyEntityChanged(Entity*)
{
    parent->repaintDrawingArea();
}
