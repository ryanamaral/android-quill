#ifndef org_libharu_HPDF__H
#define org_libharu_HPDF__H

#include <jni.h>
#include "hpdf.h"

#ifdef __cplusplus
extern "C" {
#endif

// access to the HPDF_Page field in org.libharu.Page
HPDF_Page get_HPDF_Page(JNIEnv *env, jobject obj);
void set_HPDF_Page(JNIEnv *env, jobject obj, HPDF_Page ptr);

// access to the HPDF_Doc field in org.libharu.Document
HPDF_Doc get_HPDF_Doc(JNIEnv *env, jobject obj);
void set_HPDF_Doc(JNIEnv *env, jobject obj, HPDF_Doc ptr);

// access to the HPDF_Font field in org.libharu.Font
HPDF_Font get_HPDF_Font(JNIEnv *env, jobject obj);
void set_HPDF_Font(JNIEnv *env, jobject obj, HPDF_Font ptr);

// access to the HPDF_Image field in org.libharu.Image
HPDF_Image get_HPDF_Image(JNIEnv *env, jobject obj);
void set_HPDF_Image(JNIEnv *env, jobject obj, HPDF_Image ptr);

#ifdef __cplusplus
}
#endif
#endif
