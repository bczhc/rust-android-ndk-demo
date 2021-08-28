package pers.zhc.rustndkdemo;

/**
 * @author bczhc
 */
public class RustJNI {
    static {
        System.loadLibrary("rust_jni");
    }

    public static native String hello();
}
