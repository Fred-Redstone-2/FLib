package com.lothrazar.library.cap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ItemStackHandlerExTest {

  @BeforeAll
  static void bootstrapMinecraft() {
    Bootstrap.bootStrap();
    if (!Items.STONE.builtInRegistryHolder().areComponentsBound()) {
      Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
          .set(DataComponents.MAX_STACK_SIZE, ItemStackHandlerExTest.ITEM_STACK_SIZE)
          .build());
    }
  }

  private static final int ITEM_STACK_SIZE = 64;

  @Test
  void inventoryChangesCommitAndRollbackWithTransactions() {
    ItemStackHandlerEx handler = new ItemStackHandlerEx(1);
    ItemResource stone = ItemResource.of(Items.STONE);

    try (Transaction transaction = Transaction.openRoot()) {
      assertEquals(4, handler.insert(0, stone, 4, transaction));
    }
    assertEquals(0, handler.getAmountAsInt(0));

    try (Transaction transaction = Transaction.openRoot()) {
      assertEquals(4, handler.insert(0, stone, 4, transaction));
      transaction.commit();
    }
    assertEquals(4, handler.getAmountAsInt(0));

    try (Transaction transaction = Transaction.openRoot()) {
      assertEquals(4, handler.extract(0, stone, 4, transaction));
    }
    assertEquals(4, handler.getAmountAsInt(0));
  }

  @Test
  void wrapperKeepsInputAndOutputOperationsSeparate() {
    ItemStackHandlerEx input = new ItemStackHandlerEx(1);
    ItemStackHandlerEx output = new ItemStackHandlerEx(1);
    ItemStackHandlerWrapper wrapper = new ItemStackHandlerWrapper(input, output);
    ItemResource stone = ItemResource.of(Items.STONE);

    try (Transaction transaction = Transaction.openRoot()) {
      assertEquals(2, wrapper.insert(0, stone, 2, transaction));
      assertEquals(0, wrapper.extract(0, stone, 2, transaction));
      assertEquals(0, wrapper.insert(1, stone, 2, transaction));
      assertEquals(2, output.insert(0, stone, 2, transaction));
      transaction.commit();
    }

    try (Transaction transaction = Transaction.openRoot()) {
      assertEquals(2, wrapper.extract(1, stone, 2, transaction));
      transaction.commit();
    }

    assertEquals(2, input.getAmountAsInt(0));
    assertTrue(output.getStackInSlot(0).isEmpty());
  }
}