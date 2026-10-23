// Before any other header that could include the Xt ones
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/XmIntrinsics.h"

#include "java/lang/String.h"

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


static const int BUTTON_HEIGHT = 26;
static const int BUTTON_SPACING = 3;
static const int ICON_BUTTON_SIZE = 30;
static const int TEXT_BUTTON_WIDTH = 120;
static const int MARGIN = 8;

static void bindCommand(XtWidget widget, CommandListener* executor,
                 const String& command)
{
    XtWidgetSupport::bindCommand(widget, XmNactivateCallback, executor,
                                 command);
}

/**
Adds a cascade button to a menubar or a menu pane, showing a submenu.
*/
static XtWidget addCascade(XtWidget menu, const String& label,
                    XtWidget submenu, XFontSet fontSet)
{
    XmString text = XmWidgetSupport::createString(label.c_str());
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(menu, fontSet);
    Arg args[3]; Cardinal n = 0;
    XtSetArg(args[n], XmNlabelString, text); ++n;
    XtSetArg(args[n], XmNsubMenuId, submenu); ++n;
    if ( fonts != NULL ) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    XtWidget cascade = XtCreateManagedWidget("menuCascade",
        xmCascadeButtonWidgetClass, menu, args, n);
    XmStringFree(text);
    if ( fonts != NULL ) {
        XmRenderTableFree(fonts);
    }
    return cascade;
}

/**
Adds the items, separators and submenus of a menu of the GUI to a menu
pane.
*/
static void fillMenu(XtWidget pane, WidgetMenu* menu, CommandListener* executor,
              XFontSet fontSet)
{
    ArrayList<WidgetMenuElement*>& children = menu->getChildren();
    for ( long i = 0; i < children.size(); i++ ) {
        WidgetMenu* submenu = (children.get(i) != NULL && children.get(i)->instanceOf(WidgetMenu::CLASS) ? (WidgetMenu*)children.get(i) : NULL);
        if ( submenu != NULL ) {
            XtWidget submenuPane =
                XmWidgetSupport::createPulldownMenu(pane, "submenu");
            fillMenu(submenuPane, submenu, executor, fontSet);
            addCascade(pane, submenu->getName(), submenuPane, fontSet);
            continue;
        }
        WidgetMenuItem* option = (children.get(i) != NULL && children.get(i)->instanceOf(WidgetMenuItem::CLASS) ? (WidgetMenuItem*)children.get(i) : NULL);
        if ( option == NULL ) {
            continue;
        }
        if ( option->isSeparator() ) {
            XtCreateManagedWidget("separator", xmSeparatorWidgetClass, pane,
                                  NULL, 0);
            continue;
        }
        XtWidget item = XmWidgetSupport::createMenuItem(
            pane, option->getName().c_str(), fontSet);
        bindCommand(item, executor, option->getCommandName());
    }
}


XtWidget XmGuiRenderer::buildPopupMenu(XtWidget owner, WidgetMenu* menu,
                                       CommandListener* executor,
                                       XFontSet fontSet)
{
    XtWidget popup = XmWidgetSupport::createPopupMenu(owner, "vitralMenu");
    if ( menu == NULL ) {
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
    XtWidget bar = XmCreateMenuBar(parent, ((char*)("menuBar")),
                                   args, n);

    if ( menubar == NULL ) {
        // As Swing: a menu reporting the problem, which can end the program
        XtWidget pane = XmWidgetSupport::createPulldownMenu(bar, "menu");
        XtWidget exitItem = XmWidgetSupport::createMenuItem(pane, "Exit",
                                                            fontSet);
        bindCommand(exitItem, executor, "IDC_FILE_QUIT");
        addCascade(bar, "No menubar in GUI!", pane, fontSet);
    }
    else {
        ArrayList<WidgetMenuElement*>& children = menubar->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            WidgetMenu* menu = (children.get(i) != NULL && children.get(i)->instanceOf(WidgetMenu::CLASS) ? (WidgetMenu*)children.get(i) : NULL);
            if ( menu == NULL ) {
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
    BOOLEAN horizontal = group != NULL &&
        group->getDirection() == WidgetButtonGroup::HORIZONTAL;
    long count = group != NULL ? group->getCommands().size() : 0;
    int height = horizontal ? ICON_BUTTON_SIZE + 2 * BUTTON_SPACING :
        2 * MARGIN + ((int)(count)) * (BUTTON_HEIGHT + BUTTON_SPACING);
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
    if ( group == NULL ) {
        XmString text = XmWidgetSupport::createString(
            "No ButtonGroup found in GUI");
        Arg labelArgs[3]; Cardinal labelN = 0;
        XtSetArg(labelArgs[labelN], XmNlabelString, text); ++labelN;
        XtSetArg(labelArgs[labelN], XmNwidth, width); ++labelN;
        if ( fonts != NULL ) {
            XtSetArg(labelArgs[labelN], XmNrenderTable, fonts); ++labelN;
        }
        XtCreateManagedWidget("noButtonGroup", xmLabelWidgetClass, frame,
                              labelArgs, labelN);
        XmStringFree(text);
        if ( fonts != NULL ) {
            XmRenderTableFree(fonts);
        }
        return frame;
    }

    ArrayList<WidgetCommand*>& commands = group->getCommands();
    for ( long i = 0; i < count; i++ ) {
        WidgetCommand* element = commands.get(i);
        RGBAImageUncompressed* icon = element->getIcon();
        BOOLEAN withIcon = icon != NULL && group->isShowIconsSet();
        // Icon buttons show their text only if the group asks for it
        String label = !withIcon || group->isShowTextSet() ?
            element->getName() : String("");

        XmString text = XmWidgetSupport::createString(label.c_str());
        Arg buttonArgs[12]; Cardinal buttonN = 0;
        XtSetArg(buttonArgs[buttonN], XmNlabelString, text); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XmNrecomputeSize, False); ++buttonN;
        // Clicking a button leaves the keyboard focus in the drawing area
        XtSetArg(buttonArgs[buttonN], XmNtraversalOn, False); ++buttonN;
        if ( fonts != NULL ) {
            XtSetArg(buttonArgs[buttonN], XmNrenderTable, fonts); ++buttonN;
        }
        if ( horizontal ) {
            XtSetArg(buttonArgs[buttonN], XmNx, BUTTON_SPACING +
                     ((int)(i)) * (ICON_BUTTON_SIZE + BUTTON_SPACING)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNy, BUTTON_SPACING); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNwidth,
                     withIcon ? ICON_BUTTON_SIZE : TEXT_BUTTON_WIDTH); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNheight, ICON_BUTTON_SIZE); ++buttonN;
        }
        else {
            XtSetArg(buttonArgs[buttonN], XmNx, MARGIN); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XmNy, MARGIN + ((int)(i)) *
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
                              XmNlabelPixmap, pixmap, NULL);
            }
        }
        bindCommand(button, executor, element->getId());
    }
    if ( fonts != NULL ) {
        XmRenderTableFree(fonts);
    }
    return frame;
}
