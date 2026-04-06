package gov.sandia.specutils;

/**
 * Thread-safe loader for SpecUtils JNI native libraries.
 * Loads both libSpecUtils (core C++ library) and libSpecUtilsJni (SWIG JNI bridge)
 * in the correct dependency order.
 *
 * In production, the NativeClassLoader extracts these from classpath JARs
 * (lib/native/{os}/{arch}/ resources). In tests, java.library.path must be set
 * to point at the appropriate native lib directory.
 */
public class SpecUtilsNativeLoader {

    private static volatile boolean loaded = false;
    private static volatile boolean available = false;

    /**
     * Attempts to load the SpecUtils native libraries.
     * Safe to call multiple times; loading only occurs once.
     *
     * @return true if the native libraries are available, false otherwise
     */
    public static boolean load() {
        if (!loaded) {
            synchronized (SpecUtilsNativeLoader.class) {
                if (!loaded) {
                    try {
                        // On Linux/macOS the core library is a separate shared lib that must
                        // be loaded first. On Windows it is statically linked into the JNI
                        // module, so libSpecUtils is absent and that load will fail — ignore.
                        try {
                            System.loadLibrary("SpecUtils");
                        } catch (UnsatisfiedLinkError ignored) {
                        }
                        System.loadLibrary("SpecUtilsJni");
                        available = true;
                    } catch (UnsatisfiedLinkError e) {
                        available = false;
                    }
                    loaded = true;
                }
            }
        }
        return available;
    }

    /**
     * @return true if the native libraries have been successfully loaded
     */
    public static boolean isAvailable() {
        return available;
    }
}
