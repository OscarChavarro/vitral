// Before any other header that could include the Xt ones
#include "vsdk/toolkit/gui/XtIntrinsics.h"

#include <algorithm>
#include <stdexcept>

#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "vsdk/toolkit/render/xt/XtDialogHost.h"
#include "vsdk/toolkit/render/xt/XtTreeView.h"

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

static int textWidth(XFontSet fontSet, const std::string& text)
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
static std::vector<std::string> wrapText(XFontSet fontSet, const std::string& text,
                                  int width)
{
    std::vector<std::string> lines;
    std::string line;
    size_t start = 0;
    while ( start < text.size() ) {
        size_t end = text.find(' ', start);
        if ( end == std::string::npos ) end = text.size();
        std::string word = text.substr(start, end - start);
        std::string candidate = line.empty() ? word : line + " " + word;
        if ( !line.empty() && textWidth(fontSet, candidate) > width ) {
            lines.push_back(line);
            line = word;
        }
        else {
            line = candidate;
        }
        start = end + 1;
    }
    if ( !line.empty() ) lines.push_back(line);
    return lines;
}

/// Swing `JTextField`s without an action listener ignore Return
static void ignoreActivation(XtWidget, void*)
{
}

struct XtDialogHost::Form::Button {
    Form* form;
    std::string action;
    std::string label;
    XtWidget widget;
};

XtDialogHost::Form::Form(XtWidgetSet* widgetSet, XFontSet fontSet,
                         XtWidget shell, const WidgetDialog* definition,
                         ActionListener* onAction)
    : widgetSet(widgetSet), definition(definition), fontSet(fontSet),
      shell(shell),
      firstField(NULL), modal(definition->isModal()), visible(false),
      width(DIALOG_WIDTH), buttonsY(0), dialogSession(definition),
      onAction(onAction)
{
}

XtDialogHost::Form::~Form()
{
    for ( std::map<std::string, XtTreeView*>::iterator tree = trees.begin();
          tree != trees.end(); ++tree ) {
        delete tree->second;
    }
    for ( size_t i = 0; i < buttons.size(); i++ ) {
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
        std::string title = messages->message(definition->getTitleKey().c_str());
        XtVaSetValues(shell, XtNtitle, title.c_str(), NULL);
        widgetSet->setWindowCloseHandler(shell, &Form::closeRequested, form);

        // The elements, in order: rows and columns only group them (as the
        // Swing host does), and the buttons go to the bottom row
        std::vector<const WidgetDialogElement*> pending;
        for ( size_t i = definition->getElements().size(); i > 0; i-- ) {
            pending.push_back(&definition->getElements()[i - 1]);
        }
        std::vector<const WidgetDialogElement*> inputs;
        std::vector<const WidgetDialogElement*> buttons;
        bool withTree = false;
        while ( !pending.empty() ) {
            const WidgetDialogElement* element = pending.back();
            pending.pop_back();
            std::string kind = element->getKind().c_str();
            if ( kind == "textInput" || kind == "tree" ) {
                inputs.push_back(element);
                withTree = withTree || kind == "tree";
            }
            else if ( kind == "action" ) {
                // Offered by the application with keys or menus
            }
            else if ( kind == "button" ) {
                buttons.push_back(element);
            }
            else if ( kind == "row" || kind == "column" ) {
                const std::vector<WidgetDialogElement>& children =
                    element->getChildren();
                for ( size_t i = children.size(); i > 0; i-- ) {
                    pending.push_back(&children[i - 1]);
                }
            }
            else {
                throw std::invalid_argument(
                    "Unsupported form element: " + kind);
            }
        }

        if ( withTree ) {
            form->width = TREE_DIALOG_WIDTH;
        }
        std::vector<std::vector<std::string> > prompts;
        int height = MARGIN;
        for ( size_t i = 0; i < inputs.size(); i++ ) {
            std::string labelKey = inputs[i]->getLabelKey().c_str();
            prompts.push_back(labelKey.empty() ? std::vector<std::string>() :
                wrapText(fontSet, messages->message(labelKey), form->width - 2 * MARGIN));
            height += ((int)(prompts[i].size())) * LINE_HEIGHT +
                (prompts[i].empty() ? 0 : 6) +
                (inputs[i]->getKind().equals("tree") ? TREE_HEIGHT : FIELD_HEIGHT) +
                MARGIN;
        }
        form->buttonsY = height;
        if ( !buttons.empty() ) {
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
            if ( !prompts[i].empty() ) {
                y += 6;
            }
            if ( inputs[i]->getKind().equals("tree") ) {
                XtTreeView* tree = new XtTreeView(frame, fontSet, MARGIN, y,
                    form->width - 2 * MARGIN, TREE_HEIGHT);
                form->trees[inputs[i]->getBinding().c_str()] = tree;
                if ( form->firstField == NULL ) {
                    form->firstField = tree->getWidget();
                }
                y += TREE_HEIGHT + MARGIN;
                continue;
            }
            XtWidget field = widgets->createTextField(frame, "", fontSet,
                MARGIN, y, form->width - 2 * MARGIN, FIELD_HEIGHT,
                ignoreActivation, NULL);
            form->fields[inputs[i]->getBinding().c_str()] = field;
            if ( form->firstField == NULL ) {
                form->firstField = field;
            }
            y += FIELD_HEIGHT + MARGIN;
        }
        for ( size_t i = 0; i < buttons.size(); i++ ) {
            Form::Button* button = new Form::Button;
            button->form = form;
            button->action = buttons[i]->getAction().c_str();
            button->label = messages->message(buttons[i]->getLabelKey().c_str());
            button->widget = widgets->createPushButton(frame, button->label,
                fontSet, 0, form->buttonsY, 1, BUTTON_HEIGHT,
                &Form::buttonPressed, button);
            form->buttons.push_back(button);
        }
        form->layoutButtons();
    }
    catch ( ... ) {
        // i.e. std::invalid_argument for unknown elements
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
    std::vector<int> widths;
    for ( size_t i = 0; i < buttons.size(); i++ ) {
        widths.push_back(textWidth(fontSet, buttons[i]->label) +
                         BUTTON_PADDING);
        rowWidth += widths[i] + (i > 0 ? BUTTON_GAP : 0);
    }
    int x = std::max(MARGIN, (width - rowWidth) / 2);
    for ( size_t i = 0; i < buttons.size(); i++ ) {
        XtVaSetValues(buttons[i]->widget, XtNx, x, XtNy, buttonsY,
                      XtNwidth, widths[i], XtNheight, BUTTON_HEIGHT, NULL);
        x += widths[i] + BUTTON_GAP;
    }
}

XtDialogHost::Form::Button* XtDialogHost::Form::findButton(
    const std::string& action) const
{
    for ( size_t i = 0; i < buttons.size(); i++ ) {
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

XtWidget XtDialogHost::Form::field(const std::string& binding) const
{
    std::map<std::string, XtWidget>::const_iterator found =
        fields.find(binding);
    return found != fields.end() ? found->second : NULL;
}

XtWidget XtDialogHost::Form::action(const std::string& action) const
{
    Button* button = findButton(action);
    return button != NULL ? button->widget : NULL;
}

XtTreeView* XtDialogHost::Form::tree(const std::string& binding) const
{
    std::map<std::string, XtTreeView*>::const_iterator found =
        trees.find(binding);
    return found != trees.end() ? found->second : NULL;
}

bool XtDialogHost::Form::hasAction(const std::string& action) const
{
    return definition->getElementByAction(action.c_str()) != NULL;
}

DialogSession* XtDialogHost::Form::session()
{
    return &dialogSession;
}

std::string XtDialogHost::Form::getText(const std::string& binding) const
{
    XtWidget widget = field(binding);
    return widget != NULL ?
        widgetSet->getPanelWidgets()->getText(widget) : std::string();
}

void XtDialogHost::Form::setText(const std::string& binding,
                                 const std::string& text)
{
    XtWidget widget = field(binding);
    if ( widget != NULL ) {
        widgetSet->getPanelWidgets()->setText(widget, text);
    }
}

std::string XtDialogHost::Form::getActionLabel(const std::string& action) const
{
    Button* button = findButton(action);
    return button != NULL ? button->label : std::string();
}

void XtDialogHost::Form::setActionLabel(const std::string& action,
                                        const std::string& label)
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
    visible = true;
    XRaiseWindow(XtDisplay(shell), XtWindow(shell));
    if ( firstField != NULL ) {
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
    Button* button = ((Button*)(clientData));
    // The listener may close the form: nothing of it is used afterwards
    ActionListener* listener = button->form->onAction;
    std::string action = button->action;
    if ( listener != NULL ) {
        listener->actionPerformed(action);
    }
}

void XtDialogHost::Form::closeRequested(void* clientData)
{
    ((Form*)(clientData))->close();
}
