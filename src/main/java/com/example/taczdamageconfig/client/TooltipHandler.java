package com.example.taczdamageconfig.client;

import com.example.taczdamageconfig.config.ModConfig;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Optional;

public class TooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        IGun iGun = IGun.getIGunOrNull(event.getItemStack());
        if (iGun == null) return;

        ResourceLocation gunId = iGun.getGunId(event.getItemStack());
        if (gunId == null) return;

        Optional<CommonGunIndex> indexOpt = TimelessAPI.getCommonGunIndex(gunId);
        if (indexOpt.isEmpty()) return;

        float originalDamage;
        try {
            originalDamage = indexOpt.get().getBulletData().getDamageAmount();
        } catch (Exception e) {
            return;
        }

        double globalMult = ModConfig.GLOBAL_MULTIPLIER.get();
        Optional<Double> configured = ModConfig.getConfiguredDamage(gunId);

        float effectiveDamage;
        if (configured.isPresent()) {
            effectiveDamage = (float) (configured.get() * globalMult);
        } else {
            effectiveDamage = (float) (originalDamage * globalMult);
        }

        event.getToolTip().add(Component.translatable(
                "tacz_damage_config.tooltip.damage",
                String.format("%.1f", effectiveDamage)));
    }
}
