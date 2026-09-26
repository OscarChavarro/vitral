#include <cstdio>
#include <map>
#include <vector>

#include "java/io/File.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "io/GuiJsonReader.h"
#include "io/GuiI18nContextBuilder.h"

namespace {

WidgetMenu* buildMenu(Widget* context, const GuiNode& definition,
                      bool registerChildPopups)
{
    WidgetMenu* menu = new WidgetMenu(context);
    menu->setName(definition.name.c_str());

    for ( size_t i = 0; i < definition.children.size(); i++ ) {
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
        if ( !child.name.empty() ) {
            item->setName(child.name.c_str());
        }
        for ( size_t j = 0; j < child.modifiers.size(); j++ ) {
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
java::File imageFile(const std::string& globalDataPath,
                     const std::string& filename)
{
    return java::File((globalDataPath + "/" + filename).c_str());
}

void reportUnreadable(const std::string& filename)
{
    fprintf(stderr, "Warning: could not read the image file \"%s\".\n",
            filename.c_str());
}

RGBAImageUncompressed* loadIcon(const std::string& globalDataPath,
                                const GuiCommand& command, const char* field)
{
    if ( !command.has(field) ) {
        return nullptr;
    }
    std::string filename = command.get(field);
    java::File file = imageFile(globalDataPath, filename);
    RGBAImageUncompressed* image =
        file.canRead() ? ImagePersistence::importRGBA(file) : nullptr;
    if ( image == nullptr ) {
        reportUnreadable(filename);
    }
    return image;
}

RGBImageUncompressed* loadMask(const std::string& globalDataPath,
                               const GuiCommand& command, const char* field)
{
    if ( !command.has(field) ) {
        return nullptr;
    }
    std::string filename = command.get(field);
    java::File file = imageFile(globalDataPath, filename);
    RGBImageUncompressed* image =
        file.canRead() ? ImagePersistence::importRGB(file) : nullptr;
    if ( image == nullptr ) {
        reportUnreadable(filename);
    }
    return image;
}

void importCommands(Widget* context, const std::string& json,
                    const std::string& globalDataPath)
{
    const std::vector<GuiCommand> commands = GuiJsonReader(json).readCommands();
    for ( size_t i = 0; i < commands.size(); i++ ) {
        const GuiCommand& definition = commands[i];
        WidgetCommand* command = new WidgetCommand();
        command->setId(definition.id.c_str());
        command->setName(definition.get("name").c_str());
        command->setBrief(definition.get("brief").c_str());
        for ( size_t j = 0; j < definition.help.size(); j++ ) {
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

void importButtonGroups(Widget* context, const std::string& json)
{
    const std::vector<GuiButtonGroup> groups =
        GuiJsonReader(json).readButtonGroups();
    for ( size_t i = 0; i < groups.size(); i++ ) {
        WidgetButtonGroup* group = new WidgetButtonGroup(context);
        group->setName(groups[i].name.c_str());
        group->setShowIcons(groups[i].showIcons);
        group->setShowText(groups[i].showText);
        group->setTitle(groups[i].showTitle);
        group->setDirection(groups[i].horizontal ?
            WidgetButtonGroup::HORIZONTAL : WidgetButtonGroup::VERTICAL);
        for ( size_t j = 0; j < groups[i].commands.size(); j++ ) {
            group->addCommandByName(groups[i].commands[j].c_str());
        }
        context->addButtonGroup(group);
    }
}

}

Widget* GuiI18nContextBuilder::build(const std::string& json,
                                     const std::string& globalDataPath)
{
    Widget* context = new Widget();

    importCommands(context, json, globalDataPath);
    context->setMenubar(buildMenu(context, GuiJsonReader(json).readMenuBar(),
                                  true));

    const std::vector<GuiNode> popups = GuiJsonReader(json).readPopups();
    for ( size_t i = 0; i < popups.size(); i++ ) {
        context->addPopupMenu(buildMenu(context, popups[i], true));
    }

    importButtonGroups(context, json);

    const std::map<std::string, std::string> messages =
        GuiJsonReader(json).readMessages();
    std::map<std::string, std::string>::const_iterator message;
    for ( message = messages.begin(); message != messages.end(); ++message ) {
        context->addMessage(message->first.c_str(), message->second.c_str());
    }
    return context;
}
