#ifndef __WIDGET_DIALOG_ELEMENT__
#define __WIDGET_DIALOG_ELEMENT__

#include "java/util/ArrayList.h"

#include "java/lang/String.h"

/**
Ordered, toolkit-independent content of a `WidgetDialog` (the
`WidgetDialog.Element` of the Java port), as read from the dialog
definitions (`etc/gui/dialogs.json`) by `DialogPersistence`: its kind
(column, row, textInput, button, tree, action, fileChooser, fileFilter) and
the keys tying it to the messages, the values and the actions of the
application. Absent texts are empty strings (null in Java).

C++ port note: it is a class of its own, as in the TypeScript port, so it
can be used where the vitral `Widget` class and the Xt `Widget` type
collide (it does not depend on either).
*/
class WidgetDialogElement {
private:
    String kind;
    String id;
    String labelKey;
    String binding;
    String action;
    ArrayList<WidgetDialogElement> children;

public:
    /**
    Empty element, needed by the storage of `ArrayList`
    */
    WidgetDialogElement();

    WidgetDialogElement(const String& kind, const String& id,
                        const String& labelKey,
                        const String& binding,
                        const String& action,
                        const ArrayList<WidgetDialogElement>& children);

    const String& getKind() const;
    const String& getId() const;
    const String& getLabelKey() const;
    const String& getBinding() const;
    const String& getAction() const;
    const ArrayList<WidgetDialogElement>& getChildren() const;

    /**
    @return this element or the first of its descendants with the given
    action, or null
    */
    const WidgetDialogElement* findAction(const String& action) const;
};

#endif
