package com.ber.nimblePattern.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import appeng.util.ConfigManager;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.helpers.IPatternUpgradeLogicHost;
import com.ber.nimblePattern.helpers.IPatternUpgradeMenuHost;
import com.ber.nimblePattern.menu.PatternUpgradeTermMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PatternUpgradeTerminalPart extends AbstractDisplayPart implements IConfigurableObject, IPatternUpgradeLogicHost, IPatternUpgradeMenuHost {
    @PartModels
    public static final ResourceLocation MODEL_OFF = ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "part/pattern_upgrade_terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "part/pattern_upgrade_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final ConfigManager configManager = new ConfigManager(this::markForSave);
    private final PatternUpgradeLogic logic = new PatternUpgradeLogic(this);

    public PatternUpgradeTerminalPart(IPartItem<?> partItem) {
        super(partItem, true);
        // 未来或许需要用configManager配置终端的设置，暂不启用
//        this.configManager.registerSetting(PatternUpgradeTerminalSettings.DISPLAY_MODE, DisplayMode.FLAT);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for (var is : this.logic.getInputPatternInv()) {
            drops.add(is);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.getInputPatternInv().clear();
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!super.onPartActivate(player, hand, pos) && !isClientSide()) {
            MenuOpener.open(PatternUpgradeTermMenu.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public PatternUpgradeLogic getLogic() {
        return logic;
    }

    @Override
    public void markForSave() {
        getHost().markForSave();
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        configManager.writeToNBT(tag);
        logic.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        configManager.readFromNBT(tag);
        logic.readFromNBT(tag);
    }
}
