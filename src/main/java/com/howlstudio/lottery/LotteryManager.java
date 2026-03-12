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
    private static final int DRAW_INTERVAL_MIN=60;
    private final Map<UUID,Integer> tickets=new HashMap<>(); // uuid->ticket count
    private final Map<UUID,String> names=new HashMap<>();
    private double pot=0;
    private long nextDraw;
    private ScheduledExecutorService scheduler;
    public LotteryManager(Path d){this.dataDir=d;try{Files.createDirectories(d);}catch(Exception e){}load();if(nextDraw==0)nextDraw=System.currentTimeMillis()+DRAW_INTERVAL_MIN*60_000L;}
    public double getPot(){return pot;}
    public long getMinutesUntilDraw(){return Math.max(0,(nextDraw-System.currentTimeMillis())/60_000);}
    public int getTicketCount(UUID uid){return tickets.getOrDefault(uid,0);}
    public int getTotalTickets(){return tickets.values().stream().mapToInt(Integer::intValue).sum();}
    public void buyTickets(UUID uid,String name,int count,PlayerRef ref){
        double cost=count*TICKET_PRICE;
        tickets.merge(uid,count,Integer::sum);names.put(uid,name);pot+=cost;
        ref.sendMessage(Message.raw("[Lottery] Bought "+count+" ticket(s) for "+String.format("%.0f",cost)+" coins! Pot: §6"+String.format("%.0f",pot)+"§r | Your tickets: "+tickets.get(uid)));
        save();
    }
    public void draw(){
        if(tickets.isEmpty()){broadcast("[Lottery] No tickets sold — draw postponed.");nextDraw=System.currentTimeMillis()+DRAW_INTERVAL_MIN*60_000L;return;}
        // Weighted random draw
        int total=getTotalTickets();
        int pick=(int)(Math.random()*total)+1;
        int cumulative=0;UUID winner=null;
        for(Map.Entry<UUID,Integer> e:tickets.entrySet()){cumulative+=e.getValue();if(cumulative>=pick){winner=e.getKey();break;}}
        if(winner==null)winner=tickets.keySet().iterator().next();
        String wname=names.getOrDefault(winner,"?");double winAmount=pot;
        broadcast("§6[Lottery] §e"+wname+"§r won §6"+String.format("%.0f",winAmount)+" coins§r! ("+tickets.get(winner)+"/"+total+" tickets)");
        tickets.clear();pot=0;nextDraw=System.currentTimeMillis()+DRAW_INTERVAL_MIN*60_000L;
        save();
    }
    public void startTimer(){scheduler=Executors.newSingleThreadScheduledExecutor();long delay=Math.max(0,nextDraw-System.currentTimeMillis());scheduler.scheduleAtFixedRate(this::draw,delay/1000,DRAW_INTERVAL_MIN*60L,TimeUnit.SECONDS);}
    public void stopTimer(){if(scheduler!=null)scheduler.shutdownNow();}
    public void save(){try{StringBuilder sb=new StringBuilder("next="+nextDraw+"\npot="+pot+"\n");for(var e:tickets.entrySet())sb.append(e.getKey()+"|"+names.getOrDefault(e.getKey(),"?")+"|"+e.getValue()+"\n");Files.writeString(dataDir.resolve("lottery.txt"),sb.toString());}catch(Exception e){}}
    private void load(){try{Path f=dataDir.resolve("lottery.txt");if(!Files.exists(f))return;for(String l:Files.readAllLines(f)){if(l.startsWith("next="))nextDraw=Long.parseLong(l.split("=")[1]);else if(l.startsWith("pot="))pot=Double.parseDouble(l.split("=")[1]);else{String[]p=l.split("\\|",3);if(p.length==3){UUID uid=UUID.fromString(p[0]);tickets.put(uid,Integer.parseInt(p[2]));names.put(uid,p[1]);}}};}catch(Exception e){}}
    private void broadcast(String msg){try{for(PlayerRef p:Universe.get().getPlayers())p.sendMessage(Message.raw(msg));}catch(Exception e){}System.out.println(msg);}
    public AbstractPlayerCommand getLotteryCommand(){
        return new AbstractPlayerCommand("lottery","Server lottery. /lottery buy <n> | info"){
            @Override protected void execute(CommandContext ctx,Store<EntityStore> store,Ref<EntityStore> ref,PlayerRef playerRef,World world){
                String[]args=ctx.getInputString().trim().split("\\s+",2);
                String sub=args.length>0?args[0].toLowerCase():"info";
                switch(sub){
                    case"info",""->{playerRef.sendMessage(Message.raw("§6[Lottery] §rNext draw in §e"+getMinutesUntilDraw()+"m§r | Pot: §6"+String.format("%.0f",pot)+" coins§r | Tickets sold: "+getTotalTickets()));playerRef.sendMessage(Message.raw("  Your tickets: "+getTicketCount(playerRef.getUuid())+" | Ticket price: "+String.format("%.0f",TICKET_PRICE)+" coins | /lottery buy <n>"));}
                    case"buy"->{int n=1;try{if(args.length>1)n=Integer.parseInt(args[1]);}catch(Exception e){}if(n<1||n>100){playerRef.sendMessage(Message.raw("[Lottery] Buy 1-100 tickets at a time."));break;}buyTickets(playerRef.getUuid(),playerRef.getUsername(),n,playerRef);}
                    default->playerRef.sendMessage(Message.raw("Usage: /lottery info | /lottery buy <n>"));
                }
            }
        };
    }
}
