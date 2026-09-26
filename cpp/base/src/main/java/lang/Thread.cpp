#include <chrono>
#include <memory>
#include <mutex>

#include "java/lang/Thread.h"

namespace java {

Thread::Thread(Runnable *target) : target(target), name("Thread")
{
}

Thread::~Thread()
{
    if ( thread.joinable() ) {
        thread.detach();
    }
}

void
Thread::setName(const java::String &name)
{
    this->name = name;
}

const java::String &
Thread::getName() const
{
    return name;
}

void
Thread::start()
{
    Runnable *task = target;
    // The new thread waits until this object knows it: then its task may
    // destroy this object
    std::shared_ptr<std::mutex> started = std::make_shared<std::mutex>();
    started->lock();
    thread = std::thread([task, started]() {
        started->lock();
        started->unlock();
        if ( task != nullptr ) {
            task->run();
        }
    });
    started->unlock();
}

void
Thread::join()
{
    if ( thread.joinable() && thread.get_id() != std::this_thread::get_id() ) {
        thread.join();
    }
}

void
Thread::sleep(long long millis)
{
    std::this_thread::sleep_for(std::chrono::milliseconds(millis));
}

}
