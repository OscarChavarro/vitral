#ifndef __WIDGET_DIALOG__
#define __WIDGET_DIALOG__

#include <vector>

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "vsdk/toolkit/gui/widget/WidgetElement.h"

/**
This class plays a role of internal node on an n-ary tree in the composite
design pattern. Children are owned by the `Widget` context.
*/
class WidgetDialog : public WidgetElement {
private:
    String id;
    String name;
    int orientation;
    ArrayList<WidgetElement*> widgetElementList;
    /// In the importing from file process, a dialog could be "incomplete",
    /// due to having a variable reference to a variable that is not loaded
    /// yet.  In such situation, a two pass processing is performed:
    /// On first pass does not create neither associate any variable, but store
    /// its incomplete references (names) on this ArrayList. On second pass
    /// the factory traverse this list in order to add current variables
    /// from context. Check WidgetPersistence.importAquynzaWidgetDialog method.
    ArrayList<String> pendingVariableNames;
    ArrayList<String> pendingCommandNames;
    ArrayList<String> pendingDialogNames;
    ArrayList<String> pendingDialogRefNames;
    bool collapsable;
    String titleKey;
    bool modal;
    std::vector<WidgetDialogElement> elements;

public:
    static const int ORIENTATION_HORIZONTAL = 0x01;
    static const int ORIENTATION_VERTICAL = 0x02;

    WidgetDialog();
    virtual ~WidgetDialog() {}

    /**
    @return key of the message of the title of the dialog
    */
    const String& getTitleKey() const;
    void setTitleKey(const String& titleKey);
    bool isModal() const;
    void setModal(bool modal);

    /**
    @return the ordered, toolkit-independent content of the dialog
    */
    const std::vector<WidgetDialogElement>& getElements() const;
    void addElement(const WidgetDialogElement& element);

    /**
    @return the first element (in depth) with the given action, or null
    */
    const WidgetDialogElement* getElementByAction(
        const String& action) const;

    bool isCollapsable() const;
    void setCollapsable(bool collapsable);
    ArrayList<String>& getPendingCommandNames();
    void setPendingCommandNames(
        const ArrayList<String>& pendingCommandNames);
    ArrayList<WidgetElement*>& getWidgetElementList();
    void setWidgetElementList(
        const ArrayList<WidgetElement*>& widgetElementList);
    const String& getId() const;
    void setId(const String& id);
    const String& getName() const;
    void setName(const String& name);
    double getOrientation() const;
    void setOrientation(int orientation);
    ArrayList<String>& getPendingVariableNames();
    void setPendingVariableNames(
        const ArrayList<String>& pendingVariableNames);
    ArrayList<String>& getPendingDialogNames();
    void setPendingDialogNames(
        const ArrayList<String>& pendingDialogNames);
    ArrayList<WidgetElement*>& getChildren();
    void setChildren(const ArrayList<WidgetElement*>& widgetElementList);
    ArrayList<String>& getPendingDialogRefNames();
    void setPendingDialogRefNames(
        const ArrayList<String>& pendingDialogRefNames);

    /**
    Given a variableName, this method asks the context for a WidgetVariable
    pointer in order to reference the given variable. If that variable
    doesn't exist, an error is reported.
    */
    void associateVariable(const String& variableName);

    virtual String toString() const;
};

#endif
