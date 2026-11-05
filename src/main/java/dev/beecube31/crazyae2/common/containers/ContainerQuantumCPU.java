package dev.beecube31.crazyae2.common.containers;

import appeng.api.config.SecurityPermissions;
import dev.beecube31.crazyae2.client.gui.CrazyAESlot;
import dev.beecube31.crazyae2.common.containers.base.CrazyAEBaseContainer;
import dev.beecube31.crazyae2.common.containers.base.slot.RestrictedSlot;
import dev.beecube31.crazyae2.common.tile.crafting.TileQuantumCPU;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class ContainerQuantumCPU extends CrazyAEBaseContainer {
    public static final int SLOTS_PER_PAGE = 8 * 8;

    private final TileQuantumCPU quantumCPU;
    private int currentPage = 0;
    private final List<Slot> patternSlotsOnPage = new ArrayList<>();

    public ContainerQuantumCPU(final InventoryPlayer ip, final TileQuantumCPU te) {
        super(ip, te);
        this.quantumCPU = te;

        this.setupConfig();

        this.bindPlayerInventory(ip, 0, 256 - 82);
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public int getTotalPages() {
        if (this.quantumCPU == null || this.quantumCPU.getInventoryByName("patterns") == null) {
            return 1;
        }

        return (int) Math.ceil((double) this.quantumCPU.getInventoryByName("patterns").getSlots() / SLOTS_PER_PAGE);
    }

    public void setCurrentPage(int newPage) {
        if (this.quantumCPU == null) return;

        int totalPages = getTotalPages();
        if (newPage < 0) {
            newPage = 0;
        }
        if (newPage >= totalPages) {
            newPage = totalPages - 1;
        }

        if (this.currentPage == newPage && !patternSlotsOnPage.isEmpty()) {
            return;
        }

        this.currentPage = newPage;
        updatePatternSlots();

        // 即使两页对应槽位的物品相同，也要同步新页，避免客户端保留过期内容。
        for (IContainerListener listener : this.listeners) {
            listener.sendAllContents(this, this.getInventory());
        }
    }

    private void updatePatternSlots() {
        if (this.quantumCPU == null || this.quantumCPU.getInventoryByName("patterns") == null) {
            return;
        }

        final IItemHandler patternsHandler = this.quantumCPU.getInventoryByName("patterns");
        int startIndex = this.currentPage * SLOTS_PER_PAGE;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int slotIndexOnPage = x + y * 8;
                int slotIndexInFullInv = startIndex + slotIndexOnPage;
                if (slotIndexInFullInv < patternsHandler.getSlots()) {
                    CrazyAESlot patternSlot = new RestrictedSlot(
                            RestrictedSlot.PlaceableItemType.ENCODED_CRAFTING_PATTERN,
                            patternsHandler,
                            slotIndexInFullInv,
                            17 + x * 18,
                            15 + y * 18,
                            this.getInventoryPlayer()
                    ).setStackLimit(1);
                    if (slotIndexOnPage < this.patternSlotsOnPage.size()) {
                        // 原位替换样板映射，保持背包槽编号和同步缓存的位置不变。
                        Slot previousSlot = this.patternSlotsOnPage.set(slotIndexOnPage, patternSlot);
                        patternSlot.slotNumber = previousSlot.slotNumber;
                        patternSlot.setContainer(this);
                        this.inventorySlots.set(patternSlot.slotNumber, patternSlot);
                    } else {
                        this.addSlotToContainer(patternSlot);
                        this.patternSlotsOnPage.add(patternSlot);
                    }
                }
            }
        }
    }


    protected void setupConfig() {
        if (this.quantumCPU != null) {
            updatePatternSlots();
        }
    }


    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);

        super.detectAndSendChanges();
    }
}
