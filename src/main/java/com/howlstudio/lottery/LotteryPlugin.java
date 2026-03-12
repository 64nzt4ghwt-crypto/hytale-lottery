package com.howlstudio.lottery;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
/** Lottery — Server lottery with ticket purchases, jackpot pool, and scheduled draws. */
public final class LotteryPlugin extends JavaPlugin {
    private LotteryManager mgr;
    public LotteryPlugin(JavaPluginInit init){super(init);}
    @Override protected void setup(){
        System.out.println("[Lottery] Loading...");
        mgr=new LotteryManager(getDataDirectory());
        new LotteryListener(mgr).register();
        CommandManager.get().register(mgr.getLotteryCommand());
        mgr.startDrawScheduler();
        System.out.println("[Lottery] Ready. Jackpot: "+mgr.getJackpot()+" coins.");
    }
    @Override protected void shutdown(){if(mgr!=null){mgr.stopScheduler();mgr.save();}System.out.println("[Lottery] Stopped.");}
}
