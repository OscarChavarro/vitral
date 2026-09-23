#ifndef __NUMBER_FORMAT_EXCEPTION__
#define __NUMBER_FORMAT_EXCEPTION__

#include <exception>

#include "java/lang/String.h"

namespace java {

/**
Emulation of `java.lang.NumberFormatException`, thrown by the parse methods
of `Integer` and `Double`.
*/
class NumberFormatException : public std::exception {
private:
    java::String message;

public:
    explicit NumberFormatException(const java::String& message)
        : message(message) {}

    const java::String& getMessage() const
    {
        return message;
    }

    const char* what() const noexcept override
    {
        return message.c_str();
    }
};

}

#endif
