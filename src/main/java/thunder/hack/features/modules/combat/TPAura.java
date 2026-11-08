package thunder.hack.features.modules.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.events.Subscribe;
import thunder.hack.events.impl.EventUpdate;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TPAura extends Module {
    private final Setting<Float> range = new Setting<>("Range", 15.0f, 5.0f, 30.0f);
    private final Setting<Boolean> skipTicks = new Setting<>("Smart Delay", true);

    private double oldX, oldY, oldZ;

    public TPAura() {
        super("TPAura", "Teleports to nearby players to attack them", Module.Category.COMBAT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) return;

        oldX = mc.player.getX();
        oldY = mc.player.getY();
        oldZ = mc.player.getZ();
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        // Find closest valid target
        PlayerEntity target = findClosestTarget();
        if (target == null) return;

        // Save current position
        oldX = mc.player.getX();
        oldY = mc.player.getY();
        oldZ = mc.player.getZ();

        // Teleport to target
        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(
                target.getX(),
                target.getY(),
                target.getZ(),
                false,  // Изменение вращения головы (false = не изменять)
                mc.player.isOnGround()
            )
        );

        // Attack target
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Teleport back
        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(
                oldX,
                oldY,
                oldZ,
                false,  // Изменение вращения головы (false = не изменять)
                mc.player.isOnGround()
            )
        );
    }

    private PlayerEntity findClosestTarget() {
        if (mc.world == null || mc.player == null) return null;

        List<PlayerEntity> players = mc.world.getPlayers().stream()
            .filter(player -> player != mc.player)
            .filter(PlayerEntity::isAlive)
            .filter(player -> mc.player.distanceTo(player) <= range.getValue())
            .sorted(Comparator.comparingDouble(mc.player::distanceTo))
            .collect(Collectors.toList());

        return players.isEmpty() ? null : players.get(0);
    }
}
