package com.lothrazar.library.cap.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Energy handler that reads/writes energy to an ItemStack's CustomData component.
 * Register this handler with Capabilities.Energy.ITEM when attaching it to an item.
 */
public class EnergyCapabilityItemStack extends SimpleEnergyHandler {

  public static final String NBTENERGY = "energy";
  private final ItemStack stack;

  public EnergyCapabilityItemStack(final ItemStack stack, int capacity) {
    super(capacity, capacity / 4, capacity);
    this.stack = stack;
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      set(data.copyTag().getIntOr(NBTENERGY, 0));
    }
  }

  public int receiveEnergy(int maxReceive, boolean simulate) {
    if (!canReceive()) return 0;
    try (Transaction transaction = Transaction.openRoot()) {
      int received = insert(maxReceive, transaction);
      if (!simulate) {
        transaction.commit();
      }
      return received;
    }
  }

  public int extractEnergy(int maxExtract, boolean simulate) {
    if (!canExtract()) return 0;
    try (Transaction transaction = Transaction.openRoot()) {
      int extracted = extract(maxExtract, transaction);
      if (!simulate) {
        transaction.commit();
      }
      return extracted;
    }
  }

  public int getEnergyStored() {
    return getAmountAsInt();
  }

  public int getMaxEnergyStored() {
    return getCapacityAsInt();
  }

  public boolean canExtract() {
    return true;
  }

  public boolean canReceive() {
    return true;
  }

  @Override
  protected void onEnergyChanged(int value) {
    if (stack == null) {
      return;
    }
    CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    tag.putInt(NBTENERGY, value);
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
  }

  @Override
  public String toString() {
    return "EnergyCapabilityItemStack [energy=" + getEnergyStored() + ", max=" + getMaxEnergyStored() + "]";
  }
}
