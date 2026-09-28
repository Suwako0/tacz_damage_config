package com.example.taczdamageconfig;

import com.example.taczdamageconfig.client.TaczConfigScreen;
import com.example.taczdamageconfig.client.TooltipHandler;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {

    public static void init(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (minecraft, parent) -> new TaczConfigScreen(parent));

        NeoForge.EVENT_BUS.register(TooltipHandler.class);
    }
}
