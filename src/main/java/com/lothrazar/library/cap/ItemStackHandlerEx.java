package com.lothrazar.library.cap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ItemStackHandlerEx implements ResourceHandler<ItemResource>, ValueIOSerializable {

  private static final String NBT_ITEMS = "Items";
  private final ItemStack[] stacks;
  private final SnapshotJournal<ItemStack[]> journal = new SnapshotJournal<>() {

    @Override
    protected ItemStack[] createSnapshot() {
      ItemStack[] snapshot = new ItemStack[stacks.length];
      for (int i = 0; i < stacks.length; i++) {
        snapshot[i] = stacks[i].copy();
      }
      return snapshot;
    }

    @Override
    protected void revertToSnapshot(ItemStack[] snapshot) {
      for (int i = 0; i < stacks.length; i++) {
        stacks[i] = snapshot[i].copy();
        onContentsChanged(i);
      }
    }
  };

  public ItemStackHandlerEx(int size) {
    stacks = new ItemStack[size];
    Arrays.fill(stacks, ItemStack.EMPTY);
  }

  @Override
  public int size() {
    return stacks.length;
  }

  @Override
  public ItemResource getResource(int slot) {
    return stacks[slot].isEmpty() ? ItemResource.EMPTY : ItemResource.of(stacks[slot]);
  }

  @Override
  public long getAmountAsLong(int slot) {
    return stacks[slot].getCount();
  }

  @Override
  public long getCapacityAsLong(int slot, ItemResource resource) {
    return resource.isEmpty() ? getSlotLimit(slot) : Math.min(getSlotLimit(slot), resource.getMaxStackSize());
  }

  @Override
  public boolean isValid(int slot, ItemResource resource) {
    if (slot < 0 || slot >= stacks.length || resource.isEmpty()) {
      return false;
    }
    ItemStack existing = stacks[slot];
    return (existing.isEmpty() || resource.matches(existing)) && isItemValid(slot, resource.toStack(1));
  }

  @Override
  public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
    if (amount <= 0 || !isValid(slot, resource)) {
      return 0;
    }
    ItemStack existing = stacks[slot];
    int currentAmount = existing.getCount();
    int available = Math.max(0, (int) getCapacityAsLong(slot, resource) - currentAmount);
    int inserted = Math.min(amount, available);
    if (inserted > 0) {
      journal.updateSnapshots(transaction);
      if (existing.isEmpty()) {
        stacks[slot] = resource.toStack(inserted);
      }
      else {
        stacks[slot] = existing.copy();
        stacks[slot].grow(inserted);
      }
      onContentsChanged(slot);
    }
    return inserted;
  }

  @Override
  public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
    if (slot < 0 || slot >= stacks.length || amount <= 0 || resource.isEmpty()) {
      return 0;
    }
    ItemStack existing = stacks[slot];
    if (existing.isEmpty() || !resource.matches(existing)) {
      return 0;
    }
    int extracted = Math.min(amount, existing.getCount());
    if (extracted > 0) {
      journal.updateSnapshots(transaction);
      stacks[slot] = existing.getCount() == extracted ? ItemStack.EMPTY : existing.copyWithCount(existing.getCount() - extracted);
      onContentsChanged(slot);
    }
    return extracted;
  }

  public int getSlots() {
    return size();
  }

  public ItemStack getStackInSlot(int slot) {
    return stacks[slot];
  }

  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }
    try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
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
    try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
      int extracted = extract(slot, resource, amount, transaction);
      if (!simulate) {
        transaction.commit();
      }
      return extracted == 0 ? ItemStack.EMPTY : resource.toStack(extracted);
    }
  }

  public int getSlotLimit(int slot) {
    return 64;
  }

  public boolean isItemValid(int slot, ItemStack stack) {
    return slot >= 0 && slot < stacks.length;
  }

  public void setStackInSlot(int slot, ItemStack stack) {
    stacks[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    onContentsChanged(slot);
  }

  protected void onContentsChanged(int slot) {
  }

  public List<ItemStack> getStacks() {
    List<ItemStack> result = new ArrayList<>();
    for (ItemStack stack : stacks) {
      result.add(stack);
    }
    return result;
  }

  @Override
  public void serialize(ValueOutput output) {
    output.store(NBT_ITEMS, ItemStack.CODEC.listOf(), Arrays.asList(stacks));
  }

  @Override
  public void deserialize(ValueInput input) {
    List<ItemStack> savedStacks = input.read(NBT_ITEMS, ItemStack.CODEC.listOf()).orElse(Collections.emptyList());
    Arrays.fill(stacks, ItemStack.EMPTY);
    for (int i = 0; i < Math.min(stacks.length, savedStacks.size()); i++) {
      stacks[i] = savedStacks.get(i);
    }
  }
}
