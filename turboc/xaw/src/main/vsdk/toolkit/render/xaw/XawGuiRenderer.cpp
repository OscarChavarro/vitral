// Before any other header that could include the Xt ones
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/XawIntrinsics.h"

#include "java/lang/String.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/XtTextEncoding.h"
#include "vsdk/toolkit/gui/CommandListener.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuElement.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "vsdk/toolkit/render/xaw/XawGuiRenderer.h"


static const int BUTTON_HEIGHT = 26;
static const int BUTTON_SPACING = 3;
static const int ICON_BUTTON_SIZE = 30;
static const int TEXT_BUTTON_WIDTH = 120;
static const int MARGIN = 8;

static void bindCommand(XtWidget widget, CommandListener* executor,
                 const String& command)
{
    XtWidgetSupport::bindCommand(widget, XtNcallback, executor, command);
}

/**
Shows a submenu to the right of its entry.
*/
static void popupSubmenu(XtWidget entry, XtPointer clientData, XtPointer)
{
    XtWidget popup = ((XtWidget)(clientData));
    Position entryX = 0;
    Position entryY = 0;
    Dimension entryWidth = 0;
    Position x = 0;
    Position y = 0;
    XtVaGetValues(entry, XtNx, &entryX, XtNy, &entryY, XtNwidth, &entryWidth,
                  NULL);
    // SmeBSB is a windowless RectObj: its coordinates are the ones of its
    // SimpleMenu, which owns the X window
    XtTranslateCoords(XtParent(entry),
                      ((Position)(entryX + entryWidth)), entryY,
                      &x, &y);
    XtVaSetValues(popup, XtNx, x, XtNy, y, NULL);
    XtPopup(popup, XtGrabNonexclusive);
}

static XtWidget createPopupShell(XtWidget owner)
{
    // Unique names: menu buttons find their popup by name
    String name = XtWidgetSupport::uniqueName("vitralMenu");
    XtWidgetSupport::ShellVisual shellVisual =
        XtWidgetSupport::shellVisualOf(owner);
    Arg args[4]; Cardinal n = 0;
    // Xaw follows the menuName of an entry while the pointer enters it only
    // with popupOnEntry
    XtSetArg(args[n], XtNpopupOnEntry, True); ++n;
    if ( shellVisual.depth != 0 ) {
        XtSetArg(args[n], XtNvisual, shellVisual.visual); ++n;
        XtSetArg(args[n], XtNdepth, shellVisual.depth); ++n;
        XtSetArg(args[n], XtNcolormap, shellVisual.colormap); ++n;
    }
    return XtCreatePopupShell(name.c_str(), simpleMenuWidgetClass, owner,
                              args, n);
}

static XtWidget addMenuEntry(XtWidget menu, const String& label,
                      XFontSet fontSet)
{
    const String shownLabel = XtTextEncoding::toLocale(label);
    Arg args[3]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, shownLabel.c_str()); ++n;
    XtSetArg(args[n], XtNinternational, True); ++n;
    XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    return XtCreateManagedWidget("menuItem", smeBSBObjectClass, menu, args, n);
}


XtWidget XawGuiRenderer::buildPopupMenu(XtWidget owner, WidgetMenu* menu,
                                        CommandListener* executor,
                                        XFontSet fontSet)
{
    XtWidget popup = createPopupShell(owner);
    if ( menu == NULL ) {
        addMenuEntry(popup, "Popup menu not found on GUI", fontSet);
        return popup;
    }

    ArrayList<WidgetMenuElement*>& children = menu->getChildren();
    for ( long i = 0; i < children.size(); i++ ) {
        WidgetMenu* submenu = (children.get(i) != NULL && children.get(i)->instanceOf(WidgetMenu::CLASS) ? (WidgetMenu*)children.get(i) : NULL);
        if ( submenu != NULL ) {
            XtWidget entry = addMenuEntry(popup, submenu->getName(), fontSet);
            XtWidget submenuPopup =
                buildPopupMenu(owner, submenu, executor, fontSet);
            XtAddCallback(entry, XtNcallback, &popupSubmenu, submenuPopup);
            continue;
        }
        WidgetMenuItem* option = (children.get(i) != NULL && children.get(i)->instanceOf(WidgetMenuItem::CLASS) ? (WidgetMenuItem*)children.get(i) : NULL);
        if ( option == NULL ) {
            continue;
        }
        if ( option->isSeparator() ) {
            XtCreateManagedWidget("separator", smeLineObjectClass, popup,
                                  NULL, 0);
            continue;
        }
        XtWidget entry = addMenuEntry(popup, option->getName(), fontSet);
        bindCommand(entry, executor, option->getCommandName());
    }
    return popup;
}

XtWidget XawGuiRenderer::buildMenubar(XtWidget parent, WidgetMenu* menubar,
                                      CommandListener* executor,
                                      XFontSet fontSet, int x, int y,
                                      int width, int height, int buttonWidth)
{
    Arg args[4]; Cardinal n = 0;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    XtWidget bar = XtCreateManagedWidget("menuBar", compositeWidgetClass,
                                         parent, args, n);

    ArrayList<WidgetMenu*> menus;
    if ( menubar != NULL ) {
        ArrayList<WidgetMenuElement*>& children = menubar->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            WidgetMenu* menu = (children.get(i) != NULL && children.get(i)->instanceOf(WidgetMenu::CLASS) ? (WidgetMenu*)children.get(i) : NULL);
            if ( menu != NULL ) {
                menus.add(menu);
            }
        }
    }

    int count = menubar != NULL ? ((int)(menus.size())) : 1;
    for ( int i = 0; i < count; i++ ) {
        String title = menubar != NULL ?
            menus.get(i)->getName() : String("No menubar in GUI!");
        const String shownTitle = XtTextEncoding::toLocale(title);
        Arg buttonArgs[8]; Cardinal buttonN = 0;
        XtSetArg(buttonArgs[buttonN], XtNlabel, shownTitle.c_str()); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNinternational, True); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNfontSet, fontSet); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNx, i * buttonWidth); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNy, 0); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNwidth, buttonWidth); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNheight, height); ++buttonN;
        XtWidget button = XtCreateManagedWidget("menuButton",
            menuButtonWidgetClass, bar, buttonArgs, buttonN);

        XtWidget popup;
        if ( menubar != NULL ) {
            popup = buildPopupMenu(button, menus.get(i), executor, fontSet);
        }
        else {
            popup = createPopupShell(button);
            XtWidget exitEntry = addMenuEntry(popup, "Exit", fontSet);
            bindCommand(exitEntry, executor, "IDC_FILE_QUIT");
        }
        XtVaSetValues(button, XtNmenuName, XtName(popup), NULL);
    }
    return bar;
}

XtWidget XawGuiRenderer::buildButtonGroup(XtWidget parent,
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

    Arg args[4]; Cardinal n = 0;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    XtWidget frame = XtCreateManagedWidget("buttonGroup",
        compositeWidgetClass, parent, args, n);

    if ( group == NULL ) {
        Arg labelArgs[4]; Cardinal labelN = 0;
        XtSetArg(labelArgs[labelN], XtNlabel, "No ButtonGroup found in GUI"); ++labelN;
        XtSetArg(labelArgs[labelN], XtNinternational, True); ++labelN;
        XtSetArg(labelArgs[labelN], XtNfontSet, fontSet); ++labelN;
        XtSetArg(labelArgs[labelN], XtNwidth, width); ++labelN;
        XtCreateManagedWidget("noButtonGroup", labelWidgetClass, frame,
                              labelArgs, labelN);
        return frame;
    }

    ArrayList<WidgetCommand*>& commands = group->getCommands();
    for ( long i = 0; i < count; i++ ) {
        WidgetCommand* element = commands.get(i);
        RGBAImageUncompressed* icon = element->getIcon();
        BOOLEAN withIcon = icon != NULL && group->isShowIconsSet();
        // Icon buttons show their text only if the group asks for it
        String text = !withIcon || group->isShowTextSet() ?
            element->getName() : String("");

        const String shownText = XtTextEncoding::toLocale(text);
        Arg buttonArgs[10]; Cardinal buttonN = 0;
        XtSetArg(buttonArgs[buttonN], XtNlabel, shownText.c_str()); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNinternational, True); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNfontSet, fontSet); ++buttonN;
        if ( horizontal ) {
            XtSetArg(buttonArgs[buttonN], XtNx, BUTTON_SPACING +
                     ((int)(i)) * (ICON_BUTTON_SIZE + BUTTON_SPACING)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNy, BUTTON_SPACING); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNwidth,
                     withIcon ? ICON_BUTTON_SIZE : TEXT_BUTTON_WIDTH); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNheight, ICON_BUTTON_SIZE); ++buttonN;
        }
        else {
            XtSetArg(buttonArgs[buttonN], XtNx, MARGIN); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNy, MARGIN + ((int)(i)) *
                     (BUTTON_HEIGHT + BUTTON_SPACING)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNwidth, width - 2 * MARGIN); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNheight, BUTTON_HEIGHT); ++buttonN;
        }
        XtWidget button = XtCreateManagedWidget("commandButton",
            commandWidgetClass, frame, buttonArgs, buttonN);

        if ( withIcon ) {
            Pixmap pixmap = XtWidgetSupport::createIconPixmap(button, *icon);
            if ( pixmap != None ) {
                // Label resizes to the bitmap: the size of the button stays
                XtVaSetValues(button, XtNbitmap, pixmap,
                              XtNwidth, ICON_BUTTON_SIZE,
                              XtNheight, ICON_BUTTON_SIZE, NULL);
            }
        }
        bindCommand(button, executor, element->getId());
    }
    return frame;
}
