#ifndef __XT_SELECTOR_DIALOG__
#define __XT_SELECTOR_DIALOG__

#include <string>

#include <X11/Intrinsic.h>

#include "model/dialog/SceneHierarchyPresenter.h"
#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"
#include "vsdk/toolkit/render/xt/XtDialogHost.h"

class XtApplicationHost;

/**
Dialog to select objects of the scene (`h` key), as `AwtSelectorDialog`:
an Xt adapter of the `scene.selector` definition of `etc/gui/dialogs.json`
(built by `XtDialogHost`) for the `SceneHierarchyPresenter`. Its tree shows
the lights, cameras and geometry objects of the scene; selecting rows
selects their elements (undoable), the Delete key runs the `deleteSelected`
action and the context menu of the selected bodies runs
`toggleVisibility` (hide or show them). It is not modal, and closing it
only hides it.
*/
class XtSelectorDialog {
public:
    static const char* const DIALOG_ID;

    explicit XtSelectorDialog(XtApplicationHost* host);
    ~XtSelectorDialog();

    /**
    Shows the current scene in the tree, with its selection.
    */
    void rebuild();

    /**
    @param visible true to show the dialog, false to hide it
    */
    void setVisible(bool visible);
    bool isVisible() const;

    /**
    Selects only a geometry object in the tree (automation service), as a
    click on its row does.
    @param index index of the body, or negative to clear the selection
    */
    void selectBody(int index);

    /**
    Runs an action of the definition: `deleteSelected` or
    `toggleVisibility`.
    @throws std::invalid_argument for actions not in the definition
    */
    void performAction(const std::string& action);

private:
    XtApplicationHost* host;
    SceneHierarchyPresenter presenter;
    XtDialogHost::Form* form;
    DialogTreeNode hierarchy;
    bool updating;
    Widget popupMenu;

    void applyTreeSelection();
    void showPopup(int rootX, int rootY);
    void collectSelected(const DialogTreeNode* node,
                         std::vector<const DialogTreeNode*>& selected);

    static void popupChosen(int index, void* clientData);

    XtSelectorDialog(const XtSelectorDialog& other);
    XtSelectorDialog& operator=(const XtSelectorDialog& other);
};

#endif
