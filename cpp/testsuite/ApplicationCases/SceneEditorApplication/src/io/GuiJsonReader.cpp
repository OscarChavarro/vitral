#include "java/lang/RuntimeException.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include <cctype>

#include "io/GuiJsonReader.h"

GuiNode GuiJsonReader::readMenuBar()
{
    position = 0;
    skipValueStart();
    GuiNode root = readNode();
    return root;
}

java::ArrayList<GuiNode> GuiJsonReader::readPopups()
{
    const int popups = text.indexOf("\"popups\"");
    if (popups == -1) return java::ArrayList<GuiNode>();
    position = text.indexOf('[', popups);
    if (position == -1) return java::ArrayList<GuiNode>();
    return readNodes();
}

java::TreeMap<java::String, java::String> GuiJsonReader::readCommandLabels()
{
    return readCommandProperty("name");
}

java::TreeMap<java::String, java::String> GuiJsonReader::readCommandIcons()
{
    return readCommandProperty("icon");
}

java::TreeMap<java::String, java::String> GuiJsonReader::readCommandProperty(
    const java::String& property)
{
    const int commands = text.indexOf("\"commands\"");
    if (commands == -1) return java::TreeMap<java::String, java::String>();
    position = text.indexOf('[', commands);
    if (position == -1) return java::TreeMap<java::String, java::String>();
    return readCommandArray(property);
}

java::TreeMap<java::String, java::String> GuiJsonReader::readMessages()
{
    const int messages = text.indexOf("\"messages\"");
    if (messages == -1) return java::TreeMap<java::String, java::String>();
    position = text.indexOf('{', messages);
    if (position == -1) return java::TreeMap<java::String, java::String>();
    return readStringMap();
}

java::ArrayList<java::String> GuiJsonReader::readButtonGroupCommands(
    const java::String& name)
{
    return readButtonGroup(name).commands;
}

java::ArrayList<GuiCommand> GuiJsonReader::readCommands()
{
    java::ArrayList<GuiCommand> result;
    const int commands = text.indexOf("\"commands\"");
    if (commands == -1) return result;
    position = text.indexOf('[', commands);
    if (position == -1) return result;
    consume('[');
    while (!consume(']')) {
        GuiCommand command;
        consume('{');
        while (!consume('}')) {
            const java::String key = stringValue(); consume(':');
            whitespace();
            if (key == "id") command.id = stringValue();
            else if (key == "help" && position < text.size() && text[position] == '[') command.help = readStrings();
            else if (key == "help") command.help.add(stringValue());
            else if (position < text.size() && text[position] == '\"') command.properties.put(key, stringValue());
            else skipValue();
            consume(',');
        }
        if (!command.id.isEmpty()) result.add(command);
        consume(',');
    }
    return result;
}

java::ArrayList<GuiButtonGroup> GuiJsonReader::readButtonGroups()
{
    java::ArrayList<GuiButtonGroup> result;
    const int groups = text.indexOf("\"buttonGroups\"");
    if (groups == -1) return result;
    position = text.indexOf('[', groups);
    if (position == -1) return result;
    consume('[');
    while (!consume(']')) {
        GuiButtonGroup group;
        consume('{');
        while (!consume('}')) {
            const java::String key = stringValue(); consume(':');
            if (key == "name") group.name = stringValue();
            else if (key == "title") group.title = stringValue();
            else if (key == "direction") group.horizontal = stringValue() == "horizontal";
            else if (key == "showTitle") group.showTitle = booleanValue();
            else if (key == "showIcons") group.showIcons = booleanValue();
            else if (key == "showText") group.showText = booleanValue();
            else if (key == "commands") group.commands = readStrings();
            else skipValue();
            consume(',');
        }
        group.found = true;
        result.add(group);
        consume(',');
    }
    return result;
}

GuiButtonGroup GuiJsonReader::readButtonGroup(const java::String& name)
{
    GuiButtonGroup notFound;
    const int groups = text.indexOf("\"buttonGroups\"");
    if (groups == -1) return notFound;
    position = text.indexOf('[', groups);
    if (position == -1) return notFound;
    consume('[');
    while (!consume(']')) {
        GuiButtonGroup group;
        consume('{');
        while (!consume('}')) {
            const java::String key = stringValue(); consume(':');
            if (key == "name") group.name = stringValue();
            else if (key == "title") group.title = stringValue();
            else if (key == "direction") group.horizontal = stringValue() == "horizontal";
            else if (key == "showTitle") group.showTitle = booleanValue();
            else if (key == "showIcons") group.showIcons = booleanValue();
            else if (key == "showText") group.showText = booleanValue();
            else if (key == "commands") group.commands = readStrings();
            else skipValue();
            consume(',');
        }
        if (group.name == name) {
            group.found = true;
            return group;
        }
        consume(',');
    }
    return notFound;
}

bool GuiJsonReader::booleanValue()
{
    whitespace();
    if (text.startsWith("true", position)) { position += 4; return true; }
    if (text.startsWith("false", position)) { position += 5; return false; }
    skipValue();
    return false;
}

void GuiJsonReader::whitespace()
{
    while (position < text.size() && std::isspace(static_cast<unsigned char>(text[position]))) ++position;
}

bool GuiJsonReader::consume(char c)
{
    whitespace();
    if (position < text.size() && text[position] == c) { ++position; return true; }
    return false;
}

java::String GuiJsonReader::stringValue()
{
    whitespace();
    if (position >= text.size() || text[position++] != '\"') throw java::RuntimeException("Invalid GUI JSON string");
    java::String result;
    while (position < text.size() && text[position] != '\"') {
        char c = text[position++];
        if (c == '\\' && position < text.size()) {
            c = text[position++];
            if (c == 'n') result += '\n';
            else if (c == 't') result += '\t';
            else result += c;
        }
        else result += c;
    }
    if (position >= text.size()) throw java::RuntimeException("Unterminated GUI JSON string");
    ++position;
    return result;
}

void GuiJsonReader::skipValueStart()
{
    whitespace();
    if (!consume('{')) throw java::RuntimeException("Invalid GUI JSON document");
    while (position < text.size()) {
        java::String key = stringValue();
        consume(':');
        if (key == "menubar") return;
        skipValue();
        if (!consume(',')) break;
    }
    throw java::RuntimeException("GUI JSON has no menubar");
}

void GuiJsonReader::skipValue()
{
    whitespace();
    if (position >= text.size()) return;
    if (text[position] == '\"') { stringValue(); return; }
    char open = text[position];
    if (open != '{' && open != '[') { while (position < text.size() && text[position] != ',' && text[position] != '}' && text[position] != ']') ++position; return; }
    char close = open == '{' ? '}' : ']'; ++position;
    while (position < text.size()) { whitespace(); if (text[position] == close) { ++position; return; } skipValue(); whitespace(); if (position < text.size() && text[position] == ',') ++position; }
}

java::ArrayList<java::String> GuiJsonReader::readStrings()
{
    java::ArrayList<java::String> result;
    consume('[');
    while (!consume(']')) { result.add(stringValue()); consume(','); }
    return result;
}

java::ArrayList<GuiNode> GuiJsonReader::readNodes()
{
    java::ArrayList<GuiNode> result;
    consume('[');
    while (!consume(']')) { result.add(readNode()); consume(','); }
    return result;
}

java::TreeMap<java::String, java::String> GuiJsonReader::readCommandArray(
    const java::String& property)
{
    java::TreeMap<java::String, java::String> result;
    consume('[');
    while (!consume(']')) {
        java::String id;
        java::String value;
        bool found = false;
        consume('{');
        while (!consume('}')) {
            const java::String key = stringValue(); consume(':');
            if (key == "id") id = stringValue();
            else if (key == property) { value = stringValue(); found = true; }
            else skipValue();
            consume(',');
        }
        if (!id.isEmpty() && found) result.put(id, value);
        consume(',');
    }
    return result;
}

java::TreeMap<java::String, java::String> GuiJsonReader::readStringMap()
{
    java::TreeMap<java::String, java::String> result;
    consume('{');
    while (!consume('}')) {
        const java::String key = stringValue(); consume(':');
        result.put(key, stringValue());
        consume(',');
    }
    return result;
}

GuiNode GuiJsonReader::readNode()
{
    GuiNode node;
    consume('{');
    while (!consume('}')) {
        java::String key = stringValue(); consume(':');
        if (key == "type") node.type = stringValue();
        else if (key == "name") node.name = stringValue();
        else if (key == "modifiers") node.modifiers = readStrings();
        else if (key == "children") node.children = readNodes();
        else skipValue();
        consume(',');
    }
    return node;
}
