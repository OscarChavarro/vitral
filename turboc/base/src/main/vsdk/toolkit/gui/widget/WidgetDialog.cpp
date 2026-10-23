#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"

WidgetDialog::WidgetDialog()
    : id(""), name(""), orientation(ORIENTATION_VERTICAL), collapsable(FALSE),
      titleKey(""), modal(FALSE)
{
}

const String& WidgetDialog::getTitleKey() const
{
    return titleKey;
}

void WidgetDialog::setTitleKey(const String& titleKey)
{
    this->titleKey = titleKey;
}

BOOLEAN WidgetDialog::isModal() const
{
    return modal;
}

void WidgetDialog::setModal(BOOLEAN modal)
{
    this->modal = modal;
}

const ArrayList<WidgetDialogElement>& WidgetDialog::getElements() const
{
    return elements;
}

void WidgetDialog::addElement(const WidgetDialogElement& element)
{
    elements.add(element);
}

const WidgetDialogElement* WidgetDialog::getElementByAction(
    const String& action) const
{
    for ( long i = 0; i < elements.size(); i++ ) {
        const WidgetDialogElement* found = elements[i].findAction(action);
        if ( found != NULL ) {
            return found;
        }
    }
    return NULL;
}

BOOLEAN WidgetDialog::isCollapsable() const
{
    return collapsable;
}

void WidgetDialog::setCollapsable(BOOLEAN collapsable)
{
    this->collapsable = collapsable;
}

ArrayList<String>& WidgetDialog::getPendingCommandNames()
{
    return pendingCommandNames;
}

void WidgetDialog::setPendingCommandNames(
    const ArrayList<String>& pendingCommandNames)
{
    this->pendingCommandNames = pendingCommandNames;
}

ArrayList<WidgetElement*>& WidgetDialog::getWidgetElementList()
{
    return widgetElementList;
}

void WidgetDialog::setWidgetElementList(
    const ArrayList<WidgetElement*>& widgetElementList)
{
    this->widgetElementList = widgetElementList;
}

const String& WidgetDialog::getId() const
{
    return id;
}

void WidgetDialog::setId(const String& id)
{
    this->id = id;
}

const String& WidgetDialog::getName() const
{
    return name;
}

void WidgetDialog::setName(const String& name)
{
    this->name = name;
}

double WidgetDialog::getOrientation() const
{
    return orientation;
}

void WidgetDialog::setOrientation(int orientation)
{
    this->orientation = orientation;
}

ArrayList<String>& WidgetDialog::getPendingVariableNames()
{
    return pendingVariableNames;
}

void WidgetDialog::setPendingVariableNames(
    const ArrayList<String>& pendingVariableNames)
{
    this->pendingVariableNames = pendingVariableNames;
}

ArrayList<String>& WidgetDialog::getPendingDialogNames()
{
    return pendingDialogNames;
}

void WidgetDialog::setPendingDialogNames(
    const ArrayList<String>& pendingDialogNames)
{
    this->pendingDialogNames = pendingDialogNames;
}

ArrayList<WidgetElement*>& WidgetDialog::getChildren()
{
    return widgetElementList;
}

void WidgetDialog::setChildren(
    const ArrayList<WidgetElement*>& widgetElementList)
{
    this->widgetElementList = widgetElementList;
}

ArrayList<String>& WidgetDialog::getPendingDialogRefNames()
{
    return pendingDialogRefNames;
}

void WidgetDialog::setPendingDialogRefNames(
    const ArrayList<String>& pendingDialogRefNames)
{
    this->pendingDialogRefNames = pendingDialogRefNames;
}

void WidgetDialog::associateVariable(const String& variableName)
{
    Logger::reportMessage("WidgetDialog", Logger::FATAL_ERROR,
        "associateVariable",
        String("Variable ") + variableName + " not found!");
}

String WidgetDialog::toString() const
{
    String msg = "";

    msg = msg + "    DIALOG: " + getId() + "\n";
    int j;
    for ( j = 0; j < widgetElementList.size(); j++ ) {
        msg = msg + "    " + widgetElementList.get(j)->toString() + "\n";
    }
    for ( j = 0; j < pendingCommandNames.size(); j++ ) {
        msg = msg + "    Commandname: " + pendingCommandNames.get(j) + "\n";
    }

    for ( j = 0; j < pendingDialogRefNames.size(); j++ ) {
        msg = msg + "    DialogRefNames: " + pendingDialogRefNames.get(j) + "\n";
    }

    for ( j = 0; j < pendingVariableNames.size(); j++ ) {
        msg = msg + "    VariableNames: " + pendingVariableNames.get(j) + "\n";
    }
    return msg;
}

const Class WidgetDialog::CLASS = { "WidgetDialog", &WidgetElement::CLASS };

const Class *
WidgetDialog::getClass() const
{
    return &CLASS;
}
