use crate::log::log;
use jni::objects::{JClass, JObject};
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_rustndkdemo_RustJNI_hello(
    env: JNIEnv,
    _: JClass,
    context: JObject,
) -> jstring {
    log::toast(env, context, "hello! Toast...");
    log::log(env, "rust-jni", "asbdssa").unwrap();
    env.new_string("aa").unwrap().into_inner()
}
