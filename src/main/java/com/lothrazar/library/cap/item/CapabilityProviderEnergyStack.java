package com.lothrazar.library.cap.item;

import com.lothrazar.library.cap.EnergyStorageWrapper;
import net.minecraft.nbt.CompoundTag;

/**
 * Simple energy storage wrapper for items.
 */
public class CapabilityProviderEnergyStack {

  private static final String NBTENERGY = "energy";
  private final EnergyStorageWrapper energy;

  public CapabilityProviderEnergyStack(int max) {
    energy = new EnergyStorageWrapper(max, max);
  }

  public EnergyStorageWrapper getEnergyStorage() {
    return energy;
  }

  public CompoundTag serializeNBT() {
    return energy.saveToTag();
  }

  public void deserializeNBT(CompoundTag nbt) {
    energy.loadFromTag(nbt);
  }

  @Override
  public String toString() {
    return "CapabilityProviderEnergyStack [energy=" + energy + "]";
  }
}
