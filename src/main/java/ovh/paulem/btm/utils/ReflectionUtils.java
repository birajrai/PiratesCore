package ovh.paulem.btm.utils;

import org.jetbrains.annotations.Nullable;

public class ReflectionUtils {
    @Nullable
    public static<T> T getValueFromEnum(Class<T> cls, String name) {
        try {
            for (T obj : cls.getEnumConstants()) {
                if(obj.toString().equalsIgnoreCase(name)) {
                    return obj;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
