package com.example.taczdamageconfig;

import com.example.taczdamageconfig.config.ModConfig;
import com.example.taczdamageconfig.handler.DamageEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TaczDamageConfigMod.MOD_ID)
public class TaczDamageConfigMod {

    public static final String MOD_ID = "tacz_damage_config";

    public TaczDamageConfigMod(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(Type.COMMON, ModConfig.SPEC);

        // 伤害事件在游戏事件总线上触发
        NeoForge.EVENT_BUS.register(DamageEventHandler.class);

        // 配置文件变更时刷新缓存
        modBus.addListener(ModConfig::onConfigLoad);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modContainer);
        }
    }
}
