package com.example.taczdamageconfig.client;

import com.example.taczdamageconfig.config.ModConfig;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TaczConfigScreen extends Screen {

    private final Screen parent;

    // 全局倍率输入框
    private EditBox globalMultiplierBox;

    // 每把枪对应的数据（按顺序一一对应）
    private final List<ResourceLocation> gunIds = new ArrayList<>();
    private final List<Float> originalDamages = new ArrayList<>();   // TACZ 原始伤害
    private final List<EditBox> gunBoxes = new ArrayList<>();        // 自定义伤害输入框

    // 滚动相关
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP_OFFSET = 90;
    private static final int BOTTOM_BUTTON_HEIGHT = 36;

    // 列布局（相对 width 的偏移，在 init/render 中动态计算）
    // 列：枪名 | 原始伤害 | 输入框 | 生效伤害预览
    private static final int COL_ORIG_W  = 70;  // "原始: XX.X" 宽度
    private static final int COL_BOX_W   = 80;  // 输入框宽度
    private static final int COL_PREV_W  = 90;  // "→ XX.X" 宽度
    private static final int COL_GAP     = 6;
    // 右侧三列总宽 = COL_ORIG_W + GAP + COL_BOX_W + GAP + COL_PREV_W
    private static final int RIGHT_COLS_W = COL_ORIG_W + COL_GAP + COL_BOX_W + COL_GAP + COL_PREV_W;

    public TaczConfigScreen(Screen parent) {
        super(Component.translatable("tacz_damage_config.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        gunIds.clear();
        originalDamages.clear();
        gunBoxes.clear();
        scrollOffset = 0;

        // 全局倍率输入框
        globalMultiplierBox = new EditBox(font,
                width / 2 - 80, 48, 160, 18,
                Component.translatable("tacz_damage_config.config.global_multiplier"));
        globalMultiplierBox.setMaxLength(12);
        globalMultiplierBox.setValue(String.valueOf(ModConfig.GLOBAL_MULTIPLIER.get()));
        globalMultiplierBox.setFilter(s -> s.matches("[0-9]*\\.?[0-9]*"));
        globalMultiplierBox.setHint(Component.literal("1.0"));
        addRenderableWidget(globalMultiplierBox);

        // 收集枪械数据
        try {
            Set<Map.Entry<ResourceLocation, CommonGunIndex>> all =
                    TimelessAPI.getAllCommonGunIndex();
            if (all != null) {
                all.stream()
                        .sorted(Comparator.comparing(e -> e.getKey().toString()))
                        .forEach(e -> {
                            float dmg = 0f;
                            try {
                                dmg = e.getValue().getBulletData().getDamageAmount();
                            } catch (Exception ignored) {}
                            gunIds.add(e.getKey());
                            originalDamages.add(dmg);
                        });
            }
        } catch (Exception ignored) {}

        Map<ResourceLocation, Double> currentMap = ModConfig.getGunDamageMap();
        for (int i = 0; i < gunIds.size(); i++) {
            ResourceLocation id = gunIds.get(i);
            float origDmg = originalDamages.get(i);
            // 有配置用配置值，否则用原始伤害作为默认显示
            double displayVal = currentMap.containsKey(id)
                    ? currentMap.get(id)
                    : origDmg;

            EditBox box = new EditBox(font, 0, 0, COL_BOX_W, 16,
                    Component.literal(id.toString()));
            box.setMaxLength(10);
            box.setValue(String.format("%.2f", displayVal));
            box.setFilter(s -> s.matches("[0-9]*\\.?[0-9]*"));
            box.setHint(Component.literal(String.format("%.2f", origDmg)));
            gunBoxes.add(box);
        }

        // 保存按钮
        addRenderableWidget(Button.builder(
                Component.translatable("tacz_damage_config.config.save"),
                btn -> save())
                .bounds(width / 2 - 82, height - BOTTOM_BUTTON_HEIGHT + 8, 78, 20)
                .build());

        // 取消按钮
        addRenderableWidget(Button.builder(
                Component.translatable("tacz_damage_config.config.cancel"),
                btn -> minecraft.setScreen(parent))
                .bounds(width / 2 + 4, height - BOTTOM_BUTTON_HEIGHT + 8, 78, 20)
                .build());
    }

    private void save() {
        // 保存全局倍率
        try {
            double g = Double.parseDouble(globalMultiplierBox.getValue());
            g = Math.max(0.0, Math.min(100.0, g));
            ModConfig.GLOBAL_MULTIPLIER.set(g);
            ModConfig.GLOBAL_MULTIPLIER.save();
        } catch (NumberFormatException ignored) {}

        // 保存每把枪的绝对伤害值
        Map<ResourceLocation, Double> map = new HashMap<>();
        for (int i = 0; i < gunIds.size(); i++) {
            try {
                double v = Double.parseDouble(gunBoxes.get(i).getValue());
                v = Math.max(0.0, Math.min(10000.0, v));
                float orig = originalDamages.get(i);
                // 仅当与原始伤害不同时才写入，减少配置文件噪音
                if (Math.abs(v - orig) > 0.005) {
                    map.put(gunIds.get(i), v);
                }
            } catch (NumberFormatException ignored) {}
        }
        ModConfig.saveGunDamageMap(map);

        minecraft.setScreen(parent);
    }

    // -----------------------------------------------------------------------
    // 渲染
    // -----------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        // 标题
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        // 全局倍率标签
        g.drawString(font,
                Component.translatable("tacz_damage_config.config.global_multiplier"),
                width / 2 - 80, 36, 0xAAAAAA);

        // 枪列表区域
        int labelX = 8;
        int rightStart = width - RIGHT_COLS_W - 8;
        int colOrigX  = rightStart;
        int colBoxX   = colOrigX + COL_ORIG_W + COL_GAP;
        int colPrevX  = colBoxX + COL_BOX_W + COL_GAP;

        // 列头
        if (!gunIds.isEmpty()) {
            int headerY = LIST_TOP_OFFSET - 16;
            g.drawString(font,
                    Component.translatable("tacz_damage_config.config.col_gun"),
                    labelX, headerY, 0x888888);
            g.drawString(font,
                    Component.translatable("tacz_damage_config.config.col_orig"),
                    colOrigX, headerY, 0x888888);
            g.drawString(font,
                    Component.translatable("tacz_damage_config.config.col_custom"),
                    colBoxX, headerY, 0x888888);
            g.drawString(font,
                    Component.translatable("tacz_damage_config.config.col_effective"),
                    colPrevX, headerY, 0x888888);
        } else {
            g.drawCenteredString(font,
                    Component.translatable("tacz_damage_config.config.no_guns"),
                    width / 2, LIST_TOP_OFFSET, 0xAAAAAA);
        }

        // 解析当前全局倍率（用于预览）
        double globalMult = 1.0;
        try {
            globalMult = Double.parseDouble(globalMultiplierBox.getValue());
        } catch (NumberFormatException ignored) {}

        // 可见行
        int listHeight = height - LIST_TOP_OFFSET - BOTTOM_BUTTON_HEIGHT;
        int visibleRows = listHeight / ROW_HEIGHT;

        for (int i = 0; i < visibleRows && (i + scrollOffset) < gunIds.size(); i++) {
            int idx = i + scrollOffset;
            int rowY = LIST_TOP_OFFSET + i * ROW_HEIGHT;
            int textY = rowY + 5;

            ResourceLocation id = gunIds.get(idx);
            float origDmg = originalDamages.get(idx);
            EditBox box = gunBoxes.get(idx);

            // 偶行底色
            if (idx % 2 == 0) {
                g.fill(0, rowY, width, rowY + ROW_HEIGHT, 0x22FFFFFF);
            }

            // 枪 ID（截断）
            String label = id.toString();
            int maxLabelWidth = colOrigX - labelX - 8;
            if (font.width(label) > maxLabelWidth) {
                label = font.plainSubstrByWidth(label, maxLabelWidth - font.width("…")) + "…";
            }
            g.drawString(font, label, labelX, textY, 0xDDDDDD);

            // 原始伤害标注
            String origText = String.format("%.1f", origDmg);
            g.drawString(font, origText, colOrigX, textY, 0x888888);

            // 自定义伤害输入框
            box.setX(colBoxX);
            box.setY(rowY + (ROW_HEIGHT - 16) / 2);
            box.setWidth(COL_BOX_W);
            box.setHeight(16);
            box.render(g, mouseX, mouseY, partialTick);

            // 生效伤害预览（自定义值 × 全局倍率）
            double previewBase;
            try {
                previewBase = Double.parseDouble(box.getValue());
            } catch (NumberFormatException e2) {
                previewBase = origDmg;
            }
            double effective = previewBase * globalMult;
            // 是否被修改（与原始不同）
            boolean modified = Math.abs(previewBase - origDmg) > 0.005 || Math.abs(globalMult - 1.0) >= 1e-9;
            int prevColor = modified ? 0xFFDD55 : 0x888888;
            g.drawString(font, String.format("→ %.1f", effective), colPrevX, textY, prevColor);

            // 完整 ID 悬浮提示
            if (mouseX >= labelX && mouseX < colOrigX - 4 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                g.renderTooltip(font, Component.literal(id.toString()), mouseX, mouseY);
            }
        }
    }

    // -----------------------------------------------------------------------
    // 鼠标 / 键盘路由
    // -----------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, gunIds.size() - visibleRowCount());
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listHeight = height - LIST_TOP_OFFSET - BOTTOM_BUTTON_HEIGHT;
        int visibleRows = listHeight / ROW_HEIGHT;
        for (int i = 0; i < visibleRows && (i + scrollOffset) < gunBoxes.size(); i++) {
            int idx = i + scrollOffset;
            int rowY = LIST_TOP_OFFSET + i * ROW_HEIGHT;
            EditBox box = gunBoxes.get(idx);
            box.setY(rowY + (ROW_HEIGHT - 16) / 2);
            box.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : visibleBoxes()) {
            if (box.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        for (EditBox box : visibleBoxes()) {
            if (box.charTyped(c, modifiers)) return true;
        }
        // 全局倍率框也要路由
        if (globalMultiplierBox.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }

    private List<EditBox> visibleBoxes() {
        int count = visibleRowCount();
        List<EditBox> list = new ArrayList<>(count);
        for (int i = 0; i < count && (i + scrollOffset) < gunBoxes.size(); i++) {
            list.add(gunBoxes.get(i + scrollOffset));
        }
        return list;
    }

    private int visibleRowCount() {
        int listHeight = height - LIST_TOP_OFFSET - BOTTOM_BUTTON_HEIGHT;
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
