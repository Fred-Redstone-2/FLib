package com.lothrazar.library.cap;

import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Wraps two item resource handlers: Input and Output. Input's slots come first then the Output's slots come after. Items can only be inserted into Input. Items can only be extracted from Output.
 * Note that the above only applies to operations on the wrapper, the backing handlers are not restricted. For persistence, either the backing {@link ResourceHandler}s can be saved, or the wrapper
 * itself.
 *
 * @see com/lothrazar/cyclic/capabilities/
 */
public class ItemStackHandlerWrapper implements ResourceHandler<ItemResource>, ValueIOSerializable {

  public static final String NBT_INPUT = "Input";
  public static final String NBT_OUTPUT = "Output";
  protected final ResourceHandler<ItemResource> input;
  protected final ResourceHandler<ItemResource> output;
  private final int[] slotNumbers;

  public ItemStackHandlerWrapper(ResourceHandler<ItemResource> input, ResourceHandler<ItemResource> output) {
    this.input = input;
    this.output = output;
    int size = input.size() + output.size();
    // for hopper & WorldlyContainer support
    this.slotNumbers = IntStream.range(0, size).toArray();
  }

  /**
   * Calls with the correct handler, slot for the handler and if it matches the input handler.
   */
  protected <T> T withHandler(int externalSlot, HandlerCallback<T> callback) {
    int numInputSlots = input.size();
    boolean isInput = externalSlot < numInputSlots;
    int internalSlot = isInput ? externalSlot : externalSlot - numInputSlots;
    ResourceHandler<ItemResource> handler = isInput ? input : output;
    return callback.apply(handler, internalSlot, isInput);
  }

  /**
   * For functions that return void.
   *
   * @see ItemStackHandlerWrapper#withHandler(int, HandlerCallback)
   */
  protected void withHandlerV(int slot, HandlerCallbackVoid func) {
    withHandler(slot, (h, s, isInput) -> {
      func.apply(h, s, isInput);
      return false; // Because generics can't be void >.<
    });
  }

  public int getSlots() {
    return size();
  }

  @Override
  public int size() {
    return input.size() + output.size();
  }

  @Override
  public ItemResource getResource(int slot) {
    return withHandler(slot, (handler, internalSlot, isInput) -> handler.getResource(internalSlot));
  }

  @Override
  public long getAmountAsLong(int slot) {
    return withHandler(slot, (handler, internalSlot, isInput) -> handler.getAmountAsLong(internalSlot));
  }

  @Override
  public long getCapacityAsLong(int slot, ItemResource resource) {
    return withHandler(slot, (handler, internalSlot, isInput) -> handler.getCapacityAsLong(internalSlot, resource));
  }

  @Override
  public boolean isValid(int slot, ItemResource resource) {
    return withHandler(slot, (handler, internalSlot, isInput) -> isInput && handler.isValid(internalSlot, resource));
  }

  @Override
  public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
    return withHandler(slot, (handler, internalSlot, isInput) -> isInput ? handler.insert(internalSlot, resource, amount, transaction) : 0);
  }

  @Override
  public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
    return withHandler(slot, (handler, internalSlot, isInput) -> isInput ? 0 : handler.extract(internalSlot, resource, amount, transaction));
  }

  public ItemStack getStackInSlot(int slot) {
    ItemResource resource = getResource(slot);
    int amount = getAmountAsInt(slot);
    return resource.isEmpty() || amount <= 0 ? ItemStack.EMPTY : resource.toStack(amount);
  }

  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }
    try (Transaction transaction = Transaction.openRoot()) {
      int inserted = insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
      if (!simulate) {
        transaction.commit();
      }
      return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
    }
  }

  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    ItemResource resource = getResource(slot);
    if (resource.isEmpty()) {
      return ItemStack.EMPTY;
    }
    try (Transaction transaction = Transaction.openRoot()) {
      int extracted = extract(slot, resource, amount, transaction);
      if (!simulate) {
        transaction.commit();
      }
      return extracted == 0 ? ItemStack.EMPTY : resource.toStack(extracted);
    }
  }

  public int getSlotLimit(int slot) {
    ItemResource resource = getResource(slot);
    return resource.isEmpty() ? 64 : getCapacityAsInt(slot, resource);
  }

  public boolean isItemValid(int slot, ItemStack stack) {
    return !stack.isEmpty() && isValid(slot, ItemResource.of(stack));
  }

  public void setStackInSlot(int slot, ItemStack stack) {
    withHandlerV(slot, (handler, internalSlot, isInput) -> {
      try (Transaction transaction = Transaction.openRoot()) {
        ItemResource oldResource = handler.getResource(internalSlot);
        int oldAmount = handler.getAmountAsInt(internalSlot);
        if (!oldResource.isEmpty() && oldAmount > 0) {
          handler.extract(internalSlot, oldResource, oldAmount, transaction);
        }
        if (stack.isEmpty() || handler.insert(internalSlot, ItemResource.of(stack), stack.getCount(), transaction) == stack.getCount()) {
          transaction.commit();
        }
      }
    });
  }

  @Override
  public void serialize(ValueOutput output0) {
    if (input instanceof ValueIOSerializable serializable) {
      serializable.serialize(output0.child(NBT_INPUT));
    }
    if (output instanceof ValueIOSerializable serializable) {
      serializable.serialize(output0.child(NBT_OUTPUT));
    }
  }

  @Override
  public void deserialize(ValueInput input0) {
    if (input instanceof ValueIOSerializable serializable) {
      serializable.deserialize(input0.childOrEmpty(NBT_INPUT));
    }
    if (output instanceof ValueIOSerializable serializable) {
      serializable.deserialize(input0.childOrEmpty(NBT_OUTPUT));
    }
  }

  @FunctionalInterface
  protected interface HandlerCallback<T> {

    T apply(ResourceHandler<ItemResource> handler, int slot, boolean isInput);
  }

  @FunctionalInterface
  protected interface HandlerCallbackVoid {

    void apply(ResourceHandler<ItemResource> handler, int slot, boolean isInput);
  }

  /**
   * Override this to limit face and direction specific restrictions. Default is all faces for every direction
   * 
   * For use with WorldlyContainer.java
   */
  public int[] getSlotsForFace(Direction direction) {
    return slotNumbers;
  }

  /**
   * Support for non-capability interfaces like hoppers to support in-only and out-only. For use with WorldlyContainer.java
   */
  public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, Direction direction) {
    return i < this.input.size();
  }

  /**
   * Support for non-capability interfaces like hoppers to support in-only and out-only. For use with WorldlyContainer.java
   */
  public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
    return i >= this.input.size();
  }
}
