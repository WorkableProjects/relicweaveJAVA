package com.relicweave.vestige.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world Vestige state stored in the Vestige level's data storage (Arrival Plaza location). */
public class VestigeWorldData extends SavedData {
    public static final String NAME = "relicweave_vestige";

    private boolean built;
    private BlockPos waystone = BlockPos.ZERO;
    private BlockPos arrival = BlockPos.ZERO;
    private int buildCount;
    private int plazaVersion = 1;

    public static VestigeWorldData get(ServerLevel vestige) {
        return vestige.getDataStorage().computeIfAbsent(VestigeWorldData::load, VestigeWorldData::new, NAME);
    }

    public static VestigeWorldData load(CompoundTag tag) {
        VestigeWorldData data = new VestigeWorldData();
        data.built = tag.getBoolean("built");
        data.waystone = NbtUtils.readBlockPos(tag.getCompound("waystone"));
        data.arrival = NbtUtils.readBlockPos(tag.getCompound("arrival"));
        data.buildCount = tag.getInt("buildCount");
        data.plazaVersion = tag.contains("plazaVersion") ? tag.getInt("plazaVersion") : 1;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("built", built);
        tag.put("waystone", NbtUtils.writeBlockPos(waystone));
        tag.put("arrival", NbtUtils.writeBlockPos(arrival));
        tag.putInt("buildCount", buildCount);
        tag.putInt("plazaVersion", plazaVersion);
        return tag;
    }

    public boolean isBuilt() {
        return built;
    }

    public BlockPos waystone() {
        return waystone;
    }

    public BlockPos arrival() {
        return arrival;
    }

    /** How many times the full plaza was built (1 in a healthy world). */
    public int buildCount() {
        return buildCount;
    }

    /** Design version of the built plaza (1 = the original 11x11 slab, also the value for worlds saved before it existed). */
    public int plazaVersion() {
        return plazaVersion;
    }

    /** Overrides the stored plaza version; used by tests to simulate an older world. */
    public void setPlazaVersion(int version) {
        this.plazaVersion = version;
        setDirty();
    }

    /** Records an in-place rebuild with the current design; the full-build counter is left untouched. */
    void setUpgraded(BlockPos waystone, BlockPos arrival) {
        this.waystone = waystone;
        this.arrival = arrival;
        this.plazaVersion = ArrivalPlaza.CURRENT_VERSION;
        setDirty();
    }

    void setBuilt(BlockPos waystone, BlockPos arrival) {
        this.built = true;
        this.plazaVersion = ArrivalPlaza.CURRENT_VERSION;
        this.waystone = waystone;
        this.arrival = arrival;
        this.buildCount++;
        setDirty();
    }
}
