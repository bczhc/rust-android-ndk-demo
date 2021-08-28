use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jstring;

#[no_mangle]
#[allow(non_snake_case)]
extern "C" fn Java_pers_zhc_rustndkdemo_RustJNI_hello(env: JNIEnv, _class: JClass) -> jstring {
    let s = env.new_string("Hello, from Rust.").unwrap();
    s.into_inner()
}
