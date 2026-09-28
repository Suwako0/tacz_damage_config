package com.example.taczdamageconfig.handler;

import com.example.taczdamageconfig.config.ModConfig;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Optional;

public class DamageEventHandler {

    /**
     * 拦截 TACZ 枪械伤害事件（Pre 阶段），按以下规则计算最终伤害：
     *   - 若该枪配置了绝对伤害值：最终伤害 = 配置值 × 全局倍率
     *   - 否则：最终伤害 = 原始伤害 × 全局倍率
     */
    @SubscribeEvent
    public static void onEntityHurtByGun(EntityHurtByGunEvent.Pre event) {
        ResourceLocation gunId = event.getGunId();
        if (gunId == null) return;

        double globalMult = ModConfig.GLOBAL_MULTIPLIER.get();
        Optional<Double> configured = ModConfig.getConfiguredDamage(gunId);

        float newDamage;
        if (configured.isPresent()) {
            newDamage = (float) (configured.get() * globalMult);
        } else {
            if (Math.abs(globalMult - 1.0) < 1e-9) return;
            newDamage = (float) (event.getBaseAmount() * globalMult);
        }

        event.setBaseAmount(newDamage);
    }
}
