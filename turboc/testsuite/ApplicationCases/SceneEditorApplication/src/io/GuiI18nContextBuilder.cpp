#include <stdio.h>
#include "java/util/TreeMap.h"

#include "java/io/File.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/io/gui/DialogPersistence.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "io/GuiJsonReader.h"
#include "io/GuiI18nContextBuilder.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/nio/file/Files.h"


static WidgetMenu* buildMenu(Widget* context, const GuiNode& definition,
                      bool registerChildPopups)
{
    WidgetMenu* menu = new WidgetMenu(context);
    menu->setName(definition.name.c_str());

    for ( long i = 0; i < definition.children.size(); i++ ) {
        const GuiNode& child = definition.children[i];
        if ( child.type == "menu" ) {
            WidgetMenu* childMenu =
                buildMenu(context, child, registerChildPopups);
            if ( registerChildPopups ) {
                context->addPopupMenu(childMenu);
            }
            menu->addChild(childMenu);
            continue;
        }
        WidgetMenuItem* item = new WidgetMenuItem(context);
        if ( !child.name.isEmpty() ) {
            item->setName(child.name.c_str());
        }
        for ( long j = 0; j < child.modifiers.size(); j++ ) {
            item->addModifier(child.modifiers[j].c_str());
        }
        menu->addChild(item);
    }
    return menu;
}

/**
@return the image of a file of the definition, or null if it can not be
read (reporting it, as Java)
*/
static File imageFile(const String& globalDataPath,
                     const String& filename)
{
    return File((globalDataPath + "/" + filename).c_str());
}

static void reportUnreadable(const String& filename)
{
    fprintf(stderr, "Warning: could not read the image file \"%s\".\n",
            filename.c_str());
}

static RGBAImageUncompressed* loadIcon(const String& globalDataPath,
                                const GuiCommand& command, const char* field)
{
    if ( !command.has(field) ) {
        return NULL;
    }
    String filename = command.get(field);
    File file = imageFile(globalDataPath, filename);
    RGBAImageUncompressed* image =
        file.canRead() ? ImagePersistence::importRGBA(file) : NULL;
    if ( image == NULL ) {
        reportUnreadable(filename);
    }
    return image;
}

static RGBImageUncompressed* loadMask(const String& globalDataPath,
                               const GuiCommand& command, const char* field)
{
    if ( !command.has(field) ) {
        return NULL;
    }
    String filename = command.get(field);
    File file = imageFile(globalDataPath, filename);
    RGBImageUncompressed* image =
        file.canRead() ? ImagePersistence::importRGB(file) : NULL;
    if ( image == NULL ) {
        reportUnreadable(filename);
    }
    return image;
}

static void importCommands(Widget* context, const String& json,
                    const String& globalDataPath)
{
    const ArrayList<GuiCommand> commands = GuiJsonReader(json).readCommands();
    for ( long i = 0; i < commands.size(); i++ ) {
        const GuiCommand& definition = commands[i];
        WidgetCommand* command = new WidgetCommand();
        command->setId(definition.id.c_str());
        command->setName(definition.get("name").c_str());
        command->setBrief(definition.get("brief").c_str());
        for ( long j = 0; j < definition.help.size(); j++ ) {
            command->appendToHelp(definition.help[j].c_str());
        }

        command->setIcon(loadIcon(globalDataPath, definition, "icon"));
        command->setSecondaryIcon(
            loadIcon(globalDataPath, definition, "secondaryIcon"));
        command->setIconTransparency(
            loadMask(globalDataPath, definition, "iconTransparency"));
        command->setSecondaryIconTransparency(
            loadMask(globalDataPath, definition, "secondaryIconTransparency"));
        command->applyTransparency();
        command->applySecondTransparency();

        context->addCommand(command);
    }
}

static void importButtonGroups(Widget* context, const String& json)
{
    const ArrayList<GuiButtonGroup> groups =
        GuiJsonReader(json).readButtonGroups();
    for ( long i = 0; i < groups.size(); i++ ) {
        WidgetButtonGroup* group = new WidgetButtonGroup(context);
        group->setName(groups[i].name.c_str());
        group->setShowIcons(groups[i].showIcons);
        group->setShowText(groups[i].showText);
        group->setTitle(groups[i].showTitle);
        group->setDirection(groups[i].horizontal ?
            WidgetButtonGroup::HORIZONTAL : WidgetButtonGroup::VERTICAL);
        for ( long j = 0; j < groups[i].commands.size(); j++ ) {
            group->addCommandByName(groups[i].commands[j].c_str());
        }
        context->addButtonGroup(group);
    }
}


Widget* GuiI18nContextBuilder::build(const String& json,
                                     const String& globalDataPath)
{
    Widget* context = new Widget();

    importCommands(context, json, globalDataPath);
    context->setMenubar(buildMenu(context, GuiJsonReader(json).readMenuBar(),
                                  true));

    const ArrayList<GuiNode> popups = GuiJsonReader(json).readPopups();
    for ( long i = 0; i < popups.size(); i++ ) {
        context->addPopupMenu(buildMenu(context, popups[i], true));
    }

    importButtonGroups(context, json);

    const TreeMap<String, String> messages =
        GuiJsonReader(json).readMessages();
    for ( long m = 0; m < messages.size(); m++ ) {
        context->addMessage(messages.getKey(m).c_str(),
                            messages.getValue(m).c_str());
    }

    // The dialogs, shared by every language and port
    String dialogsPath = globalDataPath + "/etc/gui/dialogs.json";
    String dialogsJson;
    if ( !Files::readString(dialogsPath, dialogsJson) ) {
        delete context;
        throw VSDKFatalException(
            ("Could not open dialog definitions: " + dialogsPath).c_str());
    }
    try {
        DialogPersistence::importDialogs(dialogsJson, context);
    }
    catch ( const IllegalArgumentException& e ) {
        delete context;
        throw VSDKFatalException(e.getMessage().c_str());
    }
    return context;
}
