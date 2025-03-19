package me.mantou.jvmti4j;

public final class JVMTIScheduler {
    private static LoadHook LOAD_HOOK;

    public static void setLoadHook(LoadHook loadHook) {
        LOAD_HOOK = loadHook;
    }

    // call from native
    public static LoadHook getLoadHook() {
        return LOAD_HOOK;
    }

    /**
     * Retransform class
     *
     * @param clazz Target class name.
     * @throws IllegalArgumentException If class not found.
     */
    public static void retransformClass(String clazz) throws IllegalArgumentException {
        try {
            retransformClass(Class.forName(clazz));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Retransform class
     *
     * @param clazz Target class.
     * @throws JVMTIException If jvmti retransform error.
     */
    public static void retransformClass(Class<?> clazz) throws JVMTIException {
        JVMTIError error = JVMTIError.fromCode(retransformClass0(clazz));
        if (error != JVMTIError.JVMTI_ERROR_NONE) {
            throw new JVMTIException(error);
        }
    }

    private static native int retransformClass0(Class<?> clazz);

    public static native Class<?>[] getLoadedClasses();

    /**
     * Get class loader loaded classes
     *
     * @param loader The class loader to be used, pass null to use the bootstrap class loader.
     * @return Class loader loaded classes, some JVMTI APIs return all the class loaders and their accessible classes.
     */
    public static native Class<?>[] getClassLoaderClasses(ClassLoader loader);

    @FunctionalInterface
    public interface LoadHook {
        /**
         * Class reloaded callback
         *
         * @param clazz        Target of be reloaded class.
         * @param originalData Target of be reloaded class bytecode.
         * @return The transformed class data if modified, otherwise return null or the original data.
         */
        byte[] invoke(Class<?> clazz, byte[] originalData);
    }
}
