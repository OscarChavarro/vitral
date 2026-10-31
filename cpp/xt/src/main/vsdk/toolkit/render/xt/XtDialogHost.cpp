// Before any other header that could include the Xt ones
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/gui/XtIntrinsics.h"


#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "vsdk/toolkit/render/xt/XtDialogHost.h"
#include "vsdk/toolkit/render/xt/XtTreeView.h"

namespace {
const int DIALOG_WIDTH = 560;
/// Size of the dialogs with trees, as the 450x560 Swing scene selector
const int TREE_DIALOG_WIDTH = 450;
const int TREE_HEIGHT = 500;
const int MARGIN = 12;
const int LINE_HEIGHT = 20;
const int FIELD_HEIGHT = 28;
const int BUTTON_HEIGHT = 28;
const int BUTTON_GAP = 10;
const int BUTTON_PADDING = 28;

int textWidth(XFontSet fontSet, const java::String& text)
{
    if ( fontSet == nullptr ) {
        return static_cast<int>(text.size()) * 8;
    }
    return XmbTextEscapement(fontSet, text.c_str(),
                             static_cast<int>(text.size()));
}

/**
Splits a text in lines no wider than the given width, at blanks, as a
wrapping label.
*/
java::ArrayList<java::String> wrapText(XFontSet fontSet, const java::String& text,
                                  int width)
{
    java::ArrayList<java::String> lines;
    java::String line;
    size_t start = 0;
    while ( start < text.size() ) {
        size_t end = text.find(' ', start);
        if ( end == -1 ) end = text.size();
        java::String word = text.substr(start, end - start);
        java::String candidate = line.isEmpty() ? word : line + " " + word;
        if ( !line.isEmpty() && textWidth(fontSet, candidate) > width ) {
            lines.add(line);
            line = word;
        }
        else {
            line = candidate;
        }
        start = end + 1;
    }
    if ( !line.isEmpty() ) lines.add(line);
    return lines;
}

/// Swing `JTextField`s without an action listener ignore Return
void ignoreActivation(XtWidget, void*)
{
}
}

struct XtDialogHost::Form::Button {
    Form* form;
    java::String action;
    java::String label;
    XtWidget widget;
};

XtDialogHost::Form::Form(XtWidgetSet* widgetSet, XFontSet fontSet,
                         XtWidget shell, const WidgetDialog* definition,
                         const ActionListener& onAction)
    : widgetSet(widgetSet), definition(definition), fontSet(fontSet),
      shell(shell),
      firstField(nullptr), modal(definition->isModal()), visible(false),
      width(DIALOG_WIDTH), buttonsY(0), dialogSession(definition),
      onAction(onAction)
{
}

XtDialogHost::Form::~Form()
{
    for ( long i = 0; i < buttons.size(); i++ ) {
        delete buttons[i];
    }
    for (long tree = 0; tree < trees.size(); tree++) {
        delete trees.getValue(tree);
    }
    if ( shell != nullptr ) {
        XtDestroyWidget(shell);
    }
}

XtDialogHost::Form* XtDialogHost::form(XtWidget shell, XtWidgetSet* widgetSet,
                                       XFontSet fontSet,
                                       const MessageSource& messages,
                                       const WidgetDialog* definition,
                                       const ActionListener& onAction)
{
    Form* form = new Form(widgetSet, fontSet, shell, definition, onAction);
    try {
        XtPanelWidgets* widgets = widgetSet->getPanelWidgets();
        java::String title = messages.apply(definition->getTitleKey().c_str());
        XtVaSetValues(shell, XtNtitle, title.c_str(), nullptr);
        widgetSet->setWindowCloseHandler(shell, &Form::closeRequested, form);

        // The elements, in order: rows and columns only group them (as the
        // Swing host does), and the buttons go to the bottom row
        java::ArrayList<const WidgetDialogElement*> pending;
        for ( size_t i = definition->getElements().size(); i > 0; i-- ) {
            pending.add(&definition->getElements()[i - 1]);
        }
        java::ArrayList<const WidgetDialogElement*> inputs;
        java::ArrayList<const WidgetDialogElement*> buttons;
        bool withTree = false;
        while ( !pending.isEmpty() ) {
            const WidgetDialogElement* element = pending[pending.size() - 1];
            pending.remove(pending.size() - 1);
            java::String kind = element->getKind().c_str();
            if ( kind == "textInput" || kind == "tree" ) {
                inputs.add(element);
                withTree = withTree || kind == "tree";
            }
            else if ( kind == "action" ) {
                // Offered by the application with keys or menus
            }
            else if ( kind == "button" ) {
                buttons.add(element);
            }
            else if ( kind == "row" || kind == "column" ) {
                const java::ArrayList<WidgetDialogElement>& children =
                    element->getChildren();
                for ( size_t i = children.size(); i > 0; i-- ) {
                    pending.add(&children[i - 1]);
                }
            }
            else {
                throw java::IllegalArgumentException(
                    "Unsupported form element: " + kind);
            }
        }

        if ( withTree ) {
            form->width = TREE_DIALOG_WIDTH;
        }
        java::ArrayList<java::ArrayList<java::String> > prompts;
        int height = MARGIN;
        for ( size_t i = 0; i < inputs.size(); i++ ) {
            java::String labelKey = inputs[i]->getLabelKey().c_str();
            prompts.add(labelKey.isEmpty() ? java::ArrayList<java::String>() :
                wrapText(fontSet, messages.apply(labelKey), form->width - 2 * MARGIN));
            height += static_cast<int>(prompts[i].size()) * LINE_HEIGHT +
                (prompts[i].isEmpty() ? 0 : 6) +
                (inputs[i]->getKind().equals("tree") ? TREE_HEIGHT : FIELD_HEIGHT) +
                MARGIN;
        }
        form->buttonsY = height;
        if ( !buttons.isEmpty() ) {
            height += BUTTON_HEIGHT + MARGIN;
        }

        XtWidget frame = widgets->createPanel(shell, "dialogFrame", 0, 0,
                                              form->width, height, true);
        int y = MARGIN;
        for ( size_t i = 0; i < inputs.size(); i++ ) {
            for ( size_t line = 0; line < prompts[i].size(); line++ ) {
                widgets->createLabel(frame, prompts[i][line], fontSet,
                                     XtPanelWidgets::LEFT, MARGIN, y,
                                     form->width - 2 * MARGIN, LINE_HEIGHT);
                y += LINE_HEIGHT;
            }
            if ( !prompts[i].isEmpty() ) {
                y += 6;
            }
            if ( inputs[i]->getKind().equals("tree") ) {
                XtTreeView* tree = new XtTreeView(frame, fontSet, MARGIN, y,
                    form->width - 2 * MARGIN, TREE_HEIGHT);
                form->trees.put(inputs[i]->getBinding().c_str(), tree);
                if ( form->firstField == nullptr ) {
                    form->firstField = tree->getWidget();
                }
                y += TREE_HEIGHT + MARGIN;
                continue;
            }
            XtWidget field = widgets->createTextField(frame, "", fontSet,
                MARGIN, y, form->width - 2 * MARGIN, FIELD_HEIGHT,
                ignoreActivation, nullptr);
            form->fields.put(inputs[i]->getBinding().c_str(), field);
            if ( form->firstField == nullptr ) {
                form->firstField = field;
            }
            y += FIELD_HEIGHT + MARGIN;
        }
        for ( size_t i = 0; i < buttons.size(); i++ ) {
            Form::Button* button = new Form::Button;
            button->form = form;
            button->action = buttons[i]->getAction().c_str();
            button->label = messages.apply(buttons[i]->getLabelKey().c_str());
            button->widget = widgets->createPushButton(frame, button->label,
                fontSet, 0, form->buttonsY, 1, BUTTON_HEIGHT,
                &Form::buttonPressed, button);
            form->buttons.add(button);
        }
        form->layoutButtons();
    }
    catch ( ... ) {
        delete form;
        throw;
    }
    return form;
}

/**
Sizes the buttons to their texts and centers their row.
*/
void XtDialogHost::Form::layoutButtons()
{
    int rowWidth = 0;
    java::ArrayList<int> widths;
    for ( size_t i = 0; i < buttons.size(); i++ ) {
        widths.add(textWidth(fontSet, buttons[i]->label) +
                         BUTTON_PADDING);
        rowWidth += widths[i] + (i > 0 ? BUTTON_GAP : 0);
    }
    int x = java::Math::max(MARGIN, (width - rowWidth) / 2);
    for ( size_t i = 0; i < buttons.size(); i++ ) {
        XtVaSetValues(buttons[i]->widget, XtNx, x, XtNy, buttonsY,
                      XtNwidth, widths[i], XtNheight, BUTTON_HEIGHT, nullptr);
        x += widths[i] + BUTTON_GAP;
    }
}

XtDialogHost::Form::Button* XtDialogHost::Form::findButton(
    const java::String& action) const
{
    for ( size_t i = 0; i < buttons.size(); i++ ) {
        if ( buttons[i]->action == action ) {
            return buttons[i];
        }
    }
    return nullptr;
}

XtWidget XtDialogHost::Form::dialog() const
{
    return shell;
}

XtWidget XtDialogHost::Form::field(const java::String& binding) const
{
    XtWidget const* found = fields.get(binding);
    return found != nullptr ? (*found) : nullptr;
}

XtWidget XtDialogHost::Form::action(const java::String& action) const
{
    Button* button = findButton(action);
    return button != nullptr ? button->widget : nullptr;
}

XtTreeView* XtDialogHost::Form::tree(const java::String& binding) const
{
    XtTreeView* const* found = trees.get(binding);
    return found != nullptr ? (*found) : nullptr;
}

bool XtDialogHost::Form::hasAction(const java::String& action) const
{
    return definition->getElementByAction(action.c_str()) != nullptr;
}

DialogSession* XtDialogHost::Form::session()
{
    return &dialogSession;
}

java::String XtDialogHost::Form::getText(const java::String& binding) const
{
    XtWidget widget = field(binding);
    return widget != nullptr ?
        widgetSet->getPanelWidgets()->getText(widget) : java::String();
}

void XtDialogHost::Form::setText(const java::String& binding,
                                 const java::String& text)
{
    XtWidget widget = field(binding);
    if ( widget != nullptr ) {
        widgetSet->getPanelWidgets()->setText(widget, text);
    }
}

java::String XtDialogHost::Form::getActionLabel(const java::String& action) const
{
    Button* button = findButton(action);
    return button != nullptr ? button->label : java::String();
}

void XtDialogHost::Form::setActionLabel(const java::String& action,
                                        const java::String& label)
{
    Button* button = findButton(action);
    if ( button == nullptr ) {
        return;
    }
    button->label = label;
    widgetSet->getPanelWidgets()->setLabel(button->widget, label);
    layoutButtons();
}

void XtDialogHost::Form::show()
{
    XtPopup(shell, modal ? XtGrabExclusive : XtGrabNone);
    visible = true;
    XRaiseWindow(XtDisplay(shell), XtWindow(shell));
    if ( firstField != nullptr ) {
        widgetSet->setKeyboardFocus(shell, firstField);
    }
}

void XtDialogHost::Form::close()
{
    dialogSession.close();
    if ( visible ) {
        visible = false;
        XtPopdown(shell);
    }
}

bool XtDialogHost::Form::isVisible() const
{
    return visible;
}

void XtDialogHost::Form::buttonPressed(XtWidget, void* clientData)
{
    Button* button = static_cast<Button*>(clientData);
    // The listener may close the form: nothing of it is used afterwards
    ActionListener listener = button->form->onAction;
    java::String action = button->action;
    if ( listener ) {
        listener.accept(action);
    }
}

void XtDialogHost::Form::closeRequested(void* clientData)
{
    static_cast<Form*>(clientData)->close();
}
