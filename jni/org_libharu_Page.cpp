#include "org_libharu_Page.h"
#include "org_libharu_HPDF.h"
#include "org_libharu_Image.h"
#include "hpdf.h"
#include "haru_error_handler.h"
#include <assert.h>
#include <android/log.h>  

#define DEBUG_TAG "libharu.Page"  

// Cache the field IDs
jfieldID Document_HPDF_Page_Pointer_ID;

JNIEXPORT void JNICALL Java_org_libharu_Page_initIDs
  (JNIEnv *env, jclass cls)
{
  haru_setup_error_handler(env, __func__);
  Document_HPDF_Page_Pointer_ID = env->GetFieldID(cls, "HPDF_Page_Pointer", "I");
  if (Document_HPDF_Page_Pointer_ID == NULL) {
    return;
  }
  haru_clear_error_handler();
}


HPDF_Page get_HPDF_Page(JNIEnv *env, jobject obj) 
{
  int ptr = env->GetIntField(obj, Document_HPDF_Page_Pointer_ID);
  return (HPDF_Page)ptr;
}

 
void set_HPDF_Page(JNIEnv *env, jobject obj, HPDF_Page ptr) 
{
  env->SetIntField(obj, Document_HPDF_Page_Pointer_ID, (int)ptr);
}

 



// JNI methods
JNIEXPORT void JNICALL Java_org_libharu_Page_construct
  (JNIEnv *env, jobject obj, jobject document)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Doc pdf = get_HPDF_Doc(env, document);
  HPDF_Page page = HPDF_AddPage(pdf); 
  set_HPDF_Page(env, obj, page);
  haru_clear_error_handler();
}

JNIEXPORT void JNICALL Java_org_libharu_Page_destruct
  (JNIEnv *, jobject)
{
  // nothing to do
}


JNIEXPORT jfloat JNICALL Java_org_libharu_Page_getHeight
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  jfloat height = HPDF_Page_GetHeight(page);
  haru_clear_error_handler();
  return height;
}


JNIEXPORT jfloat JNICALL Java_org_libharu_Page_getWidth
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  jfloat width = HPDF_Page_GetWidth(page);
  haru_clear_error_handler();
  return width;
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setLineWidth
  (JNIEnv *env, jobject obj, jfloat width)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_SetLineWidth(page, width);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setLineCap
  (JNIEnv *env, jobject obj, jobject cap)
{
  haru_setup_error_handler(env, __func__);

  jclass LineCap = env->FindClass("org/libharu/Page$LineCap");
  jmethodID getNameMethod = env->GetMethodID(LineCap, "name", "()Ljava/lang/String;");
  jstring cap_value = (jstring)env->CallObjectMethod(cap, getNameMethod);
  const char* cap_str = env->GetStringUTFChars(cap_value, 0);

  HPDF_Page page = get_HPDF_Page(env, obj); 
  if (strcmp(cap_str, "BUTT_END") == 0) {
    HPDF_Page_SetLineCap(page, HPDF_BUTT_END);
  } else if (strcmp(cap_str, "ROUND_END") == 0) {
    HPDF_Page_SetLineCap(page, HPDF_ROUND_END);
  } else if (strcmp(cap_str, "PROJECTING_SQUARE_END") == 0) {
    // Note the typo square -> scuare
    HPDF_Page_SetLineCap(page, HPDF_PROJECTING_SCUARE_END);
  } else {
    haru_throw_exception("Unsupported Cap style.");
  }

  env->ReleaseStringUTFChars(cap_value, cap_str);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setSize
    (JNIEnv *env, jobject obj, jobject sizeEnum, jobject directionEnum)
{
  haru_setup_error_handler(env, __func__);

  jclass PageSize = env->FindClass("org/libharu/Page$PageSize");
  jmethodID PageSize_getNameMethod = env->GetMethodID(PageSize, "name", "()Ljava/lang/String;");
  jstring size_value = (jstring)env->CallObjectMethod(sizeEnum, PageSize_getNameMethod);
  const char* size_str = env->GetStringUTFChars(size_value, 0);

  jclass PageDirection = env->FindClass("org/libharu/Page$PageDirection");
  jmethodID PageDirection_getNameMethod = env->GetMethodID(PageDirection, "name", "()Ljava/lang/String;");
  jstring direction_value = (jstring)env->CallObjectMethod(directionEnum, PageDirection_getNameMethod);
  const char* direction_str = env->GetStringUTFChars(direction_value, 0);

  HPDF_PageSizes size;
  if (strcmp(size_str, "LETTER") == 0)         size = HPDF_PAGE_SIZE_LETTER;
  else if (strcmp(size_str, "LEGAL") == 0)     size = HPDF_PAGE_SIZE_LEGAL;
  else if (strcmp(size_str, "A3") == 0)        size = HPDF_PAGE_SIZE_A3;
  else if (strcmp(size_str, "A4") == 0)        size = HPDF_PAGE_SIZE_A4;
  else if (strcmp(size_str, "A5") == 0)        size = HPDF_PAGE_SIZE_A5;
  else if (strcmp(size_str, "B4") == 0)        size = HPDF_PAGE_SIZE_B4;
  else if (strcmp(size_str, "B5") == 0)        size = HPDF_PAGE_SIZE_B5;
  else if (strcmp(size_str, "EXECUTIVE") == 0) size = HPDF_PAGE_SIZE_EXECUTIVE;
  else if (strcmp(size_str, "US4x6") == 0)     size = HPDF_PAGE_SIZE_US4x6;
  else if (strcmp(size_str, "US4x8") == 0)     size = HPDF_PAGE_SIZE_US4x8;
  else if (strcmp(size_str, "US5x7") == 0)     size = HPDF_PAGE_SIZE_US5x7;
  else if (strcmp(size_str, "COMM10") == 0)    size = HPDF_PAGE_SIZE_COMM10;
  else haru_throw_exception("Unsupported page size.");

  HPDF_PageDirection direction;
  if (strcmp(direction_str, "PORTRAIT") == 0)       direction = HPDF_PAGE_PORTRAIT;
  else if (strcmp(direction_str, "LANDSCAPE") == 0) direction = HPDF_PAGE_LANDSCAPE;
  else haru_throw_exception("Unsupported orientation.");

  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_SetSize(page, size, direction);

  env->ReleaseStringUTFChars(size_value, size_str);
  env->ReleaseStringUTFChars(direction_value, direction_str);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setLineJoin
  (JNIEnv *env, jobject obj, jobject join)
{
  haru_setup_error_handler(env, __func__);

  jclass LineJoin = env->FindClass("org/libharu/Page$LineJoin");
  jmethodID getNameMethod = env->GetMethodID(LineJoin, "name", "()Ljava/lang/String;");
  jstring join_value = (jstring)env->CallObjectMethod(join, getNameMethod);
  const char* join_str = env->GetStringUTFChars(join_value, 0);

  HPDF_Page page = get_HPDF_Page(env, obj); 
  if (strcmp(join_str, "MITER_JOIN") == 0)      HPDF_Page_SetLineJoin(page, HPDF_MITER_JOIN);
  else if (strcmp(join_str, "ROUND_JOIN") == 0) HPDF_Page_SetLineJoin(page, HPDF_ROUND_JOIN);
  else if (strcmp(join_str, "BEVEL_JOIN") == 0) HPDF_Page_SetLineJoin(page, HPDF_BEVEL_JOIN);
  else haru_throw_exception("Unsupported line join.");

  env->ReleaseStringUTFChars(join_value, join_str);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setMiterLimit
  (JNIEnv *env, jobject obj, jfloat lim)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_SetMiterLimit(page, lim);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_moveTo
  (JNIEnv *env, jobject obj, jfloat x, jfloat y)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_MoveTo(page, x, y);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_lineTo
  (JNIEnv *env, jobject obj, jfloat x, jfloat y)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_LineTo(page, x, y);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_curveTo
  (JNIEnv *env, jobject obj, jfloat x1, jfloat y1, jfloat x2, jfloat y2, jfloat x3, jfloat y3)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_CurveTo(page, x1, y1, x2, y2, x3, y3);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setRGBStroke
  (JNIEnv *env, jobject obj, jfloat red, jfloat green, jfloat blue)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_SetRGBStroke(page, red, green, blue);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setRGBFill
  (JNIEnv *env, jobject obj, jfloat red, jfloat green, jfloat blue)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_SetRGBFill(page, red, green, blue);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_stroke
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_Stroke(page);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_fill
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_Fill(page);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_fillStroke
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_FillStroke(page);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_image
  (JNIEnv *env, jobject obj_page, jobject obj_image, 
   jfloat x, jfloat y, jfloat width, jfloat height) 
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj_page); 
  HPDF_Image image = get_HPDF_Image(env, obj_image);
  HPDF_Page_DrawImage(page, image, x, y, width, height);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_beginText
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_BeginText (page);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_endText
  (JNIEnv *env, jobject obj)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj); 
  HPDF_Page_EndText (page);
  haru_clear_error_handler();
}


JNIEXPORT jfloat JNICALL Java_org_libharu_Page_getTextWidth
  (JNIEnv *env, jobject obj, jstring text)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj);
  const char* str = env->GetStringUTFChars(text, 0);
  float tw = HPDF_Page_TextWidth (page, str);
  env->ReleaseStringUTFChars(text, str);
  haru_clear_error_handler();
  return tw;
}


JNIEXPORT void JNICALL Java_org_libharu_Page_setFontAndSize
  (JNIEnv *env, jobject obj, jobject font, jfloat size)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj);
  HPDF_Font font_ptr = get_HPDF_Font(env, font);
  HPDF_Page_SetFontAndSize (page, font_ptr, size);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_textOut
  (JNIEnv *env, jobject obj, jfloat x, jfloat y, jstring text)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj);
  const char* str = env->GetStringUTFChars(text, 0);
  HPDF_Page_TextOut (page, x, y, str);
  env->ReleaseStringUTFChars(text, str);
  haru_clear_error_handler();
}


JNIEXPORT void JNICALL Java_org_libharu_Page_moveTextPos
  (JNIEnv *env, jobject obj, jfloat x, jfloat y)
{
  haru_setup_error_handler(env, __func__);
  HPDF_Page page = get_HPDF_Page(env, obj);
  HPDF_Page_MoveTextPos (page, x, y);
  haru_clear_error_handler();
}

