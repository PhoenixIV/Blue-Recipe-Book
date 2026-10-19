package dev.bluerecipebook.client.dev;

import dev.architectury.event.events.client.ClientTickEvent;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Runner that sends one test sequence a command at a time. The first command waits {@link #FIRST_DELAY_TICKS}, and each
 * one after it waits {@link #STEP_DELAY_TICKS}. A new sequence replaces the running one, and leaving the world drops it.
 */
public final class DevFeed {
    public static final int FIRST_DELAY_TICKS = 3 * 20;
    public static final int STEP_DELAY_TICKS = 6 * 20;

    private static List<String> pending = List.of();
    private static int next;
    private static int countdown;

    private DevFeed() {
    }

    /** Registers the ticker that sends the sequence. Call it only in dev mode. */
    public static void register() {
        ClientTickEvent.CLIENT_PRE.register(minecraft -> tick());
    }

    public static void start(List<String> commands) {
        pending = List.copyOf(commands);
        next = 0;
        countdown = FIRST_DELAY_TICKS;
    }

    private static void tick() {
        if (next >= pending.size()) {
            return;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            pending = List.of();
            return;
        }
        if (countdown > 0) {
            countdown--;
            return;
        }
        connection.sendCommand(pending.get(next++));
        countdown = STEP_DELAY_TICKS;
    }
}
