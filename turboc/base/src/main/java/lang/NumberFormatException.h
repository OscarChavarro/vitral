#ifndef __NUMBER_FORMAT_EXCEPTION__
#define __NUMBER_FORMAT_EXCEPTION__

#include <exception>

#include "java/lang/String.h"


/**
Emulation of `java.lang.NumberFormatException`, thrown by the parse methods
of `Integer` and `Double`.
*/
class NumberFormatException : public std::exception {
private:
    String message;

public:
    explicit NumberFormatException(const String& message)
        : message(message) {}

    virtual ~NumberFormatException() throw() {}

    const String& getMessage() const
    {
        return message;
    }

    const char* what() const throw()
    {
        return message.c_str();
    }
};


#endif
