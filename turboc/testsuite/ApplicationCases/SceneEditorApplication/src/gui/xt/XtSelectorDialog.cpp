#include "java/util/ArrayList.txx"

// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/Shell.h>
#undef Object
#undef String

#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtSelectorDialog.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "vsdk/toolkit/render/xt/XtTreeView.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"

const char* const XtSelectorDialog::DIALOG_ID = "scene.selector";

static const char* TREE_BINDING = "sceneHierarchy";

XtSelectorDialog::XtSelectorDialog(XtApplicationHost* host)
    : host(host),
      repaintRequest(host),
      treeSelectionListener(this),
      treeDeleteListener(this),
      treeContextMenuListener(this),
      presenter(host->getApplicationModel(), &repaintRequest),
      form(NULL),
      hierarchy("Scene", NULL),
      updating(false),
      popupMenu(NULL)
{
    const WidgetDialog* definition = host->getDialogById(DIALOG_ID);
    if ( definition == NULL ) {
        throw IllegalStateException("Missing scene.selector dialog");
    }
    // Its buttons do nothing: its actions come from the tree
    form = XtDialogHost::form(
        host->createDialogShell("sceneSelector", transientShellWidgetClass, ""),
        host->getUiFactory()->getWidgetSet(), host->getPanelFontSet(),
        this, definition, NULL);
    XtTreeView* tree = form->tree(TREE_BINDING);
    if ( tree == NULL ) {
        delete form;
        throw IllegalStateException("The scene.selector dialog has no tree");
    }
    tree->setSelectionListener(&treeSelectionListener);
    tree->setDeleteListener(&treeDeleteListener);
    tree->setContextMenuListener(&treeContextMenuListener);
    rebuild();
}

void XtSelectorDialog::RepaintRequest::run()
{
    host->repaintDrawingArea();
}

String XtSelectorDialog::message(const String& key)
{
    return host->getMessage(key.c_str());
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
    tree->setRoot(NULL);
    hierarchy = presenter.hierarchy();
    tree->setRoot(&hierarchy);
    ArrayList<const DialogTreeNode*> selected;
    collectSelected(&hierarchy, selected);
    tree->setSelection(selected);
    updating = false;
}

void XtSelectorDialog::collectSelected(
    const DialogTreeNode* node, ArrayList<const DialogTreeNode*>& selected)
{
    if ( presenter.isSelected(node) ) {
        selected.add(node);
    }
    for ( long i = 0; i < node->getChildren().size(); i++ ) {
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
        tree->selectOnly(NULL);
        return;
    }
    // The geometry objects are the third group, as in the Java selector
    const DialogTreeNode& bodies = hierarchy.getChildren()[2];
    tree->selectOnly(&bodies.getChildren()[index]);
}

void XtSelectorDialog::performAction(const String& action)
{
    if ( !form->hasAction(action) ) {
        throw IllegalArgumentException("Unknown selector action: " + action);
    }
    if ( action == "deleteSelected" ) {
        presenter.deleteSelected();
        rebuild();
    }
    else if ( action == "toggleVisibility" ) {
        ArrayList<SimpleBody*> bodies = presenter.selectedBodies(
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
    ArrayList<SimpleBody*> bodies = presenter.selectedBodies(tree->getSelection());
    if ( bodies.isEmpty() ) {
        return;
    }
    bool anyHidden = presenter.anyHidden(bodies);
    XtWidgetSet::MenuChoice item;
    item.label = presenter.text(anyHidden ? "IDM_SELECTOR_SHOW" : "IDM_SELECTOR_HIDE",
        anyHidden ? "Show objects" : "Hide objects").c_str();
    item.checked = false;
    item.separator = false;
    ArrayList<XtWidgetSet::MenuChoice> items;
    items.add(item);
    XtWidgetSet* widgetSet = host->getUiFactory()->getWidgetSet();
    if ( popupMenu != NULL ) {
        XtDestroyWidget(popupMenu);
    }
    popupMenu = widgetSet->createChoiceMenu(tree->getWidget(), items,
        host->getPanelFontSet(), &XtSelectorDialog::popupChosen, this);
    widgetSet->popupChoiceMenu(popupMenu, rootX, rootY, NULL, NULL);
}

void XtSelectorDialog::popupChosen(int, void* clientData)
{
    ((XtSelectorDialog*)(clientData))->performAction("toggleVisibility");
}
