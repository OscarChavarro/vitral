#ifndef __RUNNABLE__
#define __RUNNABLE__


/**
Emulation of `java.lang.Runnable`: a task, i.e. the body of a `Thread`.
*/
class Runnable {
  public:
    virtual ~Runnable() {}
    virtual void run() = 0;
};


#endif
