// Before any other header that could include the Xt ones
#include "vsdk/toolkit/gui/XmIntrinsics.h"

#include <string>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/CommandListener.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuElement.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "vsdk/toolkit/render/xm/XmGuiRenderer.h"

namespace {

const int BUTTON_HEIGHT = 26;
const int BUTTON_SPACING = 3;
const int ICON_BUTTON_SIZE = 30;
const int TEXT_BUTTON_WIDTH = 120;
const int MARGIN = 8;

void bindCommand(XtWidget widget, CommandListener* executor,
                 const java::String& command)
{
    XtWidgetSupport::bindCommand(widget, XmNactivateCallback, executor,
                                 command);
}

/**
Adds a cascade button to a menubar or a menu pane, showing a submenu.
*/
XtWidget addCascade(XtWidget menu, const java::String& label,
                    XtWidget submenu, XFontSet fontSet)
{
    XmString text = XmWidgetSupport::createString(label.c_str());
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(menu, fontSet);
    Arg args[3]; Cardinal n = 0;
    XtSetArg(args[n], XmNlabelString, text); ++n;
    XtSetArg(args[n], XmNsubMenuId, submenu); ++n;
    if ( fonts != nullptr ) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    XtWidget cascade = XtCreateManagedWidget("menuCascade",
        xmCascadeButtonWidgetClass, menu, args, n);
    XmStringFree(text);
    if ( fonts != nullptr ) {
        XmRenderTableFree(fonts);
    }
    return cascade;
}

/**
Adds the items, separators and submenus of a menu of the GUI to a menu
pane.
*/
void fillMenu(XtWidget pane, WidgetMenu* menu, CommandListener* executor,
              XFontSet fontSet)
{
    java::ArrayList<WidgetMenuElement*>& children = menu->getChildren();
    for ( long i = 0; i < children.size(); i++ ) {
        WidgetMenu* submenu = dynamic_cast<WidgetMenu*>(children.get(i));
        if ( submenu != nullptr ) {
            XtWidget submenuPane =
                XmWidgetSupport::createPulldownMenu(pane, "submenu");
            fillMenu(submenuPane, submenu, executor, fontSet);
            addCascade(pane, submenu->getName(), submenuPane, fontSet);
            continue;
        }
        WidgetMenuItem* option = dynamic_cast<WidgetMenuItem*>(children.get(i));
        if ( option == nullptr ) {
            continue;
        }
        if ( option->isSeparator() ) {
            XtCreateManagedWidget("separator", xmSeparatorWidgetClass, pane,
                                  nullptr, 0);
            continue;
        }
        XtWidget item = XmWidgetSupport::createMenuItem(
            pane, option->getName().c_str(), fontSet);
        bindCommand(item, executor, option->getCommandName());
    }
}

}

XtWidget XmGuiRenderer::buildPopupMenu(XtWidget owner, WidgetMenu* menu,
                                       CommandListener* executor,
                                       XFontSet fontSet)
{
    XtWidget popup = XmWidgetSupport::createPopupMenu(owner, "vitralMenu");
    if ( menu == nullptr ) {
        XmWidgetSupport::createMenuItem(popup, "Popup menu not found on GUI",
                                        fontSet);
        return popup;
    }
    fillMenu(popup, menu, executor, fontSet);
    return popup;
}

XtWidget XmGuiRenderer::buildMenubar(XtWidget parent, WidgetMenu* menubar,
                                     CommandListener* executor,
                                     XFontSet fontSet, int x, int y,
                                     int width, int height, int)
{
    Arg args[8]; Cardinal n = 0;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    XtSetArg(args[n], XmNresizeWidth, False); ++n;
    XtSetArg(args[n], XmNresizeHeight, False); ++n;
    XtSetArg(args[n], XmNmarginHeight, 0); ++n;
    XtSetArg(args[n], XmNshadowThickness, 1); ++n;
    XtWidget bar = XmCreateMenuBar(parent, const_cast<char*>("menuBar"),
                                   args, n);

    if ( menubar == nullptr ) {
        // As Swing: a menu reporting the problem, which can end the program
        XtWidget pane = XmWidgetSupport::createPulldownMenu(bar, "menu");
        XtWidget exitItem = XmWidgetSupport::createMenuItem(pane, "Exit",
                                                            fontSet);
        bindCommand(exitItem, executor, "IDC_FILE_QUIT");
        addCascade(bar, "No menubar in GUI!", pane, fontSet);
    }
    else {
        java::ArrayList<WidgetMenuElement*>& children = menubar->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            WidgetMenu* menu = dynamic_cast<WidgetMenu*>(children.get(i));
            if ( menu == nullptr ) {
                continue;
            }
            XtWidget pane = XmWidgetSupport::createPulldownMenu(bar, "menu");
            fillMenu(pane, menu, executor, fontSet);
            addCascade(bar, menu->getName(), pane, fontSet);
        }
    }
    XtManageChild(bar);
    return bar;
}

XtWidget XmGuiRenderer::buildButtonGroup(XtWidget parent,
                                         WidgetButtonGroup* group,
                                         CommandListener* executor,
                                         XFontSet fontSet, int x, int y,
                                         int width)
{
    bool horizontal = group != nullptr &&
        group->getDirection() == WidgetButtonGroup::HORIZONTAL;
    long count = group != nullptr ? group->getCommands().size() : 0;
    int height = horizontal ? ICON_BUTTON_SIZE + 2 * BUTTON_SPACING :
        2 * MARGIN + static_cast<int>(count) * (BUTTON_HEIGHT + BUTTON_SPACING);
    if ( height < BUTTON_HEIGHT ) {
        height = BUTTON_HEIGHT;
    }

    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    XtSetArg(args[n], XmNmarginWidth, 0); ++n;
    XtSetArg(args[n], XmNmarginHeight, 0); ++n;
    XtSetArg(args[n], XmNshadowThickness, 0); ++n;
    XtSetArg(args[n], XmNresizePolicy, XmRESIZE_NONE); ++n;
    XtWidget frame = XtCreateManagedWidget("buttonGroup",
        xmBulletinBoardWidgetClass, parent, args, n);

    XmRenderTable fonts = XmWidgetSupport::createRenderTable(parent, fontSet);
    if ( group == nullptr ) {
        XmString text = XmWidgetSupport::createString(
            "No ButtonGroup found in GUI");
        Arg labelArgs[3]; Cardinal labelN = 0;
        XtSetArg(labelArgs[labelN], XmNlabelString, text); ++labelN;
        XtSetArg(labelArgs[labelN], XmNwidth, width); ++labelN;
        if ( fonts != nullptr ) {
            XtSetArg(labelArgs[labelN], XmNrenderTable, fonts); ++labelN;
        }
        XtCreateManagedWidget("noButtonGroup", xmLabelWidgetClass, frame,
                              labelArgs, labelN);
        XmStringFree(text);
        if ( fonts != nullptr ) {
            XmRenderTableFree(fonts);
        }
        return frame;
    }

    java::ArrayList<WidgetCommand*>& commands = group->getCommands();
    for ( long i = 0; i < count; i++ ) {
        WidgetCommand* element = commands.get(i);
        RGBAImageUncompressed* icon = element->getIcon();
        bool withIcon = icon != nullptr && group->isShowIconsSet();
        // Icon buttons show their text only if the group asks for it
        java::String label = !withIcon || group->isShowTextSet() ?
            element->getName() : java::String("");

        XmString text = XmWidgetSupport::createString(label.c_str());
        Arg buttonArgs[12]; Cardinal buttonN = 0;
        XtSetArg(buttonArgs[buttonN], XmNlabelString, text); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XmNrecomputeSize, False); ++buttonN;
        // Clicking a button leaves the keyboard focus in the drawing area
        XtSetArg(buttonArgs[buttonN], XmNtraversalOn, False); ++buttonN;
        if ( fonts != nullptr ) {
            XtSetArg(buttonArgs[buttonN], XmNrenderTable, fonts); ++buttonN;
        }
        if ( horizontal ) {
            XtSetArg(buttonArgs[buttonN], XmNx, BUTTON_SPACING +
                     static_cast<int>(i) * (ICON_BUTTON_SIZE + BUTTON_SPACING)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNy, BUTTON_SPACING); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNwidth,
                     withIcon ? ICON_BUTTON_SIZE : TEXT_BUTTON_WIDTH); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNheight, ICON_BUTTON_SIZE); ++buttonN;
        }
        else {
            XtSetArg(buttonArgs[buttonN], XmNx, MARGIN); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNy, MARGIN + static_cast<int>(i) *
                     (BUTTON_HEIGHT + BUTTON_SPACING)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNwidth, width - 2 * MARGIN); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNheight, BUTTON_HEIGHT); ++buttonN;
        }
        if ( withIcon ) {
            // The whole button for the icon
            XtSetArg(buttonArgs[buttonN], XmNmarginWidth, 0); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNmarginHeight, 0); ++buttonN;
        }
        XtWidget button = XtCreateManagedWidget("commandButton",
            xmPushButtonWidgetClass, frame, buttonArgs, buttonN);
        XmStringFree(text);

        if ( withIcon ) {
            Pixmap pixmap = XtWidgetSupport::createIconPixmap(button, *icon);
            if ( pixmap != None ) {
                XtVaSetValues(button, XmNlabelType, XmPIXMAP,
                              XmNlabelPixmap, pixmap, nullptr);
            }
        }
        bindCommand(button, executor, element->getId());
    }
    if ( fonts != nullptr ) {
        XmRenderTableFree(fonts);
    }
    return frame;
}
