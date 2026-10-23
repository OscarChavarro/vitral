// Before any other header that could include the Xt ones
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/XtIntrinsics.h"


#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtTextEncoding.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "vsdk/toolkit/render/xt/XtDialogHost.h"
#include "vsdk/toolkit/render/xt/XtTreeView.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.h"

static const int DIALOG_WIDTH = 560;
/// Size of the dialogs with trees, as the 450x560 Swing scene selector
static const int TREE_DIALOG_WIDTH = 450;
static const int TREE_HEIGHT = 500;
static const int MARGIN = 12;
static const int LINE_HEIGHT = 20;
static const int FIELD_HEIGHT = 28;
static const int BUTTON_HEIGHT = 28;
static const int BUTTON_GAP = 10;
static const int BUTTON_PADDING = 28;

static int textWidth(XFontSet fontSet, const String& text)
{
    if ( fontSet == NULL ) {
        return ((int)(text.size())) * 8;
    }
    return XmbTextEscapement(fontSet, text.c_str(),
                             ((int)(text.size())));
}

/**
Splits a text in lines no wider than the given width, at blanks, as a
wrapping label.
*/
static ArrayList<String> wrapText(XFontSet fontSet, const String& text,
                                  int width)
{
    ArrayList<String> lines;
    String line;
    int length = text.size();
    int start = 0;
    while ( start < length ) {
        int end = text.indexOf(' ', start);
        if ( end < 0 ) end = length;
        String word = text.substr(start, end - start);
        String candidate = line.isEmpty() ? word : line + " " + word;
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
static void ignoreActivation(XtWidget, void*)
{
}

struct XtDialogHost::Form::Button {
    Form* form;
    String action;
    String label;
    XtWidget widget;
};

XtDialogHost::Form::Form(XtWidgetSet* widgetSet, XFontSet fontSet,
                         XtWidget shell, const WidgetDialog* definition,
                         ActionListener* onAction)
    : widgetSet(widgetSet), definition(definition), fontSet(fontSet),
      shell(shell),
      firstField(NULL), modal(definition->isModal()), visible(FALSE),
      width(DIALOG_WIDTH), buttonsY(0), dialogSession(definition),
      onAction(onAction)
{
}

XtDialogHost::Form::~Form()
{
    long i;
    for ( i = 0; i < trees.size(); i++ ) {
        delete trees.getValue(i);
    }
    for ( i = 0; i < buttons.size(); i++ ) {
        delete buttons[i];
    }
    if ( shell != NULL ) {
        XtDestroyWidget(shell);
    }
}

XtDialogHost::Form* XtDialogHost::form(XtWidget shell, XtWidgetSet* widgetSet,
                                       XFontSet fontSet,
                                       MessageSource* messages,
                                       const WidgetDialog* definition,
                                       ActionListener* onAction)
{
    Form* form = new Form(widgetSet, fontSet, shell, definition, onAction);
    try {
        XtPanelWidgets* widgets = widgetSet->getPanelWidgets();
        String title = messages->message(definition->getTitleKey().c_str());
        const String shownTitle = XtTextEncoding::toLocale(title);
        XtVaSetValues(shell, XtNtitle, shownTitle.c_str(), NULL);
        widgetSet->setWindowCloseHandler(shell,
            &XtDialogHost::Form::closeRequested, form);

        // The elements, in order: rows and columns only group them (as the
        // Swing host does), and the buttons go to the bottom row
        ArrayList<const WidgetDialogElement*> pending;
        long i;
        for ( i = definition->getElements().size(); i > 0; i-- ) {
            pending.add(&definition->getElements()[i - 1]);
        }
        ArrayList<const WidgetDialogElement*> inputs;
        ArrayList<const WidgetDialogElement*> buttons;
        BOOLEAN withTree = FALSE;
        while ( !pending.isEmpty() ) {
            const WidgetDialogElement* element = pending[pending.size() - 1];
            pending.remove(pending.size() - 1);
            String kind = element->getKind().c_str();
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
                const ArrayList<WidgetDialogElement>& children =
                    element->getChildren();
                for ( i = children.size(); i > 0; i-- ) {
                    pending.add(&children[i - 1]);
                }
            }
            else {
                throw IllegalArgumentException(
                    "Unsupported form element: " + kind);
            }
        }

        if ( withTree ) {
            form->width = TREE_DIALOG_WIDTH;
        }
        ArrayList<ArrayList<String> > prompts;
        int height = MARGIN;
        for ( i = 0; i < inputs.size(); i++ ) {
            String labelKey = inputs[i]->getLabelKey().c_str();
            prompts.add(labelKey.isEmpty() ? ArrayList<String>() :
                wrapText(fontSet, messages->message(labelKey), form->width - 2 * MARGIN));
            height += ((int)(prompts[i].size())) * LINE_HEIGHT +
                (prompts[i].isEmpty() ? 0 : 6) +
                (inputs[i]->getKind().equals("tree") ? TREE_HEIGHT : FIELD_HEIGHT) +
                MARGIN;
        }
        form->buttonsY = height;
        if ( !buttons.isEmpty() ) {
            height += BUTTON_HEIGHT + MARGIN;
        }

        XtWidget frame = widgets->createPanel(shell, "dialogFrame", 0, 0,
                                              form->width, height, TRUE);
        int y = MARGIN;
        for ( i = 0; i < inputs.size(); i++ ) {
            for ( long line = 0; line < prompts[i].size(); line++ ) {
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
                form->trees.put(inputs[i]->getBinding(), tree);
                if ( form->firstField == NULL ) {
                    form->firstField = tree->getWidget();
                }
                y += TREE_HEIGHT + MARGIN;
                continue;
            }
            XtWidget field = widgets->createTextField(frame, "", fontSet,
                MARGIN, y, form->width - 2 * MARGIN, FIELD_HEIGHT,
                ignoreActivation, NULL);
            form->fields.put(inputs[i]->getBinding(), field);
            if ( form->firstField == NULL ) {
                form->firstField = field;
            }
            y += FIELD_HEIGHT + MARGIN;
        }
        for ( i = 0; i < buttons.size(); i++ ) {
            // Fully qualified (as Form::buttonPressed below): gcc 2.7.2 does
            // not find the members of Form here
            XtDialogHost::Form::Button* button =
                new XtDialogHost::Form::Button;
            button->form = form;
            button->action = buttons[i]->getAction().c_str();
            button->label = messages->message(buttons[i]->getLabelKey().c_str());
            button->widget = widgets->createPushButton(frame, button->label,
                fontSet, 0, form->buttonsY, 1, BUTTON_HEIGHT,
                &XtDialogHost::Form::buttonPressed, button);
            form->buttons.add(button);
        }
        form->layoutButtons();
    }
    catch ( ... ) {
        // i.e. IllegalArgumentException for unknown elements
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
    ArrayList<int> widths;
    long i;
    for ( i = 0; i < buttons.size(); i++ ) {
        widths.add(textWidth(fontSet, buttons[i]->label) +
                         BUTTON_PADDING);
        rowWidth += widths[i] + (i > 0 ? BUTTON_GAP : 0);
    }
    int x = Math::max(MARGIN, (width - rowWidth) / 2);
    for ( i = 0; i < buttons.size(); i++ ) {
        XtVaSetValues(buttons[i]->widget, XtNx, x, XtNy, buttonsY,
                      XtNwidth, widths[i], XtNheight, BUTTON_HEIGHT, NULL);
        x += widths[i] + BUTTON_GAP;
    }
}

XtDialogHost::Form::Button* XtDialogHost::Form::findButton(
    const String& action) const
{
    for ( long i = 0; i < buttons.size(); i++ ) {
        if ( buttons[i]->action == action ) {
            return buttons[i];
        }
    }
    return NULL;
}

XtWidget XtDialogHost::Form::dialog() const
{
    return shell;
}

XtWidget XtDialogHost::Form::field(const String& binding) const
{
    const XtWidget* found = fields.get(binding);
    return found != NULL ? (*found) : NULL;
}

XtWidget XtDialogHost::Form::action(const String& action) const
{
    Button* button = findButton(action);
    return button != NULL ? button->widget : NULL;
}

XtTreeView* XtDialogHost::Form::tree(const String& binding) const
{
    XtTreeView* const* found = trees.get(binding);
    return found != NULL ? (*found) : NULL;
}

BOOLEAN XtDialogHost::Form::hasAction(const String& action) const
{
    return definition->getElementByAction(action.c_str()) != NULL;
}

DialogSession* XtDialogHost::Form::session()
{
    return &dialogSession;
}

String XtDialogHost::Form::getText(const String& binding) const
{
    XtWidget widget = field(binding);
    return widget != NULL ?
        widgetSet->getPanelWidgets()->getText(widget) : String();
}

void XtDialogHost::Form::setText(const String& binding,
                                 const String& text)
{
    XtWidget widget = field(binding);
    if ( widget != NULL ) {
        widgetSet->getPanelWidgets()->setText(widget, text);
    }
}

String XtDialogHost::Form::getActionLabel(const String& action) const
{
    Button* button = findButton(action);
    return button != NULL ? button->label : String();
}

void XtDialogHost::Form::setActionLabel(const String& action,
                                        const String& label)
{
    Button* button = findButton(action);
    if ( button == NULL ) {
        return;
    }
    button->label = label;
    widgetSet->getPanelWidgets()->setLabel(button->widget, label);
    layoutButtons();
}

void XtDialogHost::Form::show()
{
    XtPopup(shell, modal ? XtGrabExclusive : XtGrabNone);
    visible = TRUE;
    XRaiseWindow(XtDisplay(shell), XtWindow(shell));
    if ( firstField != NULL ) {
        widgetSet->setKeyboardFocus(shell, firstField);
    }
}

void XtDialogHost::Form::close()
{
    dialogSession.close();
    if ( visible ) {
        visible = FALSE;
        XtPopdown(shell);
    }
}

BOOLEAN XtDialogHost::Form::isVisible() const
{
    return visible;
}

void XtDialogHost::Form::buttonPressed(XtWidget, void* clientData)
{
    Button* button = ((Button*)(clientData));
    // The listener may close the form: nothing of it is used afterwards
    ActionListener* listener = button->form->onAction;
    String action = button->action;
    if ( listener != NULL ) {
        listener->actionPerformed(action);
    }
}

void XtDialogHost::Form::closeRequested(void* clientData)
{
    ((Form*)(clientData))->close();
}
