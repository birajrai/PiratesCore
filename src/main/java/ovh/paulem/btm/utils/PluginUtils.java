package ovh.paulem.btm.utils;

import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.listeners.extendables.ManagersListener;

public class PluginUtils {
    public static void reloadConfig() {
        BetterMending.getInstance().reloadConfig();

        if(BetterMending.getPlayerConfig() != null) BetterMending.getPlayerConfig().reload();

        ManagersListener.reloadConfig(BetterMending.getConf());
    }
}
