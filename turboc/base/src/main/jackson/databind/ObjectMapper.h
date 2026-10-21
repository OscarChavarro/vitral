#ifndef __OBJECT_MAPPER__
#define __OBJECT_MAPPER__

#include "java/lang/String.h"
#include "jackson/databind/JsonNode.h"

class ObjectMapper {
public:
    JsonNode readTree(const String& content) const;
};


#endif
