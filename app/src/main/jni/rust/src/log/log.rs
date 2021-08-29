use std::ffi::{CStr, CString};

use jni::objects::{JObject, JValue};
use jni::signature::JavaType;
use jni::strings::JNIString;
use jni::JNIEnv;

pub fn toast(env: JNIEnv, context: JObject, content: &str) -> jni::errors::Result<()> {
    let content = env.new_string(content)?;

    let class = env.find_class("android/widget/Toast")?;
    let result = env.call_static_method(
        class,
        "makeText",
        "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;",
        &[
            JValue::Object(context),
            JValue::Object(content.into()),
            JValue::Int(0),
        ],
    )?;
    let toast = result.l()?;
    env.call_method(toast, "show", "()V", &[])?;
    Ok(())
}

pub fn log(env: JNIEnv, tag: &str, msg: &str) -> jni::errors::Result<()> {
    let tag = env.new_string(tag)?;
    let msg = env.new_string(msg)?;

    let class = env.find_class("android/util/Log")?;
    env.call_static_method(
        class,
        "d",
        "(Ljava/lang/String;Ljava/lang/String;)I",
        &[JValue::Object(tag.into()), JValue::Object(msg.into())],
    )?;
    Ok(())
}
