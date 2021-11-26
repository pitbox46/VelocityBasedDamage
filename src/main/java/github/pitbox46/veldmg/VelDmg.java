package github.pitbox46.veldmg;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod("veldmg")
public class VelDmg {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<ServerPlayerEntity, Vector3d> CURRENT_POS = new HashMap<>();
    private static final Map<ServerPlayerEntity, Vector3d> PREVIOUS_POS = new HashMap<>();

    public VelDmg() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onDamage(LivingHurtEvent event) {
        String dmgType = event.getSource().getDamageType();
        if(dmgType.equals("mob") || dmgType.equals("player")) {
            Entity attacker = event.getSource().getTrueSource();
            Entity victim = event.getEntity();

            if(attacker == null || victim == null) return;

            Vector3d attackerVel;
            if (attacker instanceof ServerPlayerEntity && PREVIOUS_POS.containsKey((ServerPlayerEntity) attacker))
                attackerVel = attacker.getPositionVec().subtract(PREVIOUS_POS.get((ServerPlayerEntity) attacker));
            else
                attackerVel = attacker.getPositionVec().subtract(attacker.prevPosX, attacker.prevPosY, attacker.prevPosZ);

            Vector3d victimVel;
            if (victim instanceof ServerPlayerEntity && PREVIOUS_POS.containsKey((ServerPlayerEntity) victim))
                victimVel = victim.getPositionVec().subtract(PREVIOUS_POS.get((ServerPlayerEntity) victim));
            else
                victimVel = victim.getPositionVec().subtract(victim.prevPosX, victim.prevPosY, victim.prevPosZ);
            Vector3d relPos = victim.getPositionVec().subtract(attacker.getPositionVec());
            Vector3d relVel = attackerVel.subtract(victimVel);

            double multiplier = (relPos.dotProduct(relVel)) / relPos.length();

            if(multiplier < -0.9) multiplier = -0.9;
            else if(!Double.isFinite(multiplier)) multiplier = 0;
            event.setAmount((float) (event.getAmount() * (1 + multiplier)));

//            LOGGER.debug("relPos: " + relPos);
//            LOGGER.debug("relVel: " + relVel);
//            LOGGER.debug("multiplier: " + multiplier);
//            LOGGER.debug("damage: " + event.getAmount());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            PREVIOUS_POS.clear();
            PREVIOUS_POS.putAll(CURRENT_POS);
            CURRENT_POS.put((ServerPlayerEntity) event.player, event.player.getPositionVec());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCriticalHit(CriticalHitEvent event) {
        event.setResult(Event.Result.DENY);
    }
}
