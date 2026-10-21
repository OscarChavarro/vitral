#include <stddef.h>
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/variable/WidgetVariable.h"


static bool containsElement(const ArrayList<WidgetElement*>& list,
                     const WidgetElement* element)
{
    long i;
    for ( i = 0; i < list.size(); i++ ) {
        if ( list.get(i) == element ) {
            return true;
        }
    }
    return false;
}

static void collect(ArrayList<WidgetElement*>& owned, WidgetElement* element)
{
    if ( element == NULL || containsElement(owned, element) ) {
        return;
    }
    owned.add(element);

    WidgetMenu* menu = dynamic_cast<WidgetMenu*>(element);
    if ( menu != NULL ) {
        long i;
        for ( i = 0; i < menu->getChildren().size(); i++ ) {
            collect(owned, menu->getChildren().get(i));
        }
    }
    WidgetDialog* dialog = dynamic_cast<WidgetDialog*>(element);
    if ( dialog != NULL ) {
        long i;
        for ( i = 0; i < dialog->getChildren().size(); i++ ) {
            collect(owned, dialog->getChildren().get(i));
        }
    }
}


Widget::Widget() : menubar(NULL)
{
}

Widget::~Widget()
{
    ArrayList<WidgetElement*> owned;
    long i;

    for ( i = 0; i < commandList.size(); i++ ) {
        collect(owned, commandList.get(i));
    }
    for ( i = 0; i < variableList.size(); i++ ) {
        collect(owned, variableList.get(i));
    }
    collect(owned, menubar);
    for ( i = 0; i < popupMenuList.size(); i++ ) {
        collect(owned, popupMenuList.get(i));
    }
    for ( i = 0; i < buttonGroupList.size(); i++ ) {
        collect(owned, buttonGroupList.get(i));
    }
    for ( i = 0; i < dialogList.size(); i++ ) {
        collect(owned, dialogList.get(i));
    }
    for ( i = 0; i < owned.size(); i++ ) {
        delete owned.get(i);
    }
}

ArrayList<WidgetDialog*>& Widget::getDialogList()
{
    return dialogList;
}

void Widget::setDialogList(const ArrayList<WidgetDialog*>& dialogList)
{
    this->dialogList = dialogList;
}

void Widget::addMessage(const String& id, const String& message)
{
    messagesTable.put(id, message);
}

String Widget::getMessage(const String& id) const
{
    const String* msg;

    msg = messagesTable.get(id);

    if ( msg == NULL ) {
        return id;
    }
    return *msg;
}

void Widget::setMenubar(WidgetMenu* m)
{
    menubar = m;
}

WidgetMenu* Widget::getMenubar() const
{
    return menubar;
}

WidgetCommand* Widget::getCommandByName(const String& name) const
{
    WidgetCommand* command = NULL;
    WidgetCommand* candidate;
    int i;

    for ( i = 0; i < commandList.size(); i++ ) {
        candidate = commandList.get(i);
        if ( candidate->getId().equals(name) ) {
            command = candidate;
            break;
        }
    }
    return command;
}

WidgetButtonGroup* Widget::getButtonGroup(const String& name) const
{
    WidgetButtonGroup* group = NULL;
    WidgetButtonGroup* candidate;
    int i;

    for ( i = 0; i < buttonGroupList.size(); i++ ) {
        candidate = buttonGroupList.get(i);
        if ( candidate->getName().equals(name) ) {
            group = candidate;
            break;
        }
    }
    return group;
}

WidgetMenu* Widget::getPopup(const String& name) const
{
    WidgetMenu* menu = NULL;
    WidgetMenu* candidate;

    int i;
    for ( i = 0; i < popupMenuList.size(); i++ ) {
        candidate = popupMenuList.get(i);
        if ( candidate->getName().equals(name) ) {
            menu = candidate;
            break;
        }
    }
    return menu;
}

void Widget::addPopupMenu(WidgetMenu* p)
{
    popupMenuList.add(p);
}

void Widget::addCommand(WidgetCommand* c)
{
    commandList.add(c);
}

void Widget::addButtonGroup(WidgetButtonGroup* b)
{
    buttonGroupList.add(b);
}

void Widget::addDialog(WidgetDialog* dialog)
{
    dialogList.add(dialog);
}

WidgetDialog* Widget::getDialogById(const String& id) const
{
    for ( long i = 0; i < dialogList.size(); i++ ) {
        WidgetDialog* dialog = dialogList.get(i);
        if ( dialog != NULL && id.equals(dialog->getId()) ) {
            return dialog;
        }
    }
    return NULL;
}

void Widget::addVariable(WidgetVariable* variable)
{
    variableList.add(variable);
}

WidgetVariable* Widget::getVariableByName(const String& name) const
{
    int i;
    for ( i = 0; i < variableList.size(); i++ ) {
        if ( variableList.get(i)->getName().equals(name) ) {
            return variableList.get(i);
        }
    }
    return NULL;
}

String Widget::toString() const
{
    String msg = "= Widget report =========================================================\n";
    msg = msg + "Widget cache structure contains " +
        String::valueOf((long)popupMenuList.size()) +
        " popup submenu structures registered\n";
    msg = msg + "Widget cache structure contains " +
        String::valueOf((long)commandList.size()) +
        " commands registered\n";

    int i;
    for ( i = 0; i < commandList.size(); i++ ) {
        msg = msg + commandList.get(i)->toString();
    }

    if ( menubar == NULL ) {
        msg = msg + "There is NO menubar!";
    }
    else {
        msg = msg + "There is a menubar active, called \"" +
            menubar->getName() + "\"\n";
        msg = msg + "Dumping menubar tree structure...\n";
        msg = msg + menubar->toString();
    }

    //-----------------------------------------------------------------------
    for ( i = 0; i < dialogList.size(); i++ ) {
        msg = msg + dialogList.get(i)->toString();
    }

    for ( i = 0; i < variableList.size(); i++ ) {
        msg = msg + variableList.get(i)->toString();
    }
    //-----------------------------------------------------------------------
    msg = msg + "===========================================================================\n";

    return msg;
}
