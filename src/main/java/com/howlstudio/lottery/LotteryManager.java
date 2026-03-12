package com.howlstudio.lottery;
import com.hypixel.hytale.component.Ref; import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.*; import java.util.*;
import java.util.concurrent.*;
public class LotteryManager {
    private final Path dataDir;
    private static final double TICKET_PRICE=50;
    private static final long DRAW_INTERVAL_HRS=24;
    private final Map<UUID,Integer> tickets=new HashMap<>(); // uid→ticket count
    private final Map<UUID,String> names=new HashMap<>();
    private double jackpot=0;
    private ScheduledExecutorService scheduler;
    private long nextDrawTime;
    public LotteryManager(Path d){this.dataDir=d;try{Files.createDirectories(d);}catch(Exception e){}load();nextDrawTime=System.currentTimeMillis()+DRAW_INTERVAL_HRS*3_600_000;}
    public double getJackpot(){return jackpot;}
    public int getTicketCount(UUID uid){return tickets.getOrDefault(uid,0);}
    public int getTotalTickets(){return tickets.values().stream().mapToInt(Integer::intValue).sum();}
    public boolean buyTickets(UUID uid,String name,int count){if(count<1)return false;tickets.merge(uid,count,Integer::sum);names.put(uid,name);jackpot+=TICKET_PRICE*count;save();return true;}
    public void draw(){
        if(tickets.isEmpty()){broadcast("[Lottery] Draw: no tickets sold. Jackpot rolls over!");return;}
        // Build weighted pool
        List<UUID> pool=new ArrayList<>();
        for(Map.Entry<UUID,Integer> e:tickets.entrySet())for(int i=0;i<e.getValue();i++)pool.add(e.getKey());
        UUID winner=pool.get(new Random().nextInt(pool.size()));
        String winnerName=names.getOrDefault(winner,"?");
        double prize=jackpot;
        broadcast("§6[Lottery] §eDraw time!§r "+getTotalTickets()+" tickets entered.");
        broadcast("§6[Lottery] §aWINNER: §e"+winnerName+"§r wins §6"+String.format("%.0f",prize)+" coins§r!");
        System.out.println("[Lottery] Winner: "+winnerName+" ("+winner+") prize="+prize);
        tickets.clear();jackpot=0;save();
        nextDrawTime=System.currentTimeMillis()+DRAW_INTERVAL_HRS*3_600_000;
    }
    public void startDrawScheduler(){scheduler=Executors.newSingleThreadScheduledExecutor();scheduler.scheduleAtFixedRate(this::draw,DRAW_INTERVAL_HRS,DRAW_INTERVAL_HRS,TimeUnit.HOURS);}
    public void stopScheduler(){if(scheduler!=null)scheduler.shutdownNow();}
    public void save(){try{StringBuilder sb=new StringBuilder("jackpot="+jackpot+"\n");for(Map.Entry<UUID,Integer> e:tickets.entrySet())sb.append(e.getKey()+"|"+names.getOrDefault(e.getKey(),"?")+"|"+e.getValue()+"\n");Files.writeString(dataDir.resolve("lottery.txt"),sb.toString());}catch(Exception e){}}
    private void load(){try{Path f=dataDir.resolve("lottery.txt");if(!Files.exists(f))return;for(String l:Files.readAllLines(f)){if(l.startsWith("jackpot="))jackpot=Double.parseDouble(l.split("=")[1]);else{String[]p=l.split("\\|",3);if(p.length==3){UUID uid=UUID.fromString(p[0]);tickets.put(uid,Integer.parseInt(p[2]));names.put(uid,p[1]);}}}}catch(Exception e){}}
    private void broadcast(String msg){try{for(PlayerRef p:Universe.get().getPlayers())p.sendMessage(Message.raw(msg));}catch(Exception e){}System.out.println(msg);}
    public AbstractPlayerCommand getLotteryCommand(){
        return new AbstractPlayerCommand("lottery","Server lottery. /lottery buy <n>|info|draw (admin)"){
            @Override protected void execute(CommandContext ctx,Store<EntityStore> store,Ref<EntityStore> ref,PlayerRef playerRef,World world){
                UUID uid=playerRef.getUuid();String[]args=ctx.getInputString().trim().split("\\s+",2);
                String sub=args.length>0?args[0].toLowerCase():"info";
                switch(sub){
                    case"info"->{long mins=(nextDrawTime-System.currentTimeMillis())/60_000;playerRef.sendMessage(Message.raw("[Lottery] Jackpot: §6"+String.format("%.0f",jackpot)+" coins§r | Tickets: "+getTotalTickets()+" | Your tickets: "+getTicketCount(uid)+" | Next draw: "+Math.max(0,mins)+"m | Price: "+String.format("%.0f",TICKET_PRICE)+" each"));}
                    case"buy"->{int n=1;try{if(args.length>1)n=Integer.parseInt(args[1]);}catch(Exception e){}if(n<1||n>100){playerRef.sendMessage(Message.raw("[Lottery] Buy 1-100 tickets."));break;}buyTickets(uid,playerRef.getUsername(),n);playerRef.sendMessage(Message.raw("[Lottery] Bought §6"+n+" ticket(s)§r! Total: "+getTicketCount(uid)+" | Jackpot: "+String.format("%.0f",jackpot)));}
                    case"draw"->{draw();}
                    default->playerRef.sendMessage(Message.raw("Usage: /lottery info | buy <n> | draw"));
                }
            }
        };
    }
}
