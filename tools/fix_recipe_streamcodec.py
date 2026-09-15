# -*- coding: utf-8 -*-
"""
修复三个自定义配方的网络编解码器（一次性脚本，幂等）。

背景：26.1.2 迁移时把 1.21.1 的 `SimpleCraftingRecipeSerializer`（streamCodec 走
`StreamCodec.composite`，解码时新建实例、不做同一性校验）换成了 `StreamCodec.unit(实例)`。
而原版 `StreamCodec.unit` 的 encode 会校验 `value.equals(instance)`，本配方未重写 equals，
于是**只要服务器向客户端同步配方**就抛：

    IllegalStateException: Can't encode 'BrewingCraftRecipe@x', expected 'BrewingCraftRecipe@y'

结果 `neoforge:recipe_content` 数据包编码失败、连接层抛异常，玩家进图时登录流程被打断
（实测在 ATM11 里表现为进世界即崩）。

改为「不写字段、解码时新建实例」的自定义 StreamCodec：既避开同一性校验，
又保持 1.21.1 原本「每个实例各自持有 cachedResult」的隔离语义。

    python tools/fix_recipe_streamcodec.py
"""
import io
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                    "src", "main", "java", "cn", "sd", "jrz", "alltheimbaium", "recipe")

RECIPES = ["SmeltingCraftRecipe", "BrewingCraftRecipe", "PotionCombineRecipe"]

DECL = re.compile(
    r"    public static final StreamCodec<RegistryFriendlyByteBuf, (\w+)> STREAM_CODEC =\s*\n"
    r"            StreamCodec\.unit\(new \1\(\)\);")


def decl_for(name):
    return (
        "    public static final StreamCodec<RegistryFriendlyByteBuf, {n}> STREAM_CODEC =\n"
        "            new StreamCodec<>() {\n"
        "                @Override\n"
        "                public @Nonnull {n} decode(@Nonnull RegistryFriendlyByteBuf buf) {\n"
        "                    // 本配方没有任何需要传输的字段：解码时新建实例即可。\n"
        "                    // 不能用 StreamCodec.unit —— 它会校验 value.equals(instance)，\n"
        "                    // 而本配方未重写 equals，会让整个配方同步包编码失败。\n"
        "                    return new {n}();\n"
        "                }\n"
        "\n"
        "                @Override\n"
        "                public void encode(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull {n} recipe) {\n"
        "                    // 无字段可写\n"
        "                }\n"
        "            };").replace("{n}", name)


# 迁移期注释里「实例不能共用」的说法已不准确，改为说明真实约束
DOC_OLD = re.compile(r"(?:\s*\n\s*\*[^\n]*)*?[^\n]*每次(?:从数据包)?加载都?新建实例[^\n]*\n(?:[^\n]*\n)*?[^\n]*单个实例即可。")
DOC_NEW = (
    "     * {@code MAP_CODEC} 与 {@code STREAM_CODEC} 都按「每次解码新建实例」处理：{@code cachedResult}\n"
    "     * 是 matches/assemble 之间的有状态缓存，各实例之间必须相互隔离（与 1.21.1 的\n"
    "     * {@code SimpleCraftingRecipeSerializer} 行为一致）。\n"
)


def main():
    for name in RECIPES:
        path = os.path.join(ROOT, name + ".java")
        text = io.open(path, encoding="utf-8").read()
        new_text, n = DECL.subn(lambda m: decl_for(m.group(1)), text)
        if n == 0:
            print("%s：STREAM_CODEC 已是修好的写法，跳过" % name)
        else:
            text = new_text
            print("%s：已替换 STREAM_CODEC（%d 处）" % (name, n))
        text2, n2 = DOC_OLD.subn(DOC_NEW, text)
        if n2:
            text = text2
            print("%s：已更新注释（%d 处）" % (name, n2))
        if n or n2:
            io.open(path, "w", encoding="utf-8", newline="\n").write(text)


if __name__ == "__main__":
    main()
