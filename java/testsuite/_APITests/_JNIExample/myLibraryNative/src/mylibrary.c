#include "mylibrary.h"
#include "MyNativeInterface.h"

/*extern "C"*/
JNIEXPORT jint JNICALL Java_MyNativeInterface_myfunc
  (JNIEnv *e, jobject o)
{
	return myfunc();
}

int myfunc()
{
	return 666;
}
