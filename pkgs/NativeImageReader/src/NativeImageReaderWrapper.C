#include <jni.h>
#include "NativeImageReader.h"

/*
 * Class:     vsdk_toolkit_io_image_NativeImageReaderWrapper
 * Method:    readPngHeader
 * Signature: (Lvsdk/toolkit/io/image/_NativeImageReaderWrapperHeaderInfo;Ljava/lang/String;)V
 */
extern "C" 
JNIEXPORT void JNICALL Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_readPngHeader
  (JNIEnv *Env, jclass Class, jobject Header, jstring Filename)
{
    jboolean iscopy;
    const char *mfilename = Env->GetStringUTFChars(Filename, &iscopy);

    jclass headerClass = Env->GetObjectClass(Header);

    _NativeImageReaderHeaderInfo *NativeHeader;
    
    FILE *fd;
    fd = fopen(mfilename, "rb");

    jfieldID xSizeId = Env->GetFieldID(headerClass, "xSize", "J");
    jfieldID ySizeId = Env->GetFieldID(headerClass, "ySize", "J");
    jfieldID channelsId = Env->GetFieldID(headerClass, "channels", "J");
    jfieldID nativePointerId = Env->GetFieldID(headerClass, "nativePointer", "J");
    if ( !fd ) {
        fprintf(stderr, "<NativeImageReaderWrapper> Error: Can not open file [%s].\n", mfilename);
	fflush(stderr);
	NativeHeader = NULL;
    }
    else {
        NativeHeader = readPngHeader(fd);
        NativeHeader->fd = fd;
        Env->SetLongField(Header, xSizeId, (jlong)NativeHeader->xSize);
        Env->SetLongField(Header, ySizeId, (jlong)NativeHeader->ySize);
        Env->SetLongField(Header, channelsId, (jlong)NativeHeader->channels);
    }

    // Warning: only tested on 64bit environments!
    unsigned long dir = (unsigned long)NativeHeader;

    Env->SetLongField(Header, nativePointerId, (jlong)dir);

    //dir = 0x1122334455667788L;

/*
    printf("Native structure pointer: 0x%08X-%08X\n",
           (unsigned int)((dir&0xFFFFFFFF00000000)>>32),
           (unsigned int)(dir&0x00000000FFFFFFFF)
          );
    fflush(stdout);
*/
}

/*
 * Class:     vsdk_toolkit_io_image_NativeImageReaderWrapper
 * Method:    readPngDataRGB
 * Signature: (Lvsdk/toolkit/io/image/_NativeImageReaderWrapperHeaderInfo;Ljava/nio/ByteBuffer;)V
 */
extern "C" JNIEXPORT void JNICALL
Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_readPngDataRGB
    (JNIEnv *Env, jclass Class, jobject Header, jobject Buffer)
{

    _NativeImageReaderHeaderInfo *NativeHeader;
    jclass headerClass = Env->GetObjectClass(Header);
    jfieldID nativePointerId = Env->GetFieldID(headerClass, "nativePointer", "J");
    unsigned long dir;

    dir = (unsigned long)Env->GetLongField(Header, nativePointerId);

/*
    printf("Native structure pointer: 0x%08X-%08X\n",
           (unsigned int)((dir&0xFFFFFFFF00000000)>>32),
           (unsigned int)(dir&0x00000000FFFFFFFF)
          );
    fflush(stdout);
*/

    if ( dir == 0 ) {
        return;
    }

    NativeHeader = (_NativeImageReaderHeaderInfo *)dir;
    readPngDataRGB(NativeHeader, NativeHeader->fd, (BYTE*)Env->GetDirectBufferAddress(Buffer), TRUE);
    fclose(NativeHeader->fd);
    delete NativeHeader;
}

/*
 * Class:     vsdk_toolkit_io_image_NativeImageReaderWrapper
 * Method:    readPngDataRGBA
 * Signature: (Lvsdk/toolkit/io/image/_NativeImageReaderWrapperHeaderInfo;Ljava/nio/ByteBuffer;)V
 */
extern "C" JNIEXPORT void JNICALL
Java_vsdk_toolkit_io_image_NativeImageReaderWrapper_readPngDataRGBA
    (JNIEnv *Env, jclass Class, jobject Header, jobject Buffer)
{
    _NativeImageReaderHeaderInfo *NativeHeader;
    jclass headerClass = Env->GetObjectClass(Header);
    jfieldID nativePointerId = Env->GetFieldID(headerClass, "nativePointer", "J");
    unsigned long dir;

    dir = (unsigned long)Env->GetLongField(Header, nativePointerId);

/*
    printf("Native structure pointer: 0x%08X-%08X\n",
           (unsigned int)((dir&0xFFFFFFFF00000000)>>32),
           (unsigned int)(dir&0x00000000FFFFFFFF)
          );
    fflush(stdout);
*/

    if ( dir == 0 ) {
        return;
    }

    NativeHeader = (_NativeImageReaderHeaderInfo *)dir;
    readPngDataRGBA(NativeHeader, NativeHeader->fd, (BYTE*)Env->GetDirectBufferAddress(Buffer), TRUE);
    fclose(NativeHeader->fd);
    delete NativeHeader;
}
