#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "jackson/databind/JsonNode.h"

/**
turboc port note: grows by 4 elements, not by the default chunk of 100, as
each JSON array would take 100 nodes at once and the GUI files (166 arrays in
english.json) do not fit in the 640 KB of real mode.
*/
class JsonNodeArrayStorage : public ArrayList<JsonNode> {
public:
    JsonNodeArrayStorage() : ArrayList<JsonNode>(4) {}
    JsonNodeArrayStorage(const JsonNodeArrayStorage& other) : ArrayList<JsonNode>(other) {}
};

/**
turboc port note: the storages declare their copy constructors, as the ones
that Turbo C++ 3.0 synthesizes leave the copies empty. Objects start with 4
buckets, not 16 (most have up to 4 keys, and the map rehashes as it grows), to
fit the GUI files in real mode.
*/
class JsonNodeObjectStorage : public HashMap<String, JsonNode> {
public:
    JsonNodeObjectStorage() : HashMap<String, JsonNode>(4) {}
    JsonNodeObjectStorage(const JsonNodeObjectStorage& other) : HashMap<String, JsonNode>(other) {}
};

JsonNode::JsonNode()
    : type_(NULL_VALUE)
    , boolValue_(FALSE)
    , numberValue_(0.0)
    , stringValue_("")
    , arrayValue_(NULL)
    , objectValue_(NULL) {}

JsonNode::JsonNode(const JsonNode& other)
    : type_(NULL_VALUE)
    , boolValue_(FALSE)
    , numberValue_(0.0)
    , stringValue_("")
    , arrayValue_(NULL)
    , objectValue_(NULL) {
    copyFrom(other);
}

JsonNode&
JsonNode::operator=(const JsonNode& other) {
    if (this != &other) {
        copyFrom(other);
    }
    return *this;
}

JsonNode::~JsonNode() {
    disposeStorage();
}

JsonNode JsonNode::newNull() { return JsonNode(); }
JsonNode JsonNode::newBoolean(BOOLEAN value) { JsonNode n; n.type_ = BOOLEAN_VALUE; n.boolValue_ = value; return n; }
JsonNode JsonNode::newNumber(double value) { JsonNode n; n.type_ = NUMBER; n.numberValue_ = value; return n; }
JsonNode JsonNode::newString(const String& value) { JsonNode n; n.type_ = STRING; n.stringValue_ = value; return n; }
JsonNode JsonNode::newArray() { JsonNode n; n.type_ = ARRAY; return n; }
JsonNode JsonNode::newObject() { JsonNode n; n.type_ = OBJECT; return n; }

JsonNode::Type JsonNode::type() const { return type_; }
BOOLEAN JsonNode::isNull() const { return type_ == NULL_VALUE; }
BOOLEAN JsonNode::isBoolean() const { return type_ == BOOLEAN_VALUE; }
BOOLEAN JsonNode::isNumber() const { return type_ == NUMBER; }
BOOLEAN JsonNode::isString() const { return type_ == STRING; }
BOOLEAN JsonNode::isArray() const { return type_ == ARRAY; }
BOOLEAN JsonNode::isObject() const { return type_ == OBJECT; }

BOOLEAN JsonNode::asBoolean(BOOLEAN defaultValue) const { return isBoolean() ? boolValue_ : defaultValue; }
double JsonNode::asDouble(double defaultValue) const { return isNumber() ? numberValue_ : defaultValue; }
int JsonNode::asInt(int defaultValue) const { return isNumber() ? ((int)(numberValue_)) : defaultValue; }
String JsonNode::asText(const String& defaultValue) const { return isString() ? stringValue_ : defaultValue; }

long JsonNode::size() const {
    if (isArray() && arrayValue_ != NULL) return ((long)(arrayValue_->size()));
    if (isObject() && objectValue_ != NULL) return ((long)(objectValue_->size()));
    return 0;
}

const JsonNode* JsonNode::get(long index) const {
    if (!isArray() || arrayValue_ == NULL) return NULL;
    if (index < 0 || index >= arrayValue_->size()) return NULL;
    return &(*arrayValue_)[index];
}

const JsonNode* JsonNode::get(const String& key) const {
    if (!isObject() || objectValue_ == NULL) return NULL;
    return objectValue_->get(key);
}

void JsonNode::add(const JsonNode& value) {
    if (!isArray()) return;
    ensureArrayStorage()->add(value);
}

void JsonNode::put(const String& key, const JsonNode& value) {
    if (!isObject()) return;
    ensureObjectStorage()->put(key, value);
}

void
JsonNode::copyFrom(const JsonNode& other) {
    if (this == &other) {
        return;
    }

    disposeStorage();
    type_ = other.type_;
    boolValue_ = other.boolValue_;
    numberValue_ = other.numberValue_;
    stringValue_ = other.stringValue_;

    if (other.arrayValue_ != NULL) {
        arrayValue_ = new ArrayStorage(*other.arrayValue_);
    }
    if (other.objectValue_ != NULL) {
        objectValue_ = new ObjectStorage(*other.objectValue_);
    }
}

void
JsonNode::disposeStorage() {
    if (arrayValue_ != NULL) {
        delete arrayValue_;
        arrayValue_ = NULL;
    }
    if (objectValue_ != NULL) {
        delete objectValue_;
        objectValue_ = NULL;
    }
}

JsonNode::ArrayStorage*
JsonNode::ensureArrayStorage() {
    if (arrayValue_ == NULL) {
        arrayValue_ = new ArrayStorage();
    }
    return arrayValue_;
}

JsonNode::ObjectStorage*
JsonNode::ensureObjectStorage() {
    if (objectValue_ == NULL) {
        objectValue_ = new ObjectStorage();
    }
    return objectValue_;
}

