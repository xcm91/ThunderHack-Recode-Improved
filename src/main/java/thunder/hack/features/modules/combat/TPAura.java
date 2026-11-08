package thunder.hack.features.modules.combat;

import thunder.hack.api.setting.Setting;
import thunder.hack.events.impl.EventUpdate;
import thunder.hack.features.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

import java.util.Comparator;

public class TPAura extends Module {

    private final Setting<Float> range = new Setting<>("Range", 15.0f, 5.0f, 30.0f);
    private final Setting<Boolean> skipTicks = new Setting<>("Smart Delay", true);

    private int ticks = 0;

    public TPAura() {
        super("TPAura", "Teleports to target, hits, and teleports back", Category.COMBAT);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (skipTicks.getValue()) {
            ticks++;
            if (ticks % 4 != 0) return; 
        }

        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .filter(p -> mc.player.distanceTo(p) <= range.getValue())
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);

        if (target != null) {
            double oldX = mc.player.getX();
            double oldY = mc.player.getY();
            double oldZ = mc.player.getZ();

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    target.getX(), target.getY(), target.getZ(), false, mc.player.isOnGround()
            ));

            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    oldX, oldY, oldZ, true, mc.player.isOnGround()
            ));
        }
    }
}
