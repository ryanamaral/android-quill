#include "org_libharu_Document.h"
#include "org_libharu_HPDF.h"
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
  HPDF_Doc pdf = HPDF_New(NULL, NULL); 
  set_HPDF_Doc(env, obj, pdf);
}

JNIEXPORT void JNICALL Java_org_libharu_Document_destruct
  (JNIEnv *env, jobject obj)
{
  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_Free(pdf);
}


JNIEXPORT void JNICALL Java_org_libharu_Document_setCompressionMode
  (JNIEnv *env, jobject obj, jobject compression)
{
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
  else assert(false);

  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_SetCompressionMode (pdf, mode);
}


JNIEXPORT void JNICALL Java_org_libharu_Document_setPassword
  (JNIEnv *env, jobject obj, jstring jownerpass, jstring juserpass)
{
  const char *ownerpass = (char*)env->GetStringUTFChars(jownerpass, NULL);
  if (ownerpass == NULL) return;
  const char *userpass = (char*)env->GetStringUTFChars(juserpass, NULL);
  if (userpass == NULL) return;

  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_SetPassword(pdf, ownerpass, userpass);

  env->ReleaseStringUTFChars(jownerpass, ownerpass);
  env->ReleaseStringUTFChars(juserpass, userpass);
}


JNIEXPORT void JNICALL Java_org_libharu_Document_saveToFile
  (JNIEnv *env, jobject obj, jstring filename)
{
  const char *str = (char*)env->GetStringUTFChars(filename, NULL);
  if (str == NULL) return;

  HPDF_Doc pdf = get_HPDF_Doc(env, obj);
  HPDF_STATUS rc = HPDF_SaveToFile(pdf, str);

  env->ReleaseStringUTFChars(filename, str);
  if (rc == HPDF_OK) return;

  jclass excCls = env->FindClass("java/io/IOException");
  if (excCls == NULL) return;
  if (rc == HPDF_INVALID_DOCUMENT)
    env->ThrowNew(excCls, "An invalid document handle is set.");
  else if (rc == HPDF_FAILD_TO_ALLOC_MEM)
    env->ThrowNew(excCls, "Memory allocation failed.");
  else if (rc == HPDF_FILE_IO_ERROR)
    env->ThrowNew(excCls, "An error occurred while processing file I/O.");
  else 
    env->ThrowNew(excCls, "Unknown return code.");
}
