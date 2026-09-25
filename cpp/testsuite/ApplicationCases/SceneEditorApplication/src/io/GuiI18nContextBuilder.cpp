#include <map>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "io/GuiJsonReader.h"
#include "io/GuiI18nContextBuilder.h"

namespace {

WidgetMenu* buildMenu(Widget* context, const GuiNode& definition)
{
    WidgetMenu* menu = new WidgetMenu(context);
    menu->setName(definition.name.c_str());

    for ( size_t i = 0; i < definition.children.size(); i++ ) {
        const GuiNode& child = definition.children[i];
        if ( child.type == "menu" ) {
            menu->addChild(buildMenu(context, child));
            continue;
        }
        WidgetMenuItem* item = new WidgetMenuItem(context);
        item->setName(child.name.c_str());
        for ( size_t j = 0; j < child.modifiers.size(); j++ ) {
            item->addModifier(child.modifiers[j].c_str());
        }
        menu->addChild(item);
    }
    return menu;
}

}

Widget* GuiI18nContextBuilder::build(const std::string& json)
{
    Widget* context = new Widget();

    const std::vector<GuiNode> popups = GuiJsonReader(json).readPopups();
    for ( size_t i = 0; i < popups.size(); i++ ) {
        context->addPopupMenu(buildMenu(context, popups[i]));
    }

    const std::map<std::string, std::string> commands =
        GuiJsonReader(json).readCommandLabels();
    std::map<std::string, std::string>::const_iterator command;
    for ( command = commands.begin(); command != commands.end(); ++command ) {
        WidgetCommand* widgetCommand = new WidgetCommand();
        widgetCommand->setId(command->first.c_str());
        widgetCommand->setName(command->second.c_str());
        context->addCommand(widgetCommand);
    }

    const std::map<std::string, std::string> messages =
        GuiJsonReader(json).readMessages();
    std::map<std::string, std::string>::const_iterator message;
    for ( message = messages.begin(); message != messages.end(); ++message ) {
        context->addMessage(message->first.c_str(), message->second.c_str());
    }
    return context;
}
