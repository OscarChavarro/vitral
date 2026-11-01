#include <stddef.h>
#include "jackson/databind/JsonNode.h"
#include "jackson/databind/ObjectMapper.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferMenuReader.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"

static WidgetMenuElement *
readMenuElement(const JsonNode &node, Widget &widget)
{
    const JsonNode *type = node.get(String("type"));
    const JsonNode *name = node.get(String("name"));
    if ( !type || !type->isString() ) {
        return NULL;
    }
    long i;
    if ( type->asText().equals("menu") ) {
        WidgetMenu *menu = new WidgetMenu(&widget);
        if ( name && name->isString() ) {
            menu->setName(name->asText());
        }
        const JsonNode *children = node.get(String("children"));
        if ( children && children->isArray() ) {
            for ( i = 0; i < children->size(); i++ ) {
                const JsonNode *child = children->get(i);
                WidgetMenuElement *element = child ? readMenuElement(*child, widget) : (WidgetMenuElement *)NULL;
                if ( element ) {
                    menu->addChild(element);
                }
            }
        }
        return menu;
    }
    if ( type->asText().equals("item") ) {
        WidgetMenuItem *item = new WidgetMenuItem(&widget);
        if ( name && name->isString() ) {
            item->setName(name->asText());
        }
        const JsonNode *modifiers = node.get(String("modifiers"));
        if ( modifiers && modifiers->isArray() ) {
            for ( i = 0; i < modifiers->size(); i++ ) {
                const JsonNode *modifier = modifiers->get(i);
                if ( modifier && modifier->isString() ) {
                    item->addModifier(modifier->asText());
                }
            }
        }
        return item;
    }
    return NULL;
}

BOOLEAN
FramebufferMenuReader::read(const String &json, Widget &widget)
{
    if ( widget.getMenubar() ) {
        return FALSE;
    }
    ObjectMapper mapper;
    JsonNode root = mapper.readTree(json);
    const JsonNode *bar = root.get(String("menubar"));
    if ( !bar || !bar->isObject() ) {
        return FALSE;
    }
    const JsonNode *type = bar->get(String("type"));
    if ( !type || !type->asText().equals("menu") ) {
        return FALSE;
    }
    widget.setMenubar((WidgetMenu *)readMenuElement(*bar, widget));
    return widget.getMenubar() != NULL;
}
