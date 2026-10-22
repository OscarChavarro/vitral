#include <ctype.h>
#include <string.h>

#include "io/GuiJsonReader.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.h"

GuiJsonReader::GuiJsonReader(const String& text)
    : text(text), data(text.c_str()), length(text.length()), position(0)
{
}

long GuiJsonReader::indexOf(const char* token) const
{
    const char* found = strstr(data, token);
    return found != NULL ? (long)(found - data) : -1;
}

long GuiJsonReader::indexOf(char token, long from) const
{
    if (from < 0 || from >= length) return -1;
    const char* found = strchr(data + from, token);
    return found != NULL ? (long)(found - data) : -1;
}

GuiNode GuiJsonReader::readMenuBar()
{
    position = 0;
    skipValueStart();
    GuiNode root = readNode();
    return root;
}

ArrayList<GuiNode> GuiJsonReader::readPopups()
{
    const long popups = indexOf("\"popups\"");
    if (popups < 0) return ArrayList<GuiNode>();
    position = indexOf('[', popups);
    if (position < 0) return ArrayList<GuiNode>();
    return readNodes();
}

TreeMap<String, String> GuiJsonReader::readCommandLabels()
{
    return readCommandProperty("name");
}

TreeMap<String, String> GuiJsonReader::readCommandIcons()
{
    return readCommandProperty("icon");
}

TreeMap<String, String> GuiJsonReader::readCommandProperty(
    const String& property)
{
    const long commands = indexOf("\"commands\"");
    if (commands < 0) return TreeMap<String, String>();
    position = indexOf('[', commands);
    if (position < 0) return TreeMap<String, String>();
    return readCommandArray(property);
}

TreeMap<String, String> GuiJsonReader::readMessages()
{
    const long messages = indexOf("\"messages\"");
    if (messages < 0) return TreeMap<String, String>();
    position = indexOf('{', messages);
    if (position < 0) return TreeMap<String, String>();
    return readStringMap();
}

ArrayList<String> GuiJsonReader::readButtonGroupCommands(
    const String& name)
{
    return readButtonGroup(name).commands;
}

ArrayList<GuiCommand> GuiJsonReader::readCommands()
{
    ArrayList<GuiCommand> result;
    const long commands = indexOf("\"commands\"");
    if (commands < 0) return result;
    position = indexOf('[', commands);
    if (position < 0) return result;
    consume('[');
    while (!consume(']')) {
        GuiCommand command;
        consume('{');
        while (!consume('}')) {
            const String key = stringValue(); consume(':');
            whitespace();
            if (key == "id") command.id = stringValue();
            else if (key == "help" && position < length && data[position] == '[') command.help = readStrings();
            else if (key == "help") command.help.add(stringValue());
            else if (position < length && data[position] == '\"') command.properties.put(key, stringValue());
            else skipValue();
            consume(',');
        }
        if (!command.id.isEmpty()) result.add(command);
        consume(',');
    }
    return result;
}

ArrayList<GuiButtonGroup> GuiJsonReader::readButtonGroups()
{
    ArrayList<GuiButtonGroup> result;
    const long groups = indexOf("\"buttonGroups\"");
    if (groups < 0) return result;
    position = indexOf('[', groups);
    if (position < 0) return result;
    consume('[');
    while (!consume(']')) {
        GuiButtonGroup group;
        consume('{');
        while (!consume('}')) {
            const String key = stringValue(); consume(':');
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

GuiButtonGroup GuiJsonReader::readButtonGroup(const String& name)
{
    GuiButtonGroup notFound;
    const long groups = indexOf("\"buttonGroups\"");
    if (groups < 0) return notFound;
    position = indexOf('[', groups);
    if (position < 0) return notFound;
    consume('[');
    while (!consume(']')) {
        GuiButtonGroup group;
        consume('{');
        while (!consume('}')) {
            const String key = stringValue(); consume(':');
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
    if (position + 4 <= length && strncmp(data + position, "true", 4) == 0) { position += 4; return true; }
    if (position + 5 <= length && strncmp(data + position, "false", 5) == 0) { position += 5; return false; }
    skipValue();
    return false;
}

void GuiJsonReader::whitespace()
{
    while (position < length && isspace(((unsigned char)(data[position])))) ++position;
}

bool GuiJsonReader::consume(char c)
{
    whitespace();
    if (position < length && data[position] == c) { ++position; return true; }
    return false;
}

String GuiJsonReader::stringValue()
{
    whitespace();
    if (position >= length || data[position++] != '\"') throw RuntimeException("Invalid GUI JSON string");
    StringBuilder result;
    while (position < length && data[position] != '\"') {
        char c = data[position++];
        if (c == '\\' && position < length) {
            c = data[position++];
            if (c == 'n') result.append('\n');
            else if (c == 't') result.append('\t');
            else result.append(c);
        }
        else result.append(c);
    }
    if (position >= length) throw RuntimeException("Unterminated GUI JSON string");
    ++position;
    return result.toString();
}

void GuiJsonReader::skipValueStart()
{
    whitespace();
    if (!consume('{')) throw RuntimeException("Invalid GUI JSON document");
    while (position < length) {
        String key = stringValue();
        consume(':');
        if (key == "menubar") return;
        skipValue();
        if (!consume(',')) break;
    }
    throw RuntimeException("GUI JSON has no menubar");
}

void GuiJsonReader::skipValue()
{
    whitespace();
    if (position >= length) return;
    if (data[position] == '\"') { stringValue(); return; }
    char open = data[position];
    if (open != '{' && open != '[') { while (position < length && data[position] != ',' && data[position] != '}' && data[position] != ']') ++position; return; }
    char close = open == '{' ? '}' : ']'; ++position;
    while (position < length) { whitespace(); if (data[position] == close) { ++position; return; } skipValue(); whitespace(); if (position < length && data[position] == ',') ++position; }
}

ArrayList<String> GuiJsonReader::readStrings()
{
    ArrayList<String> result;
    consume('[');
    while (!consume(']')) { result.add(stringValue()); consume(','); }
    return result;
}

ArrayList<GuiNode> GuiJsonReader::readNodes()
{
    ArrayList<GuiNode> result;
    consume('[');
    while (!consume(']')) { result.add(readNode()); consume(','); }
    return result;
}

TreeMap<String, String> GuiJsonReader::readCommandArray(
    const String& property)
{
    TreeMap<String, String> result;
    consume('[');
    while (!consume(']')) {
        String id;
        String value;
        bool found = false;
        consume('{');
        while (!consume('}')) {
            const String key = stringValue(); consume(':');
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

TreeMap<String, String> GuiJsonReader::readStringMap()
{
    TreeMap<String, String> result;
    consume('{');
    while (!consume('}')) {
        const String key = stringValue(); consume(':');
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
        String key = stringValue(); consume(':');
        if (key == "type") node.type = stringValue();
        else if (key == "name") node.name = stringValue();
        else if (key == "modifiers") node.modifiers = readStrings();
        else if (key == "children") node.children = readNodes();
        else skipValue();
        consume(',');
    }
    return node;
}
