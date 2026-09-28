package com.example.taczdamageconfig.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    /** 全局武器伤害倍率 */
    public static final ModConfigSpec.DoubleValue GLOBAL_MULTIPLIER;

    /**
     * 每把武器单独的绝对伤害值列表。
     * 格式："命名空间:武器ID=伤害值"，例如 "tacz:ak47=25.0"
     * 未配置的武器使用 TACZ 原始伤害，再乘全局倍率。
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PER_GUN_DAMAGES;

    private static volatile Map<ResourceLocation, Double> gunDamageCache = null;

    static {
        BUILDER.comment("TACZ 武器伤害设置").push("general");
        GLOBAL_MULTIPLIER = BUILDER
                .comment("全局武器伤害倍率。最终伤害 = 实际伤害 × 全局倍率",
                        "取值范围：0.0 ~ 100.0，默认为 1.0（不改变伤害）")
                .defineInRange("globalMultiplier", 1.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("每把武器的单独绝对伤害值").push("perGunDamages");
        PER_GUN_DAMAGES = BUILDER
                .comment("武器绝对伤害值列表，格式为 \"命名空间:武器ID=伤害值\"",
                        "例如：[\"tacz:ak47=25.0\", \"tacz:m4a1=18.0\"]",
                        "未出现在此列表中的武器使用 TACZ 原始伤害值")
                .defineListAllowEmpty("guns", new ArrayList<>(),
                        e -> e instanceof String s && s.contains(":") && s.contains("="));
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            invalidateCache();
        }
    }

    public static void invalidateCache() {
        gunDamageCache = null;
    }

    public static Map<ResourceLocation, Double> getGunDamageMap() {
        if (gunDamageCache == null) {
            gunDamageCache = parseGunDamages();
        }
        return gunDamageCache;
    }

    private static Map<ResourceLocation, Double> parseGunDamages() {
        Map<ResourceLocation, Double> map = new HashMap<>();
        for (String entry : PER_GUN_DAMAGES.get()) {
            int eq = entry.lastIndexOf('=');
            if (eq <= 0) continue;
            String id = entry.substring(0, eq).trim();
            String val = entry.substring(eq + 1).trim();
            try {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    map.put(rl, Double.parseDouble(val));
                }
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    /** 返回某把武器的配置绝对伤害值，未配置时返回 empty */
    public static Optional<Double> getConfiguredDamage(ResourceLocation gunId) {
        return Optional.ofNullable(getGunDamageMap().get(gunId));
    }

    /** 将整个武器伤害 Map 写回配置文件（并刷新缓存）。仅在客户端配置界面调用。 */
    public static void saveGunDamageMap(Map<ResourceLocation, Double> map) {
        List<String> list = new ArrayList<>(map.size());
        for (Map.Entry<ResourceLocation, Double> e : map.entrySet()) {
            list.add(e.getKey().toString() + "=" + e.getValue());
        }
        PER_GUN_DAMAGES.set(list);
        PER_GUN_DAMAGES.save();
        invalidateCache();
    }
}
