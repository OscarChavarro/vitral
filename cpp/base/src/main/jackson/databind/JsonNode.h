#ifndef JACKSON_DATABIND_JSON_NODE_H
#define JACKSON_DATABIND_JSON_NODE_H

#include "java/lang/String.h"
#include "java/util/HashMap.h"
#include "java/util/ArrayList.h"

namespace jackson {
namespace databind {

class JsonNode {
public:
    enum Type { NULL_VALUE, BOOLEAN, NUMBER, STRING, ARRAY, OBJECT };

    JsonNode();

    static JsonNode newNull();
    static JsonNode newBoolean(bool value);
    static JsonNode newNumber(double value);
    static JsonNode newString(const java::String& value);
    static JsonNode newArray();
    static JsonNode newObject();

    Type type() const;
    bool isNull() const;
    bool isBoolean() const;
    bool isNumber() const;
    bool isString() const;
    bool isArray() const;
    bool isObject() const;

    bool asBoolean(bool defaultValue = false) const;
    double asDouble(double defaultValue = 0.0) const;
    int asInt(int defaultValue = 0) const;
    java::String asText(const java::String& defaultValue = java::String("")) const;

    long size() const;
    const JsonNode* get(long index) const;
    const JsonNode* get(const java::String& key) const;

    void add(const JsonNode& value);
    void put(const java::String& key, const JsonNode& value);

private:
    Type type_;
    bool boolValue_;
    double numberValue_;
    java::String stringValue_;
    java::ArrayList<JsonNode> arrayValue_;
    java::HashMap<java::String, JsonNode> objectValue_;
};

}
}

#endif
