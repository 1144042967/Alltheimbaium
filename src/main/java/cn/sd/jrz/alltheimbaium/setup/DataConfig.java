package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DataConfig {
    public static final DataConfig FARM_IRON_GOLEM = new DataConfig(product(Items.IRON_INGOT, 7), product(Items.POPPY, 1)) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_IRON_GOLEM_ENTITY.get();
        }
    };

    public abstract BlockEntityType<?> getType();

    private final List<ItemProduct> productList;

    public DataConfig(ItemProduct... array) {
        this.productList = new ArrayList<>();
        this.productList.addAll(Arrays.asList(array));
    }

    public List<ItemProduct> getProductList() {
        return productList;
    }

    public static ItemProduct product(Item item, long count) {
        return new ItemProduct(item, count);
    }

    public static class ItemProduct {
        public Item item;
        public long count;

        public ItemProduct(Item item, long count) {
            this.item = item;
            this.count = count;
        }
    }
}
