/*=========================================================================*/
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#include "compress.h"

/* This could make the library not thread safe! */
static JNIEnv *GLOBAL_env;
static jmethodID GLOBAL_javaWriteMethod;
static jmethodID GLOBAL_javaReadMethod;
static jmethodID GLOBAL_javaAvailableMethod;
static jbyteArray GLOBAL_arrRead;
static int LAST_arrReadSize = -1;
static jbyteArray GLOBAL_arrWrite;
static int LAST_arrWriteSize = -1;

size_t
myfread(void *data, size_t size, size_t n, void *descriptor)
{
/*
    static FILE *fd;
    static int ft = 1;
    size_t r;

    if ( ft ) {
        fd = fopen("./src/main.java", "rb");
        ft = 0;
    }
    r = fread(data, size, n, fd);
    return r;
*/

    jboolean eof = 0;
    jobject *is = (jobject*)(descriptor);
    static jboolean lastFlag = 1; // Not very clear why those are needed
    static jboolean flag = 1;

    if ( eof ) {
        return -1;
    }

    if ( n*size != LAST_arrReadSize ) {
        if ( LAST_arrReadSize > 0 ) {
            (*GLOBAL_env)->DeleteLocalRef(GLOBAL_env, GLOBAL_arrRead);
        }
        GLOBAL_arrRead = (jbyteArray)(*GLOBAL_env)->NewByteArray(GLOBAL_env, size*n);
        LAST_arrReadSize = n*size;
    }

    int nn = (*GLOBAL_env)->CallIntMethod(GLOBAL_env, *is, GLOBAL_javaReadMethod, GLOBAL_arrRead);
    (*GLOBAL_env)->GetByteArrayRegion(GLOBAL_env, GLOBAL_arrRead, 0, size*n, (jbyte *)data);

    // Not very clear. Flag sometimes gives false, when it should be true.
    // It has been observed that this never gets twice false...
    flag = (*GLOBAL_env)->CallBooleanMethod(GLOBAL_env, *is, GLOBAL_javaAvailableMethod);

    if ( !flag && !lastFlag  ) {
        eof = 1;
        return 0;
    }
    lastFlag = flag;

    if ( n != nn ) {
        eof = 1;
    }

    /*
    static int acum;
    acum+=nn;
    printf("[%d] fread(%d, %d), nn: %d-> %c\n", acum, size, n, nn, ((jbyte*)data)[0]);
    fflush(stdout);
    */

    return nn;
}

size_t
myfwrite(void *data, size_t size, size_t n, void *descriptor)
{
    jobject *os = (jobject*)(descriptor);
    int i;

    if ( n*size != LAST_arrWriteSize ) {
        if ( LAST_arrWriteSize > 0 ) {
            (*GLOBAL_env)->DeleteLocalRef(GLOBAL_env, GLOBAL_arrWrite);
        }
        GLOBAL_arrWrite = (jbyteArray)(*GLOBAL_env)->NewByteArray(GLOBAL_env, size*n);
        LAST_arrWriteSize = n*size;
    }

    (*GLOBAL_env)->SetByteArrayRegion(GLOBAL_env, GLOBAL_arrWrite, 0, size*n, data);
    for ( i = 0; i < n; i++ ) {
        (*GLOBAL_env)->CallVoidMethod(GLOBAL_env, *os, GLOBAL_javaWriteMethod, GLOBAL_arrWrite);
    }

    return n;
}

void
myflush(void *descriptor)
{
    LAST_arrReadSize = -1;
    LAST_arrWriteSize = -1;
    (*GLOBAL_env)->DeleteLocalRef(GLOBAL_env, GLOBAL_arrRead);
    (*GLOBAL_env)->DeleteLocalRef(GLOBAL_env, GLOBAL_arrWrite);
}


#ifdef __cplusplus
extern "C" 
#endif
/*
 * Class:     LzwWrapper
 * Method:    decompress
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_vsdk_toolkit_processing_LzwWrapper_decompress
(JNIEnv *env, jclass cl, jobject is, jobject os)
{
    GLOBAL_env = env;

    jclass class1;
    jclass class2;

    class1 = (*env)->GetObjectClass(env, os);
    GLOBAL_javaWriteMethod = (*env)->GetMethodID(env, class1, "write", "([B)V");

    class2 = (*env)->GetObjectClass(env, is);
    GLOBAL_javaReadMethod = (*env)->GetMethodID(env, class2, "read", "([B)I");
    GLOBAL_javaAvailableMethod = (*env)->GetMethodID(env, class2, "available", "()I");

    initLZW(-1, myfread, myfwrite, myflush);
    decompress(&is, &os);
}

#ifdef __cplusplus
extern "C" 
#endif
/*
 * Class:     LzwWrapper
 * Method:    decompress
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_vsdk_toolkit_processing_LzwWrapper_compress
(JNIEnv *env, jclass cl, jobject is, jobject os)
{
    GLOBAL_env = env;

    jclass class1;
    jclass class2;

    class1 = (*env)->GetObjectClass(env, os);
    GLOBAL_javaWriteMethod = (*env)->GetMethodID(env, class1, "write", "([B)V");

    class2 = (*env)->GetObjectClass(env, is);
    GLOBAL_javaReadMethod = (*env)->GetMethodID(env, class2, "read", "([B)I");
    GLOBAL_javaAvailableMethod = (*env)->GetMethodID(env, class2, "available", "()I");

    initLZW(-1, myfread, myfwrite, myflush);
    compress(&is, &os);
}

/*=========================================================================
= EOF                                                                     =
=========================================================================*/
