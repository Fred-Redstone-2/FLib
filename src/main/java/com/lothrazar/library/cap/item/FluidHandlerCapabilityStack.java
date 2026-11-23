package com.lothrazar.library.cap.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Resource handler that reads/writes a single fluid to an ItemStack's CustomData component.
 */
public class FluidHandlerCapabilityStack extends ItemAccessResourceHandler<FluidResource> {

  public static final String FLUID_NBT_KEY = "Fluid";
  protected final ItemStack container;
  protected final int capacity;

  public FluidHandlerCapabilityStack(ItemStack container, int capacity) {
    super(ItemAccess.forStack(container), 1);
    this.container = container;
    this.capacity = capacity;
  }

  public ItemStack getContainer() {
    return container;
  }

  private static CompoundTag getContainerTag(ItemResource item) {
    return item.toStack(1).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  private void setContainerTag(CompoundTag tag) {
    container.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
  }

  public FluidStack getFluid() {
    return readFluid(ItemResource.of(container));
  }

  private static FluidStack readFluid(ItemResource item) {
    CompoundTag tag = getContainerTag(item);
    if (!tag.contains(FLUID_NBT_KEY)) {
      return FluidStack.EMPTY;
    }
    CompoundTag fluidTag = tag.getCompoundOrEmpty(FLUID_NBT_KEY);
    Identifier fluidId = Identifier.tryParse(fluidTag.getStringOr("id", ""));
    int amount = fluidTag.getIntOr("amount", 0);
    if (fluidId == null) return FluidStack.EMPTY;
    Fluid fluid = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(null);
    if (fluid == null || fluid == Fluids.EMPTY) return FluidStack.EMPTY;
    return new FluidStack(fluid, amount);
  }

  public void setFluid(FluidStack fluid) {
    CompoundTag tag = getContainerTag(ItemResource.of(container));
    if (fluid.isEmpty()) {
      tag.remove(FLUID_NBT_KEY);
    } else {
      CompoundTag fluidTag = new CompoundTag();
      fluidTag.putString("id", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
      fluidTag.putInt("amount", fluid.getAmount());
      tag.put(FLUID_NBT_KEY, fluidTag);
    }
    setContainerTag(tag);
  }

  @Override
  protected FluidResource getResourceFrom(ItemResource item, int tank) {
    return FluidResource.of(readFluid(item));
  }

  @Override
  protected int getAmountFrom(ItemResource item, int tank) {
    return readFluid(item).getAmount();
  }

  @Override
  protected ItemResource update(ItemResource item, int tank, FluidResource resource, int amount) {
    CompoundTag tag = getContainerTag(item);
    if (resource.isEmpty() || amount <= 0) {
      tag.remove(FLUID_NBT_KEY);
    }
    else {
      FluidStack fluid = resource.toStack(amount);
      CompoundTag fluidTag = new CompoundTag();
      fluidTag.putString("id", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
      fluidTag.putInt("amount", amount);
      tag.put(FLUID_NBT_KEY, fluidTag);
    }
    return item.with(DataComponents.CUSTOM_DATA, CustomData.of(tag));
  }

  @Override
  protected int getCapacity(int tank, FluidResource resource) {
    return capacity;
  }

  @Override
  public boolean isValid(int tank, FluidResource resource) {
    return container.getCount() == 1 && !resource.isEmpty() && canFillFluidType(resource.toStack(1));
  }

  public int getTanks() {
    return size();
  }

  public FluidStack getFluidInTank(int tank) {
    return tank == 0 ? getFluid() : FluidStack.EMPTY;
  }

  public int getTankCapacity(int tank) {
    return capacity;
  }

  public boolean isFluidValid(int tank, FluidStack stack) {
    return true;
  }

  public int fill(FluidStack resource, TransactionContext transaction) {
    if (container.getCount() != 1 || resource.isEmpty() || !canFillFluidType(resource)) {
      return 0;
    }
    return insert(0, FluidResource.of(resource), resource.getAmount(), transaction);
  }

  public FluidStack drain(FluidStack resource, TransactionContext transaction) {
    if (container.getCount() != 1 || resource.isEmpty() || !FluidResource.of(resource).matches(getFluid())) {
      return FluidStack.EMPTY;
    }
    return drain(resource.getAmount(), transaction);
  }

  public FluidStack drain(int maxDrain, TransactionContext transaction) {
    if (container.getCount() != 1 || maxDrain <= 0) {
      return FluidStack.EMPTY;
    }
    FluidResource resource = getResource(0);
    int amount = getAmountAsInt(0);
    if (resource.isEmpty() || amount <= 0 || !canDrainFluidType(resource.toStack(amount))) {
      return FluidStack.EMPTY;
    }
    int drained = extract(0, resource, maxDrain, transaction);
    return drained <= 0 ? FluidStack.EMPTY : resource.toStack(drained);
  }

  public boolean canFillFluidType(FluidStack fluid) {
    return true;
  }

  public boolean canDrainFluidType(FluidStack fluid) {
    return true;
  }

  protected void setContainerToEmpty() {
    CompoundTag tag = getContainerTag(ItemResource.of(container));
    tag.remove(FLUID_NBT_KEY);
    setContainerTag(tag);
  }

  public String toString() {
    return "FluidHandlerCapabilityStack [container=" + container + ", capacity=" + capacity + "]";
  }
}
