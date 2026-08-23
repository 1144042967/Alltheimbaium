package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Curios 饰品槽反射封装（软依赖，未装 Curios 时静默返回空）。
 * <p>
 * 通过反射调用 Curios 桩 API（运行时由 mixin 补全）读取玩家饰品槽中的物品。
 * 只做「读取」操作，不实现 ICurioItem 接口。
 */
public class CuriosHelper {

    private static boolean init;
    private static Class<?> apiClass;
    private static Method getInventory;     // CuriosApi.getCuriosInventory(LivingEntity)
    private static Method resolve;          // LazyOptional.resolve() -> Optional<ICuriosItemHandler>
    private static Method findFirstCurio;   // ICuriosItemHandler.findFirstCurio(Item)
    private static Method slotStack;        // SlotResult.stack()

    /**
     * 是否已安装 Curios
     */
    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded("curios");
    }

    /**
     * 在玩家 Curios 饰品槽中查找指定物品，返回第一个匹配的 ItemStack；未找到/未装 Curios 返回空栈
     */
    public static ItemStack findCurioItem(Player player, Item item) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        initReflection();
        if (apiClass == null) return ItemStack.EMPTY;
        try {
            // LazyOptional<ICuriosItemHandler>
            Object lazyOptional = getInventory.invoke(null, (LivingEntity) player);
            Optional<?> resolved = (Optional<?>) resolve.invoke(lazyOptional);
            if (resolved == null || resolved.isEmpty()) return ItemStack.EMPTY;
            Object handler = resolved.get();
            // Optional<SlotResult>
            Optional<?> slotResult = (Optional<?>) findFirstCurio.invoke(handler, item);
            if (slotResult == null || slotResult.isEmpty()) return ItemStack.EMPTY;
            Object result = slotResult.get();
            return (ItemStack) slotStack.invoke(result);
        } catch (Throwable t) {
            // 反射失败时静默禁用
            return ItemStack.EMPTY;
        }
    }

    /**
     * 初始化反射信息（只尝试一次），失败时禁用 Curios 联动
     */
    private static void initReflection() {
        if (init) return;
        init = true;
        try {
            apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getInventory = apiClass.getMethod("getCuriosInventory", LivingEntity.class);
            resolve = Class.forName("net.minecraftforge.common.util.LazyOptional").getMethod("resolve");
            findFirstCurio = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                    .getMethod("findFirstCurio", Item.class);
            slotStack = Class.forName("top.theillusivec4.curios.api.SlotResult").getMethod("stack");
        } catch (Throwable t) {
            apiClass = null;
        }
    }
}
