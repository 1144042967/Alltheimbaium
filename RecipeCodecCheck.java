import cn.sd.jrz.alltheimbaium.recipe.BrewingCraftRecipe;
import cn.sd.jrz.alltheimbaium.recipe.PotionCombineRecipe;
import cn.sd.jrz.alltheimbaium.recipe.SmeltingCraftRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;

/**
 * 验证三个自定义配方的 StreamCodec 能正常编码。
 * <p>
 * 修复前用的是 {@code StreamCodec.unit(实例)}，其 encode 会校验
 * {@code value.equals(instance)}；配方未重写 equals，因此 RecipeManager 里的实例
 * （由 MAP_CODEC 解码得到）与 unit 捕获的实例不是同一个对象，编码必然抛
 * "Can't encode ... expected ..."，导致 neoforge:recipe_content 同步包发送失败、
 * 玩家进图时登录流程被打断。
 */
public class RecipeCodecCheck {

    private static <T extends Recipe<?>> void check(String name,
                                                    StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                    T fresh) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        codec.encode(buf, fresh);
        System.out.println("  OK  " + name + " 编码 " + buf.readableBytes() + " 字节");
    }

    public static void main(String[] args) {
        // 脱离游戏跑：先做数据包/注册表 bootstrap，否则静态注册表会拒绝注册
        net.minecraft.server.Bootstrap.bootStrap();
        System.out.println("检查自定义配方的网络编码器：");
        // 传入「新建的实例」——正是修复前会触发同一性校验失败的情形
        check("SmeltingCraftRecipe", SmeltingCraftRecipe.SERIALIZER.streamCodec(), new SmeltingCraftRecipe());
        check("BrewingCraftRecipe", BrewingCraftRecipe.SERIALIZER.streamCodec(), new BrewingCraftRecipe());
        check("PotionCombineRecipe", PotionCombineRecipe.SERIALIZER.streamCodec(), new PotionCombineRecipe());
        System.out.println("全部通过：配方同步包可以正常编码");
    }
}
