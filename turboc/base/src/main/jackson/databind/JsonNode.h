#ifndef __JSON_NODE__
#define __JSON_NODE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"

class JsonNode {
public:
enum Type { NULL_VALUE, BOOLEAN_VALUE, NUMBER, STRING, ARRAY, OBJECT };

JsonNode();
JsonNode(const JsonNode& other);
JsonNode& operator=(const JsonNode& other);
~JsonNode();

static JsonNode newNull();
static JsonNode newBoolean(BOOLEAN value);
static JsonNode newNumber(double value);
static JsonNode newString(const String& value);
static JsonNode newArray();
static JsonNode newObject();

Type type() const;
BOOLEAN isNull() const;
BOOLEAN isBoolean() const;
BOOLEAN isNumber() const;
BOOLEAN isString() const;
BOOLEAN isArray() const;
BOOLEAN isObject() const;

BOOLEAN asBoolean(BOOLEAN defaultValue = FALSE) const;
double asDouble(double defaultValue = 0.0) const;
int asInt(int defaultValue = 0) const;
String asText(const String& defaultValue = String("")) const;

long size() const;
const JsonNode* get(long index) const;
const JsonNode* get(const String& key) const;

void add(const JsonNode& value);
void put(const String& key, const JsonNode& value);

private:
typedef ArrayList<JsonNode> ArrayStorage;
typedef HashMap<String, JsonNode> ObjectStorage;

Type type_;
BOOLEAN boolValue_;
double numberValue_;
String stringValue_;
ArrayStorage* arrayValue_;
ObjectStorage* objectValue_;

void copyFrom(const JsonNode& other);
void disposeStorage();
ArrayStorage* ensureArrayStorage();
ObjectStorage* ensureObjectStorage();
};


#endif
