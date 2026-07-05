#ifndef __OBJECTMAPPER__
#define __OBJECTMAPPER__

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
