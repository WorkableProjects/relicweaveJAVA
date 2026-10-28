package com.relicweave.vestige.entity.humanoid;

/**
 * Implemented by humanoid Vestige mobs whose telegraph state is shown through the arms. The value is synced with
 * {@code SynchedEntityData} and read by the client model.
 */
public interface VisualPose {
    int POSE_NONE = 0;
    /** Shield raised on the off hand. */
    int POSE_GUARD = 1;
    /** Both arms raised (casting, chanting, wind-up). */
    int POSE_RAISED = 2;
    /** Main arm raised overhead (sweep wind-up). */
    int POSE_OVERHEAD = 3;
    /** Both arms forward (lunge, undead reach). */
    int POSE_REACH = 4;

    int visualPose();
}
