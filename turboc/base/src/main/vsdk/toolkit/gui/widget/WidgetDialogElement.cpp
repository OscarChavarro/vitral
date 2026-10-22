#include <stddef.h>
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "java/util/ArrayList.txx"

WidgetDialogElement::WidgetDialogElement()
    : kind(""), id(""), labelKey(""), binding(""), action(""), children()
{
}

WidgetDialogElement::WidgetDialogElement(
    const String& kind, const String& id,
    const String& labelKey, const String& binding,
    const String& action,
    const ArrayList<WidgetDialogElement>& children)
    : kind(kind), id(id), labelKey(labelKey), binding(binding),
      action(action), children(children)
{
}

const String& WidgetDialogElement::getKind() const
{
    return kind;
}

const String& WidgetDialogElement::getId() const
{
    return id;
}

const String& WidgetDialogElement::getLabelKey() const
{
    return labelKey;
}

const String& WidgetDialogElement::getBinding() const
{
    return binding;
}

const String& WidgetDialogElement::getAction() const
{
    return action;
}

const ArrayList<WidgetDialogElement>& WidgetDialogElement::getChildren() const
{
    return children;
}

const WidgetDialogElement* WidgetDialogElement::findAction(
    const String& action) const
{
    if ( !action.isEmpty() && this->action.equals(action) ) {
        return this;
    }
    for ( long i = 0; i < children.size(); i++ ) {
        const WidgetDialogElement* found = children[i].findAction(action);
        if ( found != NULL ) {
            return found;
        }
    }
    return NULL;
}
