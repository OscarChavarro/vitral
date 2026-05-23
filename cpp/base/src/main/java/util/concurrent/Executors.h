#ifndef JAVA_UTIL_CONCURRENT_EXECUTORS_H
#define JAVA_UTIL_CONCURRENT_EXECUTORS_H

#include "ExecutorService.h"

namespace java {

class Executors {
public:
    static ExecutorService* newFixedThreadPool(int numberOfThreads);
};

}

#endif
