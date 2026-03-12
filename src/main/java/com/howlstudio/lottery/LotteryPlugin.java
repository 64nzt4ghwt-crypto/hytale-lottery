package com.howlstudio.lottery;
import com.hypixel.hytale.component.Ref; import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.*;
import java.util.concurrent.*;
/**
 * Lottery — Players buy tickets; scheduled draws pick a random winner.
 * /lottery buy — buy a ticket (100 coins each)
 * /lottery pot — view current jackpot
 * /lottery draw — admin triggers a draw
 * /lottery tickets — see ticket count
 */
public final class LotteryPlugin extends JavaPlugin {
    private final List<UUID> ticketPool = new ArrayList<>();
    private final Map<UUID, String> names = new HashMap<>();
    private double pot = 0;
    private static final double TICKET_PRICE = 100;
    private final Random rng = new Random();
    private ScheduledExecutorService scheduler;
    public LotteryPlugin(JavaPluginInit init) { super(init); }
    @Override protected void setup() {
        System.out.println("[Lottery] Loading...");
        CommandManager cmd = CommandManager.get();
        cmd.register(new AbstractPlayerCommand("lottery", "Server lottery! /lottery buy|pot|tickets|draw") {
            @Override protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef pl, World world) {
                String[] args = ctx.getInputString().trim().split("\\s+");
                String sub = args.length > 0 ? args[0].toLowerCase() : "pot";
                switch (sub) {
                    case "buy" -> {
                        ticketPool.add(pl.getUuid()); names.put(pl.getUuid(), pl.getUsername());
                        pot += TICKET_PRICE;
                        pl.sendMessage(Message.raw("[Lottery] Ticket purchased! Jackpot: §6" + String.format("%.0f", pot) + "§r coins. You have "
                            + ticketPool.stream().filter(u -> u.equals(pl.getUuid())).count() + " ticket(s)."));
                    }
                    case "pot" -> pl.sendMessage(Message.raw("[Lottery] Jackpot: §6" + String.format("%.0f", pot) + "§r coins | " + ticketPool.size() + " tickets sold."));
                    case "tickets" -> pl.sendMessage(Message.raw("[Lottery] Your tickets: " + ticketPool.stream().filter(u -> u.equals(pl.getUuid())).count()));
                    case "draw" -> {
                        if (ticketPool.isEmpty()) { pl.sendMessage(Message.raw("[Lottery] No tickets sold.")); return; }
                        UUID winner = ticketPool.get(rng.nextInt(ticketPool.size()));
                        String wname = names.getOrDefault(winner, "?");
                        broadcast("§6[Lottery] 🎰 DRAW! Winner: §e" + wname + "§6 wins §a" + String.format("%.0f", pot) + " coins§6!");
                        ticketPool.clear(); pot = 0;
                    }
                    default -> pl.sendMessage(Message.raw("Usage: /lottery buy|pot|tickets|draw"));
                }
            }
        });
        // Auto-draw every 60 minutes
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (!ticketPool.isEmpty()) {
                UUID winner = ticketPool.get(rng.nextInt(ticketPool.size()));
                broadcast("§6[Lottery] ⏰ Auto-draw! Winner: §e" + names.getOrDefault(winner, "?") + "§6 wins §a" + String.format("%.0f", pot) + "§6!");
                ticketPool.clear(); pot = 0;
            }
        }, 60, 60, TimeUnit.MINUTES);
        System.out.println("[Lottery] Ready. Auto-draw every 60 min.");
    }
    private void broadcast(String msg) {
        try { for (PlayerRef p : Universe.get().getPlayers()) p.sendMessage(Message.raw(msg)); } catch (Exception e) {}
        System.out.println(msg);
    }
    @Override protected void shutdown() { if (scheduler != null) scheduler.shutdownNow(); System.out.println("[Lottery] Stopped."); }
}
