#ifndef HARU_ERROR_HANDLER__H
#define HARU_ERROR_HANDLER__H

#include <jni.h>
#include "hpdf.h"

// HPDF calls should be wrapped into these brackets
void haru_setup_error_handler(JNIEnv *env, const char* function_name);
void haru_clear_error_handler();

// Raise a java exception with the given message
void haru_throw_exception(const char* message);

// To be used as the error handler in
void haru_error_handler(HPDF_STATUS error_no, HPDF_STATUS detail_no, void *user_data);

#endif
