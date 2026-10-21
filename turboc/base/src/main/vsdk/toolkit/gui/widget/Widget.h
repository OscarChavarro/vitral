#ifndef __WIDGET__
#define __WIDGET__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/gui/PresentationElement.h"

class WidgetButtonGroup;
class WidgetCommand;
class WidgetDialog;
class WidgetElement;
class WidgetMenu;
class WidgetVariable;

/**
In order to understand this class, the following concepts must be taken into
account: - WidgetVariable - WidgetCommand - Reflection (introspection) design
pattern - Menubars, buttons bars, and other are based upon WidgetCommands -
Dialogs are based upon WidgetVariables and WidgetCommands - Dialogs and menus are
hierarchical

C++ port note: a Widget owns every element reachable from it (commands,
variables, button groups, dialogs and their children, menubar, popups and
their children); shared elements are deleted once.
*/
class Widget : public PresentationElement {
private:
    // Basic / fundamental / atomic elements
    ArrayList<WidgetCommand*> commandList;
    ArrayList<WidgetVariable*> variableList;
    HashMap<String, String> messagesTable;
    // Composite elements
    WidgetMenu* menubar;
    ArrayList<WidgetMenu*> popupMenuList;
    ArrayList<WidgetButtonGroup*> buttonGroupList;
    ArrayList<WidgetDialog*> dialogList;

    Widget(const Widget& other);
    Widget& operator=(const Widget& other);

public:
    Widget();
    virtual ~Widget();

    ArrayList<WidgetDialog*>& getDialogList();
    void setDialogList(const ArrayList<WidgetDialog*>& dialogList);
    void addMessage(const String& id, const String& message);

    /**
    @param id message identifier
    @return registered message, or the id itself if it has no message
    */
    String getMessage(const String& id) const;
    void setMenubar(WidgetMenu* m);
    WidgetMenu* getMenubar() const;
    WidgetCommand* getCommandByName(const String& name) const;
    WidgetButtonGroup* getButtonGroup(const String& name) const;
    WidgetMenu* getPopup(const String& name) const;
    void addPopupMenu(WidgetMenu* p);
    void addCommand(WidgetCommand* c);
    void addButtonGroup(WidgetButtonGroup* b);
    void addDialog(WidgetDialog* dialog);

    /**
    @return the dialog with the given identifier, or null
    */
    WidgetDialog* getDialogById(const String& id) const;
    void addVariable(WidgetVariable* variable);
    WidgetVariable* getVariableByName(const String& name) const;
    String toString() const;
};

#endif
