package com.lothrazar.library.cap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * For use with items, tile entities, etc.
 *
 * @see cyclic
 */
public class EnergyStorageWrapper extends SimpleEnergyHandler {

  public static final String NBTENERGY = "energy";

  public EnergyStorageWrapper(int capacity, int maxTransfer) {
    super(capacity, maxTransfer, maxTransfer);
  }

  public void setEnergy(int energyIn) {
    if (energyIn < 0) {
      energyIn = 0;
    }
    if (energyIn > getCapacityAsInt()) {
      energyIn = getCapacityAsInt();
    }
    set(energyIn);
  }

  public int getEnergyStored() {
    return getAmountAsInt();
  }

  public int getMaxEnergyStored() {
    return getCapacityAsInt();
  }

  public int receiveEnergy(int maxReceive, boolean simulate) {
    try (Transaction transaction = Transaction.openRoot()) {
      int received = insert(maxReceive, transaction);
      if (!simulate) {
        transaction.commit();
      }
      return received;
    }
  }

  public int extractEnergy(int maxExtract, boolean simulate) {
    try (Transaction transaction = Transaction.openRoot()) {
      int extracted = extract(maxExtract, transaction);
      if (!simulate) {
        transaction.commit();
      }
      return extracted;
    }
  }

  public boolean canExtract() {
    return true;
  }

  public boolean canReceive() {
    return true;
  }

  /**
   * Serialize energy to a CompoundTag (e.g. for block entity NBT).
   * Use this instead of serializeNBT() when embedding energy inside a larger compound.
   */
  public CompoundTag saveToTag() {
    CompoundTag tag = new CompoundTag();
    tag.putInt(NBTENERGY, getAmountAsInt());
    return tag;
  }

  /**
   * Deserialize energy from a CompoundTag (e.g. from block entity NBT).
   */
  public void loadFromTag(CompoundTag tag) {
    setEnergy(tag.getIntOr(NBTENERGY, 0));
  }

  // Override ValueIOSerializable from EnergyStorage
  @Override
  public void serialize(ValueOutput output) {
    output.putInt(NBTENERGY, getAmountAsInt());
  }

  @Override
  public void deserialize(ValueInput input) {
    setEnergy(input.getIntOr(NBTENERGY, 0));
  }
}
