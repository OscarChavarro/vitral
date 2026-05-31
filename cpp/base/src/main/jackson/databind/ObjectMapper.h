#ifndef JACKSON_DATABIND_OBJECT_MAPPER_H
#define JACKSON_DATABIND_OBJECT_MAPPER_H

#include "java/lang/String.h"
#include "jackson/databind/JsonNode.h"

namespace jackson {
namespace databind {

class ObjectMapper {
public:
    JsonNode readTree(const java::String& content) const;
};

}
}

#endif
