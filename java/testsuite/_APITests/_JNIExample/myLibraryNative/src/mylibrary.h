#ifndef __MYLIBRARY__
#define __MYLIBRARY__

#ifdef _cplusplus
extern "C"
#endif

#ifdef WIN32
__declspec(dllexport)
#endif
 int myfunc();

#endif /* __MYLIBRARY__ */
