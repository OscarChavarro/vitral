#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferButton.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferDialogForm.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferLabel.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferList.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferTextField.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"

FramebufferDialogForm::FramebufferDialogForm(const Widget &context, const WidgetDialog &definition)
    : dialog(context.getMessage(definition.getTitleKey())), complete(true) {
    // The first button is the default one; a cancel or close action answers Escape (see build()).
    for (const WidgetDialogElement &element : definition.getElements()) {
        build(context, element);
    }
}

void
FramebufferDialogForm::build(const Widget &context, const WidgetDialogElement &element) {
    const java::String &kind = element.getKind();
    if (kind.equals("row") || kind.equals("column")) {
        for (const WidgetDialogElement &child : element.getChildren()) {
            build(context, child);
        }
    } else if (kind.equals("textInput")) {
        if (!element.getLabelKey().isEmpty()) {
            dialog.add(new FramebufferLabel(context.getMessage(element.getLabelKey())));
        }
        FramebufferTextField *field = new FramebufferTextField("", 40);
        dialog.add(field);
        fields[element.getBinding()] = field;
    } else if (kind.equals("tree")) {
        FramebufferList *list = new FramebufferList(36, 14, true);
        dialog.add(list);
        trees[element.getBinding()] = list;
    } else if (kind.equals("button") || kind.equals("action")) {
        bool isCancel = element.getAction().equals("cancel") || element.getAction().equals("close");
        FramebufferButton *button = new FramebufferButton(context.getMessage(element.getLabelKey()),
                                                          element.getAction());
        dialog.addButton(button, buttons.empty() && !isCancel, isCancel);
        buttons[element.getAction()] = button;
    } else {
        complete = false;
    }
}

FramebufferTextField *
FramebufferDialogForm::field(const java::String &binding) const {
    std::map<java::String, FramebufferTextField *>::const_iterator i = fields.find(binding);
    return i == fields.end() ? nullptr : i->second;
}

FramebufferList *
FramebufferDialogForm::tree(const java::String &binding) const {
    std::map<java::String, FramebufferList *>::const_iterator i = trees.find(binding);
    return i == trees.end() ? nullptr : i->second;
}

FramebufferButton *
FramebufferDialogForm::button(const java::String &action) const {
    std::map<java::String, FramebufferButton *>::const_iterator i = buttons.find(action);
    return i == buttons.end() ? nullptr : i->second;
}

java::String
FramebufferDialogForm::actionOf(const FramebufferControl *control) const {
    for (std::map<java::String, FramebufferButton *>::const_iterator i = buttons.begin(); i != buttons.end(); ++i) {
        if (i->second == control) {
            return i->first;
        }
    }
    return java::String("");
}

void
FramebufferDialogForm::flatten(const java::String &binding, const DialogTreeNode &node, int depth) {
    FramebufferList *list = tree(binding);
    for (const DialogTreeNode &child : node.getChildren()) {
        rows[binding].add(&child);
        list->add(child.getLabel(), depth, (long)rows[binding].size() - 1);
        flatten(binding, child, depth + 1);
    }
}

void
FramebufferDialogForm::setTree(const java::String &binding, const DialogTreeNode &root) {
    FramebufferList *list = tree(binding);
    if (!list) {
        return;
    }
    list->clear();
    rows[binding].clear();
    flatten(binding, root, 0);
}

const DialogTreeNode *
FramebufferDialogForm::node(const java::String &binding, int row) const {
    std::map<java::String, java::ArrayList<const DialogTreeNode *> >::const_iterator i = rows.find(binding);
    if (i == rows.end() || row < 0 || row >= (int)i->second.size()) {
        return nullptr;
    }
    return i->second[row];
}

int
FramebufferDialogForm::rowOf(const java::String &binding, const DialogTreeNode *target) const {
    std::map<java::String, java::ArrayList<const DialogTreeNode *> >::const_iterator i = rows.find(binding);
    if (i == rows.end()) {
        return -1;
    }
    for (int row = 0; row < (int)i->second.size(); row++) {
        if (i->second[row] == target) {
            return row;
        }
    }
    return -1;
}

java::String
FramebufferDialogForm::run(Framebuffer &surface, FramebufferInput &input, const FramebufferTheme &theme,
                           FramebufferDialogListener *listener) {
    FramebufferButton *closer = dialog.run(surface, input, theme, listener);
    return closer ? actionOf(closer) : java::String("");
}
