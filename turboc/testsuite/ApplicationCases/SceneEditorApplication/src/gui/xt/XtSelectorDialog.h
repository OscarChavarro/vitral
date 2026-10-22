#ifndef __XT_SELECTOR_DIALOG__
#define __XT_SELECTOR_DIALOG__

#include "java/lang/String.h"

// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/Intrinsic.h>
#undef Object
#undef String

#include "model/dialog/SceneHierarchyPresenter.h"
#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"
#include "vsdk/toolkit/render/xt/XtDialogHost.h"
#include "vsdk/toolkit/render/xt/XtTreeView.h"
#include "java/lang/IllegalArgumentException.h"

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
class XtSelectorDialog : private XtDialogHost::MessageSource {
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
    @throws IllegalArgumentException for actions not in the definition
    */
    void performAction(const String& action);

private:
    // Callbacks of the presenter and of the tree.
    class RepaintRequest : public Runnable {
    public:
        explicit RepaintRequest(XtApplicationHost* host) : host(host) {}
        virtual void run();
    private:
        XtApplicationHost* host;
    };
    class TreeSelectionListener : public Runnable {
    public:
        explicit TreeSelectionListener(XtSelectorDialog* dialog) : dialog(dialog) {}
        virtual void run() { dialog->applyTreeSelection(); }
    private:
        XtSelectorDialog* dialog;
    };
    class TreeDeleteListener : public Runnable {
    public:
        explicit TreeDeleteListener(XtSelectorDialog* dialog) : dialog(dialog) {}
        virtual void run() { dialog->performAction("deleteSelected"); }
    private:
        XtSelectorDialog* dialog;
    };
    class TreeContextMenuListener : public XtTreeView::ContextMenuListener {
    public:
        explicit TreeContextMenuListener(XtSelectorDialog* dialog) : dialog(dialog) {}
        virtual void contextMenuRequested(int rootX, int rootY)
        {
            dialog->showPopup(rootX, rootY);
        }
    private:
        XtSelectorDialog* dialog;
    };
    friend class TreeSelectionListener;
    friend class TreeDeleteListener;
    friend class TreeContextMenuListener;

    XtApplicationHost* host;
    // Must be built before the presenter and the form that use them
    RepaintRequest repaintRequest;
    TreeSelectionListener treeSelectionListener;
    TreeDeleteListener treeDeleteListener;
    TreeContextMenuListener treeContextMenuListener;
    SceneHierarchyPresenter presenter;
    XtDialogHost::Form* form;
    DialogTreeNode hierarchy;
    bool updating;
    Widget popupMenu;

    // XtDialogHost::MessageSource
    virtual String message(const String& key);
    void applyTreeSelection();
    void showPopup(int rootX, int rootY);
    void collectSelected(const DialogTreeNode* node,
                         ArrayList<const DialogTreeNode*>& selected);

    static void popupChosen(int index, void* clientData);

    XtSelectorDialog(const XtSelectorDialog& other);
    XtSelectorDialog& operator=(const XtSelectorDialog& other);
};

#endif
