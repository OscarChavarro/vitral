#ifndef __VSDK_EXCEPTION__
#define __VSDK_EXCEPTION__

#include <exception>

#include "java/lang/String.h"

/**
The VSDKException abstract class provides an interface for *Exception
style classes inside the Vitral SDK. This serves two purposes:
  - To help in design level organization of exceptions (this eases the
    study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all exceptions (but none of these as been detected yet)
*/
class VSDKException : public std::exception {
private:
    String message;

protected:
    VSDKException() : message("") {}
    explicit VSDKException(const String& message) : message(message) {}

public:
    virtual ~VSDKException() throw() {}

    /**
    @return the message given at construction time
    */
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
