#include <map>

#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Composite.h>
#include <X11/Xaw/Command.h>
#include <X11/Xaw/Label.h>

#include "java/io/File.h"
#include "gui/xt/XlibImageConverter.h"
#include "gui/xt/XtButtonsPanel.h"
#include "gui/xt/XtCommandExecutor.h"
#include "io/GuiJsonReader.h"
#include "model/GuiState.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

namespace {

const int BUTTON_HEIGHT = 26;
const int BUTTON_SPACING = 3;
const int ICON_BUTTON_SIZE = 30;
const int MARGIN = 8;

/**
@return the path of the transparency mask of an icon of the Java
application ("xIcon.png" -> "xIconMask.png"), as named there
*/
std::string maskPathOf(const std::string& iconPath)
{
    size_t dot = iconPath.rfind('.');
    if ( dot == std::string::npos ) {
        return "";
    }
    return iconPath.substr(0, dot) + "Mask" + iconPath.substr(dot);
}

Visual* visualOf(Widget widget)
{
    Widget shell = widget;
    while ( shell != nullptr && !XtIsShell(shell) ) {
        shell = XtParent(shell);
    }
    Visual* visual = nullptr;
    if ( shell != nullptr ) {
        XtVaGetValues(shell, XtNvisual, &visual, nullptr);
    }
    if ( visual == nullptr ) {
        visual = DefaultVisualOfScreen(XtScreen(widget));
    }
    return visual;
}

}

XtButtonsPanel::XtButtonsPanel(Widget parent, const std::string& guiDefinition,
                               const std::string& groupName,
                               XtCommandExecutor* executor, XFontSet fontSet,
                               int x, int y, int width)
    : container(nullptr), display(XtDisplay(parent)), executor(executor),
      height(0)
{
    GuiButtonGroup group = GuiJsonReader(guiDefinition).readButtonGroup(groupName);
    std::map<std::string, std::string> labels =
        GuiJsonReader(guiDefinition).readCommandLabels();
    std::map<std::string, std::string> icons =
        GuiJsonReader(guiDefinition).readCommandIcons();

    if ( group.horizontal ) {
        height = ICON_BUTTON_SIZE + 2 * BUTTON_SPACING;
    }
    else {
        height = 2 * MARGIN + static_cast<int>(group.commands.size()) *
            (BUTTON_HEIGHT + BUTTON_SPACING);
    }
    if ( height < BUTTON_HEIGHT ) {
        height = BUTTON_HEIGHT;
    }

    Arg args[8]; Cardinal n = 0;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    container = XtCreateManagedWidget(
        "buttonsPanel", compositeWidgetClass, parent, args, n);

    if ( !group.found ) {
        std::string message = "No ButtonGroup \"" + groupName +
            "\" found in GUI";
        Arg labelArgs[8]; Cardinal labelN = 0;
        XtSetArg(labelArgs[labelN], XtNlabel, message.c_str()); ++labelN;
        XtSetArg(labelArgs[labelN], XtNinternational, True); ++labelN;
        XtSetArg(labelArgs[labelN], XtNfontSet, fontSet); ++labelN;
        XtSetArg(labelArgs[labelN], XtNwidth, width); ++labelN;
        XtCreateManagedWidget("noButtonGroup", labelWidgetClass, container,
                              labelArgs, labelN);
        return;
    }

    for ( size_t i = 0; i < group.commands.size(); i++ ) {
        const std::string& id = group.commands[i];
        std::map<std::string, std::string>::const_iterator label =
            labels.find(id);
        std::map<std::string, std::string>::const_iterator icon =
            icons.find(id);
        const std::string text = label != labels.end() ? label->second : id;
        bool withIcon = group.showIcons && icon != icons.end();

        Arg buttonArgs[10]; Cardinal buttonN = 0;
        XtSetArg(buttonArgs[buttonN], XtNlabel, text.c_str()); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNinternational, True); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNfontSet, fontSet); ++buttonN;
        if ( group.horizontal ) {
            int buttonWidth = withIcon ? ICON_BUTTON_SIZE : 120;
            XtSetArg(buttonArgs[buttonN], XtNx, BUTTON_SPACING + static_cast<int>(i) *
                     (ICON_BUTTON_SIZE + BUTTON_SPACING)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNy, BUTTON_SPACING); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNwidth, buttonWidth); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNheight, ICON_BUTTON_SIZE); ++buttonN;
        }
        else {
            XtSetArg(buttonArgs[buttonN], XtNx, MARGIN); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNy, MARGIN + static_cast<int>(i) *
                     (BUTTON_HEIGHT + BUTTON_SPACING)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNwidth, width - 2 * MARGIN); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNheight, BUTTON_HEIGHT); ++buttonN;
        }
        Widget button = XtCreateManagedWidget(
            "commandButton", commandWidgetClass, container, buttonArgs, buttonN);
        if ( withIcon ) {
            std::string path = std::string(GuiState::APPLICATION_DATA_FOLDER) +
                icon->second;
            Pixmap pixmap = loadIcon(button, path, maskPathOf(path));
            if ( pixmap != None ) {
                // Icon buttons keep their size: Label resizes to the bitmap
                XtVaSetValues(button, XtNbitmap, pixmap,
                              XtNwidth, ICON_BUTTON_SIZE,
                              XtNheight, ICON_BUTTON_SIZE, nullptr);
            }
        }
        Binding* binding = new Binding;
        binding->panel = this;
        binding->command = id;
        bindings.push_back(binding);
        XtAddCallback(button, XtNcallback, &XtButtonsPanel::activate, binding);
    }
}

XtButtonsPanel::~XtButtonsPanel()
{
    for ( size_t i = 0; i < bindings.size(); i++ ) {
        delete bindings[i];
    }
    for ( size_t i = 0; i < pixmaps.size(); i++ ) {
        XFreePixmap(display, pixmaps[i]);
    }
}

Pixmap XtButtonsPanel::loadIcon(Widget button, const std::string& path,
                                const std::string& maskPath)
{
    java::File file(path.c_str());
    if ( !file.canRead() ) {
        return None;
    }
    RGBImageUncompressed* image = ImagePersistence::importRGB(file);
    if ( image == nullptr ) {
        return None;
    }
    RGBImageUncompressed* mask = nullptr;
    java::File maskFile(maskPath.c_str());
    if ( !maskPath.empty() && maskFile.canRead() ) {
        mask = ImagePersistence::importRGB(maskFile);
    }

    Pixel backgroundPixel = 0;
    Colormap colormap = 0;
    int depth = 0;
    XtVaGetValues(button, XtNbackground, &backgroundPixel,
                  XtNcolormap, &colormap, XtNdepth, &depth, nullptr);
    XColor background;
    background.pixel = backgroundPixel;
    XQueryColor(display, colormap, &background);

    Pixmap pixmap = XlibImageConverter::createPixmap(
        display, RootWindowOfScreen(XtScreen(button)), visualOf(button),
        depth, *image, mask, background);
    delete image;
    delete mask;
    if ( pixmap != None ) {
        pixmaps.push_back(pixmap);
    }
    return pixmap;
}

void XtButtonsPanel::activate(Widget, XtPointer clientData, XtPointer)
{
    Binding* binding = static_cast<Binding*>(clientData);
    if ( binding != nullptr && binding->panel->executor != nullptr ) {
        binding->panel->executor->executeCommand(binding->command);
    }
}
