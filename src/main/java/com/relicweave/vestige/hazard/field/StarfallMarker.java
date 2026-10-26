package com.relicweave.vestige.hazard.field;

import com.relicweave.vestige.hazard.FieldRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The telegraph and strike of one Starfall Impact. It has no AI and no gravity, shows a ring and a light column for
 * its warning time, then strikes via {@link StarfallImpacts#strike} and discards itself. Remaining ticks, radius,
 * damage and owner are saved so a chunk reload keeps the telegraph.
 */
public class StarfallMarker extends Entity {
    public static final float DEFAULT_RADIUS = 2.5F;
    private static final int RING_POINTS = 16;

    private int remaining = 60;
    private float radius = DEFAULT_RADIUS;
    private float damage = 6.0F;
    @Nullable
    private UUID ownerId;

    public StarfallMarker(EntityType<? extends StarfallMarker> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public StarfallMarker(Level level) {
        this(FieldRegistry.STARFALL_MARKER.get(), level);
    }

    /** Configures a freshly created marker. */
    public void setup(int warningTicks, float damage, @Nullable Entity owner) {
        this.remaining = Math.max(1, warningTicks);
        this.damage = damage;
        this.ownerId = owner == null ? null : owner.getUUID();
    }

    public int remainingTicks() {
        return remaining;
    }

    public float radius() {
        return radius;
    }

    public float damage() {
        return damage;
    }

    @Nullable
    public UUID ownerId() {
        return ownerId;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (remaining % FieldHazards.INTERVAL == 0) {
            drawTelegraph(server);
        }
        if (--remaining <= 0) {
            StarfallImpacts.strike(server, this);
            discard();
        }
    }

    private void drawTelegraph(ServerLevel server) {
        double x = getX();
        double y = getY() + 0.1;
        double z = getZ();
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_POINTS;
            server.sendParticles(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
                    x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 6; i++) {
            server.sendParticles(ParticleTypes.END_ROD, x, y + 0.5 + i * 0.8, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        remaining = tag.getInt("Remaining");
        radius = tag.contains("Radius") ? tag.getFloat("Radius") : DEFAULT_RADIUS;
        damage = tag.getFloat("Damage");
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Remaining", remaining);
        tag.putFloat("Radius", radius);
        tag.putFloat("Damage", damage);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
