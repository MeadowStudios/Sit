package me.meadow.nms;

import java.lang.reflect.Constructor;

import me.meadow.Sit;

public final class NmsBridges {
    private NmsBridges() {
    }

    public static NmsBridge create(Sit plugin) {
        String className = "me.meadow.nms.v26_2.NmsBridge_26_2";

        try {
            Class<?> rawClass = Class.forName(className);
            Constructor<?> constructor = rawClass.getDeclaredConstructor(Sit.class);
            constructor.setAccessible(true);

            Object instance = constructor.newInstance(plugin);
            if (!(instance instanceof NmsBridge bridge)) {
                plugin.getLogger().warning(className + " does not implement NmsBridge.");
                return new NoopNmsBridge();
            }

            plugin.getLogger().info("NMS Sit bridge enabled for Purpur/Paper 26.2.");
            return bridge;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("NMS Sit bridge could not be enabled: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return new NoopNmsBridge();
        }
    }
}
