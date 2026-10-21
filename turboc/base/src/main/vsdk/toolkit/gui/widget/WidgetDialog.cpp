#include <stddef.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"

WidgetDialog::WidgetDialog()
    : id(""), name(""), orientation(ORIENTATION_VERTICAL), collapsable(false),
      titleKey(""), modal(false)
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

bool WidgetDialog::isModal() const
{
    return modal;
}

void WidgetDialog::setModal(bool modal)
{
    this->modal = modal;
}

const std::vector<WidgetDialogElement>& WidgetDialog::getElements() const
{
    return elements;
}

void WidgetDialog::addElement(const WidgetDialogElement& element)
{
    elements.push_back(element);
}

const WidgetDialogElement* WidgetDialog::getElementByAction(
    const String& action) const
{
    for ( size_t i = 0; i < elements.size(); i++ ) {
        const WidgetDialogElement* found = elements[i].findAction(action);
        if ( found != NULL ) {
            return found;
        }
    }
    return NULL;
}

bool WidgetDialog::isCollapsable() const
{
    return collapsable;
}

void WidgetDialog::setCollapsable(bool collapsable)
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
    for ( int j = 0; j < widgetElementList.size(); j++ ) {
        msg = msg + "    " + widgetElementList.get(j)->toString() + "\n";
    }
    for ( int j = 0; j < pendingCommandNames.size(); j++ ) {
        msg = msg + "    Commandname: " + pendingCommandNames.get(j) + "\n";
    }

    for ( int j = 0; j < pendingDialogRefNames.size(); j++ ) {
        msg = msg + "    DialogRefNames: " + pendingDialogRefNames.get(j) + "\n";
    }

    for ( int j = 0; j < pendingVariableNames.size(); j++ ) {
        msg = msg + "    VariableNames: " + pendingVariableNames.get(j) + "\n";
    }
    return msg;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int WidgetDialog::ORIENTATION_HORIZONTAL;
const int WidgetDialog::ORIENTATION_VERTICAL;
