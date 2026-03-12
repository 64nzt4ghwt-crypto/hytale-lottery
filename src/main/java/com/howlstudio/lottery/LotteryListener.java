package com.howlstudio.lottery;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
public class LotteryListener {
    private final LotteryManager mgr;
    public LotteryListener(LotteryManager m){this.mgr=m;}
    public void register(){
        HytaleServer.get().getEventBus().registerGlobal(PlayerReadyEvent.class,e->{
            Player p=e.getPlayer();if(p==null)return;
            PlayerRef ref=p.getPlayerRef();if(ref==null)return;
            ref.sendMessage(Message.raw("[Lottery] Jackpot: §6"+String.format("%.0f",mgr.getJackpot())+" coins§r | /lottery buy <n>"));
        });
    }
}
