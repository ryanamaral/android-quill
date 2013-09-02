#include "org_libharu_Image.h"
#include "org_libharu_HPDF.h"
#include "hpdf.h"
#include "haru_error_handler.h"
#include <assert.h>
#include <android/log.h>  

#define DEBUG_TAG "libharu.Image"  


// Cache the field IDs
jfieldID Document_HPDF_Image_Pointer_ID;

JNIEXPORT void JNICALL Java_org_libharu_Image_initIDs
  (JNIEnv *env, jclass cls)
{
  Document_HPDF_Image_Pointer_ID = env->GetFieldID(cls, "HPDF_Image_Pointer", "I");
  if (Document_HPDF_Image_Pointer_ID == NULL) return;
}



HPDF_Image get_HPDF_Image(JNIEnv *env, jobject obj) 
{
  int ptr = env->GetIntField(obj, Document_HPDF_Image_Pointer_ID);
  return (HPDF_Image)ptr;
}

 
void set_HPDF_Image(JNIEnv *env, jobject obj, HPDF_Image ptr) 
{
  env->SetIntField(obj, Document_HPDF_Image_Pointer_ID, (int)ptr);
}


// JNI methods
JNIEXPORT void JNICALL Java_org_libharu_Image_construct
  (JNIEnv *env, jobject obj, jobject document, jstring fileName)
{
  haru_setup_error_handler(env, __func__);

  const char* file_str = env->GetStringUTFChars(fileName, 0);
  HPDF_Doc pdf = get_HPDF_Doc(env, document);
  HPDF_Image image = HPDF_LoadJpegImageFromFile(pdf, file_str);
  set_HPDF_Image(env, obj, image);

  env->ReleaseStringUTFChars(fileName, file_str);
  haru_clear_error_handler();
}

