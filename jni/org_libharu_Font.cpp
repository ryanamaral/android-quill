#include "org_libharu_Font.h"
#include "org_libharu_HPDF.h"
#include "hpdf.h"
#include "haru_error_handler.h"
#include <assert.h>
#include <android/log.h>  

#define DEBUG_TAG "libharu.Font"  


// Cache the field IDs
jfieldID Document_HPDF_Font_Pointer_ID;

JNIEXPORT void JNICALL Java_org_libharu_Font_initIDs
  (JNIEnv *env, jclass cls)
{
  Document_HPDF_Font_Pointer_ID = env->GetFieldID(cls, "HPDF_Font_Pointer", "I");
  if (Document_HPDF_Font_Pointer_ID == NULL) {
    return;
  }
}


HPDF_Font get_HPDF_Font(JNIEnv *env, jobject obj) 
{
  int ptr = env->GetIntField(obj, Document_HPDF_Font_Pointer_ID);
  return (HPDF_Font)ptr;
}

 
void set_HPDF_Font(JNIEnv *env, jobject obj, HPDF_Font ptr) 
{
  env->SetIntField(obj, Document_HPDF_Font_Pointer_ID, (int)ptr);
}




// JNI methods
JNIEXPORT void JNICALL Java_org_libharu_Font_construct
  (JNIEnv *env, jobject obj, jobject document, jobject fontEnum, jstring encodingName)
{
  haru_setup_error_handler(env, __func__);

  jclass BuiltinFont = env->FindClass("org/libharu/Font$BuiltinFont");
  jmethodID getNameMethod = env->GetMethodID(BuiltinFont, "name", "()Ljava/lang/String;");
  jstring builtin_value = (jstring)env->CallObjectMethod(fontEnum, getNameMethod);
  const char* builtin_str = env->GetStringUTFChars(builtin_value, 0);
  const char* font_str;
  if (strcmp(builtin_str, "COURIER") == 0)                     font_str = "Courier";
  else if (strcmp(builtin_str, "COURIER_BOLD") == 0)           font_str = "Courier-Bold";
  else if (strcmp(builtin_str, "COURIER_OBLIQUE") == 0)        font_str = "Courier-Oblique";
  else if (strcmp(builtin_str, "COURIER_BOLD_OBLIQUE") == 0)   font_str = "Courier-BoldOblique";
  else if (strcmp(builtin_str, "HELVETICA") == 0)              font_str = "Helvetica";
  else if (strcmp(builtin_str, "HELVETICA_BOLD") == 0)         font_str = "Helvetica-Bold";
  else if (strcmp(builtin_str, "HELVETICA_OBLIQUE") == 0)      font_str = "Helvetica-Oblique";
  else if (strcmp(builtin_str, "HELVETICA_BOLD_OBLIQUE") == 0) font_str = "Helvetica-BoldOblique";
  else if (strcmp(builtin_str, "TIMES_ROMAN") == 0)            font_str = "Times-Roman";
  else if (strcmp(builtin_str, "TIMES_BOLD") == 0)             font_str = "Times-Bold";
  else if (strcmp(builtin_str, "TIMES_ITALIC") == 0)           font_str = "Times-Italic";
  else if (strcmp(builtin_str, "TIMES_BOLD_ITALIC") == 0)      font_str = "Times-BoldItalic";
  else if (strcmp(builtin_str, "SYMBOL") == 0)                 font_str = "Symbol";
  else if (strcmp(builtin_str, "ZAPFDINGBATS") == 0)           font_str = "ZapfDingbats";
  else haru_throw_exception("Unknown font.");
  env->ReleaseStringUTFChars(builtin_value, builtin_str);

  const char* encoding_str = env->GetStringUTFChars(encodingName, 0);
  HPDF_Doc pdf = get_HPDF_Doc(env, document);
  HPDF_Font font = HPDF_GetFont(pdf, font_str, encoding_str);
  set_HPDF_Font(env, obj, font);

  env->ReleaseStringUTFChars(encodingName, encoding_str);
  haru_clear_error_handler();
}

