#include <ctype.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "jackson/databind/ObjectMapper.h"

/**
turboc port note: the members are defined out of the class body, as Turbo
C++ 3.0 copies the JsonNode returned by an inline member bitwise, without its
copy constructor (parseObject returned an empty object).
*/
class JsonParser {
public:
explicit JsonParser(const char* text);

JsonNode parse();

private:
const char* text_;
// turboc port note: long, not int as in the cpp port: a 16 bit int overflows
// past 32767 characters, and the GUI files (english.json) are longer.
long pos_;

void skipWs();

BOOLEAN startsWith(const char* token) const;

JsonNode parseValue();

JsonNode parseObject();

JsonNode parseArray();

String parseString();

double parseNumber();
};

JsonParser::JsonParser(const char* text) : text_(text), pos_(0) {}

JsonNode
JsonParser::parse() {
    skipWs();
    JsonNode value = parseValue();
    skipWs();
    return value;
}

void
JsonParser::skipWs() {
    while (text_[pos_] != '\0' && isspace(((unsigned char)(text_[pos_])))) pos_++;
}

BOOLEAN
JsonParser::startsWith(const char* token) const {
    for (int i = 0; token[i] != '\0'; ++i) {
        if (text_[pos_ + i] != token[i]) return FALSE;
    }
    return TRUE;
}

JsonNode
JsonParser::parseValue() {
    skipWs();
    char c = text_[pos_];
    // turboc port note: objects, arrays and texts go through named locals,
    // not returned as the temporaries of the cpp port (return parseObject(),
    // newString(parseString())), which Turbo C++ 3.0 loses or destroys twice
    if (c == '{') { JsonNode object = parseObject(); return object; }
    if (c == '[') { JsonNode array = parseArray(); return array; }
    if (c == '"') {
        String text = parseString();
        JsonNode node = JsonNode::newString(text);
        return node;
    }
    if (c == 't' && startsWith("true")) { pos_ += 4; return JsonNode::newBoolean(TRUE); }
    if (c == 'f' && startsWith("false")) { pos_ += 5; return JsonNode::newBoolean(FALSE); }
    if (c == 'n' && startsWith("null")) { pos_ += 4; return JsonNode::newNull(); }
    return JsonNode::newNumber(parseNumber());
}

JsonNode
JsonParser::parseObject() {
    JsonNode obj = JsonNode::newObject();
    pos_++; // {
    skipWs();
    if (text_[pos_] == '}') { pos_++; return obj; }
    while (text_[pos_] != '\0') {
        skipWs();
        String key = parseString();
        skipWs();
        if (text_[pos_] == ':') pos_++;
        JsonNode value = parseValue();
        obj.put(key, value);
        skipWs();
        if (text_[pos_] == '}') { pos_++; break; }
        if (text_[pos_] == ',') pos_++;
    }
    return obj;
}

JsonNode
JsonParser::parseArray() {
    JsonNode arr = JsonNode::newArray();
    pos_++; // [
    skipWs();
    if (text_[pos_] == ']') { pos_++; return arr; }
    while (text_[pos_] != '\0') {
        JsonNode value = parseValue();
        arr.add(value);
        skipWs();
        if (text_[pos_] == ']') { pos_++; break; }
        if (text_[pos_] == ',') pos_++;
    }
    return arr;
}

String
JsonParser::parseString() {
    if (text_[pos_] != '"') return String("");
    pos_++; // "
    char buffer[4096];
    int out = 0;
    while (text_[pos_] != '\0' && text_[pos_] != '"' && out < 4095) {
        char c = text_[pos_++];
        if (c == '\\') {
            char esc = text_[pos_++];
            if (esc == '"' || esc == '\\' || esc == '/') c = esc;
            else if (esc == 'b') c = '\b';
            else if (esc == 'f') c = '\f';
            else if (esc == 'n') c = '\n';
            else if (esc == 'r') c = '\r';
            else if (esc == 't') c = '\t';
            else c = esc;
        }
        buffer[out++] = c;
    }
    if (text_[pos_] == '"') pos_++;
    buffer[out] = '\0';
    return String(buffer);
}

double
JsonParser::parseNumber() {
    const char* start = text_ + pos_;
    char* end = NULL;
    double value = strtod(start, &end);
    if ((const char*)end == start) return 0.0;
    pos_ += ((long)((const char*)end - start));
    return value;
}

JsonNode ObjectMapper::readTree(const String& content) const {
JsonParser parser(content.c_str());
JsonNode root = parser.parse();
return root;
}

