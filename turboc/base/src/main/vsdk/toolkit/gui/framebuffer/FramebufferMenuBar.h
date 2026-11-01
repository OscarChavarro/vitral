#ifndef VITRAL_FRAMEBUFFER_MENU_BAR_H
#define VITRAL_FRAMEBUFFER_MENU_BAR_H

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferSnapshot.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferTheme.h"

class Framebuffer;
class FramebufferEvent;
class KeyEvent;
class WidgetMenu;
class WidgetMenuElement;
class WidgetMenuItem;

// A Motif style menu bar for a Widget menu tree: cascade buttons that post pull-down menus, nested cascades,
// separators, mnemonics, accelerators (shown, not bound), check marks and grayed items.
//
// Keyboard: F10 posts the first menu; arrows move between entries and menus; Enter or Space opens a cascade or
// chooses an item; a mnemonic letter does the same for its entry; Escape closes the innermost menu, and the bar when
// only one menu is open. Every key is consumed while the bar is active. Mouse: pressing a cascade button posts its
// menu (pressing it again closes it); moving over the bar or the menus while one is posted arms entries and posts
// cascades; releasing button 1 on an item chooses it; pressing outside closes everything.
//
// Choosing an item only records it for takeChosen(): the bar never runs commands. Pull-down menus save the pixels
// they cover and put them back when they close.
// turboc port note: Entry and Popup are public, as Turbo C++ 3.0 instantiates ArrayList<Entry> outside the class.
class FramebufferMenuBar {
public:
    FramebufferMenuBar();
    ~FramebufferMenuBar();

    void setMenu(WidgetMenu *menu);
    // Lays the bar out along the top of an area, wrapping to more rows when it is too narrow, and forgets any posted
    // menu (the surface is about to be redrawn). Returns the height of the bar.
    int layout(Framebuffer &surface, const FramebufferTheme &theme, int x, int y, int width);
    // Draws the bar and the posted menus.
    void draw();
    BOOLEAN isActive() const { return active; }
    // Closes every menu and disarms the bar.
    void deactivate();
    // Posts a menu as a Motif popup menu with its top-left corner at (x, y), moved to fit on the surface. It is not
    // attached to the bar: keyPressed() and mouseEvent() drive it as the bar's menus (Left and Right do not move to
    // other menus) and takeChosen() returns its chosen item. The menu must live while it is posted.
    void popup(WidgetMenu *menu, int x, int y);
    // Return true when the event was for the menus.
    BOOLEAN keyPressed(const KeyEvent &event);
    BOOLEAN mouseEvent(const FramebufferEvent &event);
    // The item chosen since the last call, or null.
    WidgetMenuItem *takeChosen();

    class Entry {
    public:
        WidgetMenuElement *element;
        String label;
        String accelerator;
        char mnemonic;
        BOOLEAN submenu;
        BOOLEAN separator;
        BOOLEAN enabled;
        BOOLEAN checkable;
        BOOLEAN checked;
        int x;
        int y;
        int width;
        int height;
    };

    class Popup {
    public:
        WidgetMenu *menu;
        ArrayList<Entry> entries;
        int x;
        int y;
        int width;
        int height;
        int armed;
        FramebufferSnapshot saved;
    };

private:
    Framebuffer *surface;
    FramebufferTheme theme;
    WidgetMenu *menubar;
    ArrayList<Entry> bar;
    ArrayList<Popup *> popups;
    int barX;
    int barY;
    int barWidth;
    int barHeight;
    int barArmed;
    BOOLEAN active;
    BOOLEAN popupMode;
    BOOLEAN pressed;
    WidgetMenuItem *chosen;

    FramebufferMenuBar(const FramebufferMenuBar &other);
    FramebufferMenuBar &operator=(const FramebufferMenuBar &other);

    static Entry describe(WidgetMenuElement *element);
    static BOOLEAN selectable(const Entry &entry);
    int margin() const;
    int padding() const;
    int entryHeight() const;
    int separatorHeight() const;

    void drawBar();
    void drawBarEntry(int index);
    void drawPopup(const Popup &popup);
    void drawEntry(const Popup &popup, int index);

    void post(int barIndex, BOOLEAN armFirst);
    void postSubmenu(int level, BOOLEAN armFirst);
    Popup *openPopup(WidgetMenu *menu, int anchorX, int anchorY, int anchorWidth, int anchorHeight, BOOLEAN beside);
    void closeFrom(int level);
    void armBar(int index);
    void arm(int level, int index);
    void armAt(int level, int index);
    void step(int level, int direction);
    void choose(int level, int index);
    BOOLEAN mnemonic(char key);
    void locate(int x, int y, int &level, int &index) const;
};

/** Equality of the elements of ArrayList (see vsdkEquals in ArrayList.h) */
inline BOOLEAN
vsdkEquals(const FramebufferMenuBar::Entry &a, const FramebufferMenuBar::Entry &b)
{
    return a.element == b.element && a.x == b.x && a.y == b.y && a.width == b.width && a.height == b.height;
}

#endif
