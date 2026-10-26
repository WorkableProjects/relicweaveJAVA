package com.relicweave.vestige.entity.ethereal;

/** Synced pose hint for client models: 0 = idle, up to 1 = fully into an attack wind-up (arms up, wings folded...). */
public interface WindupState {
    float windupProgress();
}
