package com.ber.nimblePattern.mixin.ae2;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternProviderMenu.class, remap = false)
public abstract class PatternProviderMenuMixin extends AEBaseMenu {
    protected PatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/patternprovider/PatternProviderLogicHost;)V", at = @At("TAIL"))
    private void registerUpgradeSlots(MenuType menuType, int id, Inventory playerInventory, PatternProviderLogicHost host, CallbackInfo ci) {
        if (host.getLogic() instanceof IUpgradeableObject logic) {
            var upgrades = logic.getUpgrades();
            for (int i = 0; i < upgrades.size(); i++) {
                var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, i);
                slot.setNotDraggable();
                this.addSlot(slot, SlotSemantics.UPGRADE);
                // move the upgrade slot to be prior (for the shift-click behavior)
                this.slots.remove(this.slots.size() - 1);
                this.slots.add(0, slot);
            }
            // rearrange index since slots have been arranged
            for (int i = 0; i < slots.size(); i++) {
                this.slots.get(i).index = i;
            }
        }
    }

}
