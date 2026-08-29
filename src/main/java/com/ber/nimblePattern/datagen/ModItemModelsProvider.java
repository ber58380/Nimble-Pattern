package com.ber.nimblePattern.datagen;

import com.ber.nimblePattern.NimblePattern;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public class ModItemModelsProvider extends ItemModelProvider {
    public ModItemModelsProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, NimblePattern.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

    }

    @Override
    public ItemModelBuilder basicItem(Item item) {
        return basicItem(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)));
    }

    @Override
    public ItemModelBuilder basicItem(ResourceLocation item) {
        String path = item.getPath();
        if (path.contains("/")) {
            path = folder + "/" + path;
        }
        return getBuilder(path)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.tryParse(item.getNamespace() + ":item/" + item.getPath()));
    }
}
