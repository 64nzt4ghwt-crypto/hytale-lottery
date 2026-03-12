package com.howlstudio.lottery;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
/** Lottery — Players buy tickets for a chance to win the jackpot. Auto-draw timer. */
public final class LotteryPlugin extends JavaPlugin {
    private LotteryManager mgr;
    public LotteryPlugin(JavaPluginInit init){super(init);}
    @Override protected void setup(){
        System.out.println("[Lottery] Loading...");
        mgr=new LotteryManager(getDataDirectory());
        CommandManager.get().register(mgr.getLotteryCommand());
        mgr.startTimer();
        System.out.println("[Lottery] Ready. Pot: "+mgr.getPot()+" coins. Draw in: "+mgr.getMinutesUntilDraw()+"m");
    }
    @Override protected void shutdown(){if(mgr!=null){mgr.stopTimer();mgr.save();}System.out.println("[Lottery] Stopped.");}
}
