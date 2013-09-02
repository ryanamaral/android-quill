#include "org_libharu_Document.h"
#include "org_libharu_HPDF.h"
#include "haru_error_handler.h"
#include <assert.h>

jfieldID Document_HPDF_Doc_Pointer_ID;

JNIEXPORT void JNICALL Java_org_libharu_Document_initIDs
  (JNIEnv *env, jclass cls)
{
  Document_HPDF_Doc_Pointer_ID = env->GetFieldID(cls, "HPDF_Doc_Pointer", "I");
  if (Document_HPDF_Doc_Pointer_ID == NULL) {
    return;
  }
}


HPDF_Doc get_HPDF_Doc(JNIEnv *env, jobject obj) 
{
  int ptr = env->GetIntField(obj, Document_HPDF_Doc_Pointer_ID);
  return (HPDF_Doc)ptr;
}

 
void set_HPDF_Doc(JNIEnv *env, jobject obj, HPDF_Doc ptr) 
{
  env->SetIntField(obj, Document_HPDF_Doc_Pointer_ID, (int)ptr);
}

 
JNIEXPORT void JNICALL Java_org_libharu_Document_construct
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Doc pdf = HPDF_New(haru_error_handler, NULL);
  set_HPDF_Doc(env, obj, pdf);
  if (pdf == NULL)
    haru_throw_exception("Failed to create new PDF document.");
  haru_clear_error_handler();
}

JNIEXPORT void JNICALL Java_org_libharu_Document_destruct
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_Free(pdf);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Document_setCompressionMode
  (JNIEnv *env, jobject obj, jobject compression)
{
  haru_setup_error_handler(env, __func__);
  jclass CompressionMode = env->FindClass("org/libharu/Document$CompressionMode");
  jmethodID getNameMethod = env->GetMethodID(CompressionMode, "name", "()Ljava/lang/String;");
  jstring comp_value = (jstring)env->CallObjectMethod(compression, getNameMethod);
  const char* comp_str = env->GetStringUTFChars(comp_value, 0);

  HPDF_UINT mode;
  if (strcmp(comp_str, "COMP_NONE") == 0)          mode = HPDF_COMP_NONE;
  else if (strcmp(comp_str, "COMP_TEXT") == 0)     mode = HPDF_COMP_TEXT; 
  else if (strcmp(comp_str, "COMP_IMAGE") == 0)    mode = HPDF_COMP_IMAGE; 
  else if (strcmp(comp_str, "COMP_METADATA") == 0) mode = HPDF_COMP_METADATA;
  else if (strcmp(comp_str, "COMP_ALL") == 0)      mode = HPDF_COMP_ALL;
  else haru_throw_exception("Unknown compression mode.");

  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_SetCompressionMode (pdf, mode);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Document_setPassword
  (JNIEnv *env, jobject obj, jstring jownerpass, jstring juserpass)
{
  haru_setup_error_handler(env, __func__);
  const char *ownerpass = (char*)env->GetStringUTFChars(jownerpass, NULL);
  if (ownerpass == NULL) return;
  const char *userpass = (char*)env->GetStringUTFChars(juserpass, NULL);
  if (userpass == NULL) return;

  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_SetPassword(pdf, ownerpass, userpass);

  env->ReleaseStringUTFChars(jownerpass, ownerpass);
  env->ReleaseStringUTFChars(juserpass, userpass);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Document_saveToFile
  (JNIEnv *env, jobject obj, jstring filename)
{
  haru_setup_error_handler(env, __func__);
  const char *str = (char*)env->GetStringUTFChars(filename, NULL);
  if (str == NULL) return;

  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_STATUS rc = HPDF_SaveToFile(pdf, str);

  env->ReleaseStringUTFChars(filename, str);
  if (rc == HPDF_OK)
    ;
  else if (rc == HPDF_INVALID_DOCUMENT)
    haru_throw_exception("An invalid document handle is set.");
  else if (rc == HPDF_FAILD_TO_ALLOC_MEM)
    haru_throw_exception("Memory allocation failed.");
  else if (rc == HPDF_FILE_IO_ERROR)
    haru_throw_exception("An error occurred while processing file I/O.");
  else 
    haru_throw_exception("Unknown return code.");
  haru_clear_error_handler();
}
