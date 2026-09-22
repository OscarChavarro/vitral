//===========================================================================
//= JNI binding for the NativeImageReader library.                          =
//=                                                                         =
//= The method signatures implemented here are not written by hand: they    =
//= come from the `vsdk_toolkit_io_image_NativeImageReaderWrapper.h` header, =
//= which the build generates with `javac -h` from the Java class (the      =
//= `javah` tool used by the old Makefile was removed in JDK 10).           =
//===========================================================================

#include <jni.h>
#include <stdint.h>
#include <stdio.h>

#include "vsdk_toolkit_io_image_NativeImageReaderWrapper.h"
#include "NativeImageReader.h"

//===========================================================================

static void
throwIoException(JNIEnv *Env, const char *message)
{
    jclass exceptionClass = Env->FindClass("java/io/IOException");

    if ( exceptionClass != NULL ) {
        Env->ThrowNew(exceptionClass, message);
    }
}

/**
Obtains the native reading context stored by `readPngHeader` inside the Java
side header object. Returns NULL (and leaves a pending Java exception) if
there is no valid context.
*/
static _NativeImageReaderHeaderInfo *
extractNativeHeader(JNIEnv *Env, jobject Header)
{
    jclass headerClass = Env->GetObjectClass(Header);
    jfieldID nativePointerId = Env->GetFieldID(headerClass, "nativePointer", "J");

    if ( nativePointerId == NULL ) {
        return NULL;
    }

    jlong nativePointer = Env->GetLongField(Header, nativePointerId);

    if ( nativePointer == 0 ) {
        throwIoException(Env, "NativeImageReader: png header not read, or "
                         "image data already consumed");
        return NULL;
    }

    return (_NativeImageReaderHeaderInfo *)(intptr_t)nativePointer;
}

static void
clearNativeHeader(JNIEnv *Env, jobject Header)
{
    jclass headerClass = Env->GetObjectClass(Header);
    jfieldID nativePointerId = Env->GetFieldID(headerClass, "nativePointer", "J");

    if ( nativePointerId != NULL ) {
        Env->SetLongField(Header, nativePointerId, (jlong)0);
    }
}

/**
Common implementation for the RGB and RGBA data reading native methods.
Note that the reading context is always released, either on success or on
error, so no native memory is leaked if the Java side abandons the image.
*/
static void
readData(JNIEnv *Env, jobject Header, jobject Buffer, BOOLEAN rgba)
{
    _NativeImageReaderHeaderInfo *NativeHeader;

    NativeHeader = extractNativeHeader(Env, Header);

    if ( NativeHeader == NULL ) {
        return;
    }

    clearNativeHeader(Env, Header);

    BYTE *arr = (BYTE *)Env->GetDirectBufferAddress(Buffer);
    jlong capacity = Env->GetDirectBufferCapacity(Buffer);
    jlong needed = (jlong)NativeHeader->xSize * (jlong)NativeHeader->ySize *
                   (rgba ? 4 : 3);

    if ( arr == NULL ) {
        releasePngHeader(NativeHeader);
        throwIoException(Env, "NativeImageReader: a direct java.nio.ByteBuffer "
                         "is needed to receive the image data");
        return;
    }

    if ( capacity < needed ) {
        releasePngHeader(NativeHeader);
        throwIoException(Env, "NativeImageReader: destination buffer is too "
                         "small for the image being read");
        return;
    }

    BOOLEAN ok;

    if ( rgba ) {
        ok = readPngDataRGBA(NativeHeader, arr, TRUE);
    }
    else {
        ok = readPngDataRGB(NativeHeader, arr, TRUE);
    }

    if ( !ok ) {
        char message[NATIVE_IMAGE_READER_MESSAGE_SIZE + 64];
        snprintf(message, sizeof(message), "NativeImageReader: %s",
                 NativeHeader->errorMessage[0] != '\0' ?
                     NativeHeader->errorMessage : "png decoding failed");
        releasePngHeader(NativeHeader);
        throwIoException(Env, message);
        return;
    }

    releasePngHeader(NativeHeader);
}

//===========================================================================

/**
ABI version implemented by this binding. It must be kept in sync with
`NativeImageReaderWrapper.REQUIRED_API_VERSION` on the Java side: the Java
class refuses a library reporting a different value, so an obsolete library
installed in a system directory is never used.
*/
#define NATIVE_IMAGE_READER_API_VERSION 1

extern "C"
JNIEXPORT jint JNICALL
Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_getApiVersion
  (JNIEnv *Env, jclass Class)
{
    return (jint)NATIVE_IMAGE_READER_API_VERSION;
}

extern "C"
JNIEXPORT void JNICALL
Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_readPngHeader
  (JNIEnv *Env, jclass Class, jobject Header, jstring Filename)
{
    const char *mfilename = Env->GetStringUTFChars(Filename, NULL);

    if ( mfilename == NULL ) {
        return;
    }

    FILE *fd = fopen(mfilename, "rb");

    if ( !fd ) {
        char message[NATIVE_IMAGE_READER_MESSAGE_SIZE];
        snprintf(message, sizeof(message),
                 "NativeImageReader: can not open file [%s]", mfilename);
        Env->ReleaseStringUTFChars(Filename, mfilename);
        throwIoException(Env, message);
        return;
    }

    _NativeImageReaderHeaderInfo *NativeHeader = readPngHeader(fd);

    if ( NativeHeader == NULL ) {
        char message[NATIVE_IMAGE_READER_MESSAGE_SIZE];
        snprintf(message, sizeof(message),
                 "NativeImageReader: file [%s] is not a readable png image",
                 mfilename);
        Env->ReleaseStringUTFChars(Filename, mfilename);
        throwIoException(Env, message);
        return;
    }

    Env->ReleaseStringUTFChars(Filename, mfilename);

    jclass headerClass = Env->GetObjectClass(Header);
    jfieldID xSizeId = Env->GetFieldID(headerClass, "xSize", "J");
    jfieldID ySizeId = Env->GetFieldID(headerClass, "ySize", "J");
    jfieldID channelsId = Env->GetFieldID(headerClass, "channels", "J");
    jfieldID nativePointerId = Env->GetFieldID(headerClass, "nativePointer", "J");

    if ( xSizeId == NULL || ySizeId == NULL || channelsId == NULL ||
         nativePointerId == NULL ) {
        releasePngHeader(NativeHeader);
        return;
    }

    Env->SetLongField(Header, xSizeId, (jlong)NativeHeader->xSize);
    Env->SetLongField(Header, ySizeId, (jlong)NativeHeader->ySize);
    Env->SetLongField(Header, channelsId, (jlong)NativeHeader->channels);
    Env->SetLongField(Header, nativePointerId, (jlong)(intptr_t)NativeHeader);
}

extern "C"
JNIEXPORT void JNICALL
Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_readPngDataRGB
  (JNIEnv *Env, jclass Class, jobject Header, jobject Buffer)
{
    readData(Env, Header, Buffer, FALSE);
}

extern "C"
JNIEXPORT void JNICALL
Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_readPngDataRGBA
  (JNIEnv *Env, jclass Class, jobject Header, jobject Buffer)
{
    readData(Env, Header, Buffer, TRUE);
}

extern "C"
JNIEXPORT void JNICALL
Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_releasePngHeader
  (JNIEnv *Env, jclass Class, jobject Header)
{
    jclass headerClass = Env->GetObjectClass(Header);
    jfieldID nativePointerId = Env->GetFieldID(headerClass, "nativePointer", "J");

    if ( nativePointerId == NULL ) {
        return;
    }

    jlong nativePointer = Env->GetLongField(Header, nativePointerId);

    if ( nativePointer == 0 ) {
        return;
    }

    Env->SetLongField(Header, nativePointerId, (jlong)0);
    releasePngHeader((_NativeImageReaderHeaderInfo *)(intptr_t)nativePointer);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
