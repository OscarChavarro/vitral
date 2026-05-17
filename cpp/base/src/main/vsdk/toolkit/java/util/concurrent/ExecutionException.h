#ifndef JAVA_UTIL_CONCURRENT_EXECUTIONEXCEPTION_H
#define JAVA_UTIL_CONCURRENT_EXECUTIONEXCEPTION_H

#include <stdexcept>
#include <string>

namespace java {
namespace concurrent {

class ExecutionException : public std::runtime_error {
public:
    explicit ExecutionException(const std::string& msg) : std::runtime_error(msg) {}
};

}
}

#endif
