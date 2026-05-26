#ifndef JAVA_UTIL_CONCURRENT_EXECUTIONEXCEPTION_H
#define JAVA_UTIL_CONCURRENT_EXECUTIONEXCEPTION_H

#include <stdexcept>
#include "java/lang/String.h"

namespace java {

class ExecutionException : public std::runtime_error {
public:
    explicit ExecutionException(const java::String& msg) : std::runtime_error(msg.toCString()) {}
};

}

#endif
