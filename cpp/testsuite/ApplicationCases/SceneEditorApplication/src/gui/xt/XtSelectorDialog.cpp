#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"

#include <X11/Shell.h>

#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtSelectorDialog.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "vsdk/toolkit/render/xt/XtTreeView.h"

const char* const XtSelectorDialog::DIALOG_ID = "scene.selector";

namespace {
const char* TREE_BINDING = "sceneHierarchy";
}

XtSelectorDialog::XtSelectorDialog(XtApplicationHost* host)
    : host(host),
      presenter(host->getApplicationModel(),
                [host]() { host->repaintDrawingArea(); }),
      form(nullptr),
      hierarchy("Scene", nullptr),
      updating(false),
      popupMenu(nullptr)
{
    const WidgetDialog* definition = host->getDialogById(DIALOG_ID);
    if ( definition == nullptr ) {
        throw java::IllegalStateException("Missing scene.selector dialog");
    }
    XtApplicationHost* messages = host;
    form = XtDialogHost::form(
        host->createDialogShell("sceneSelector", transientShellWidgetClass, ""),
        host->getUiFactory()->getWidgetSet(), host->getPanelFontSet(),
        [messages](const java::String& key) {
            return messages->getMessage(key.c_str());
        },
        definition, [](const java::String&) {});
    XtTreeView* tree = form->tree(TREE_BINDING);
    if ( tree == nullptr ) {
        delete form;
        throw java::IllegalStateException("The scene.selector dialog has no tree");
    }
    tree->setSelectionListener([this]() { applyTreeSelection(); });
    tree->setDeleteListener([this]() { performAction("deleteSelected"); });
    tree->setContextMenuListener([this](int rootX, int rootY) {
        showPopup(rootX, rootY);
    });
    rebuild();
}

XtSelectorDialog::~XtSelectorDialog()
{
    // The popup menu belongs to the tree, destroyed with the form
    delete form;
}

void XtSelectorDialog::rebuild()
{
    XtTreeView* tree = form->tree(TREE_BINDING);
    updating = true;
    // The tree references the rows: they are replaced after it forgets them
    tree->setRoot(nullptr);
    hierarchy = presenter.hierarchy();
    tree->setRoot(&hierarchy);
    java::ArrayList<const DialogTreeNode*> selected;
    collectSelected(&hierarchy, selected);
    tree->setSelection(selected);
    updating = false;
}

void XtSelectorDialog::collectSelected(
    const DialogTreeNode* node, java::ArrayList<const DialogTreeNode*>& selected)
{
    if ( presenter.isSelected(node) ) {
        selected.add(node);
    }
    for ( size_t i = 0; i < node->getChildren().size(); i++ ) {
        collectSelected(&node->getChildren()[i], selected);
    }
}

void XtSelectorDialog::setVisible(bool visible)
{
    if ( visible ) {
        form->show();
    }
    else {
        form->close();
    }
}

bool XtSelectorDialog::isVisible() const
{
    return form->isVisible();
}

void XtSelectorDialog::applyTreeSelection()
{
    if ( !updating ) {
        presenter.select(form->tree(TREE_BINDING)->getSelection());
    }
}

void XtSelectorDialog::selectBody(int index)
{
    XtTreeView* tree = form->tree(TREE_BINDING);
    if ( index < 0 ) {
        tree->selectOnly(nullptr);
        return;
    }
    // The geometry objects are the third group, as in the Java selector
    const DialogTreeNode& bodies = hierarchy.getChildren()[2];
    tree->selectOnly(&bodies.getChildren()[index]);
}

void XtSelectorDialog::performAction(const java::String& action)
{
    if ( !form->hasAction(action) ) {
        throw java::IllegalArgumentException("Unknown selector action: " + action);
    }
    if ( action == "deleteSelected" ) {
        presenter.deleteSelected();
        rebuild();
    }
    else if ( action == "toggleVisibility" ) {
        java::ArrayList<SimpleBody*> bodies = presenter.selectedBodies(
            form->tree(TREE_BINDING)->getSelection());
        if ( !bodies.isEmpty() ) {
            presenter.setHidden(bodies, !presenter.anyHidden(bodies));
            rebuild();
        }
    }
}

void XtSelectorDialog::showPopup(int rootX, int rootY)
{
    XtTreeView* tree = form->tree(TREE_BINDING);
    java::ArrayList<SimpleBody*> bodies = presenter.selectedBodies(tree->getSelection());
    if ( bodies.isEmpty() ) {
        return;
    }
    bool anyHidden = presenter.anyHidden(bodies);
    XtWidgetSet::MenuChoice item;
    item.label = presenter.text(anyHidden ? "IDM_SELECTOR_SHOW" : "IDM_SELECTOR_HIDE",
        anyHidden ? "Show objects" : "Hide objects").c_str();
    item.checked = false;
    item.separator = false;
    java::ArrayList<XtWidgetSet::MenuChoice> items = java::Collections::nCopies<XtWidgetSet::MenuChoice>(1, item);
    XtWidgetSet* widgetSet = host->getUiFactory()->getWidgetSet();
    if ( popupMenu != nullptr ) {
        XtDestroyWidget(popupMenu);
    }
    popupMenu = widgetSet->createChoiceMenu(tree->getWidget(), items,
        host->getPanelFontSet(), &XtSelectorDialog::popupChosen, this);
    widgetSet->popupChoiceMenu(popupMenu, rootX, rootY, nullptr, nullptr);
}

void XtSelectorDialog::popupChosen(int, void* clientData)
{
    static_cast<XtSelectorDialog*>(clientData)->performAction("toggleVisibility");
}
