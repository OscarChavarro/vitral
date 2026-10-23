#include <ctype.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "jackson/databind/ObjectMapper.h"

class JsonParser {
public:
explicit JsonParser(const char* text) : text_(text), pos_(0) {}

JsonNode parse() {
    skipWs();
    JsonNode value = parseValue();
    skipWs();
    return value;
}

private:
const char* text_;
int pos_;

void skipWs() {
    while (text_[pos_] != '\0' && isspace(((unsigned char)(text_[pos_])))) pos_++;
}

BOOLEAN startsWith(const char* token) const {
    for (int i = 0; token[i] != '\0'; ++i) {
        if (text_[pos_ + i] != token[i]) return FALSE;
    }
    return TRUE;
}

JsonNode parseValue() {
    skipWs();
    char c = text_[pos_];
    if (c == '{') return parseObject();
    if (c == '[') return parseArray();
    if (c == '"') return JsonNode::newString(parseString());
    if (c == 't' && startsWith("true")) { pos_ += 4; return JsonNode::newBoolean(TRUE); }
    if (c == 'f' && startsWith("false")) { pos_ += 5; return JsonNode::newBoolean(FALSE); }
    if (c == 'n' && startsWith("null")) { pos_ += 4; return JsonNode::newNull(); }
    return JsonNode::newNumber(parseNumber());
}

JsonNode parseObject() {
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

JsonNode parseArray() {
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

String parseString() {
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

double parseNumber() {
    char* end = NULL;
    double value = strtod(text_ + pos_, &end);
    if (end == text_ + pos_) return 0.0;
    pos_ += ((int)(end - (text_ + pos_)));
    return value;
}
};

JsonNode ObjectMapper::readTree(const String& content) const {
JsonParser parser(content.c_str());
return parser.parse();
}

