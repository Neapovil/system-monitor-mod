package com.github.neapovil.systemmonitor;

import java.util.ArrayList;
import java.util.List;

import com.github.neapovil.systemmonitor.mixin.MinecraftClientMixin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.PlayerListEntry;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

public final class SystemMonitor implements ClientModInitializer
{
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = this.systemInfo.getHardware();
    private int ticks = 0;
    private long[] cpuOldData = {};
    private long[] cpuNewData = {};
    private double oldCpuLoad = 0.0;
    private double newCpuLoad = 0.0;
    private double cpuLoad = 0.0;

    @Override
    public void onInitializeClient()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null)
            {
                return;
            }

            this.ticks++;

            if (this.ticks >= 20)
            {
                this.cpuNewData = this.hardware.getProcessor().getSystemCpuLoadTicks();

                this.ticks = 0;
            }
        });

        HudRenderCallback.EVENT.register((matrixStack, delta) -> {
            final MinecraftClient client = MinecraftClient.getInstance();

            if (client.options.debugEnabled)
            {
                return;
            }

            final List<String> strings = new ArrayList<>();

            strings.add(MinecraftClientMixin.currentFps() + " FPS");

            final PlayerListEntry playerlistentry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());

            if (playerlistentry != null)
            {
                strings.add(playerlistentry.getLatency() + "ms");
            }

            if (this.cpuNewData.length == 0)
            {
                this.cpuLoad = 0.0;
            }
            else if (this.cpuNewData.equals(this.cpuOldData))
            {
                this.cpuLoad = this.oldCpuLoad;
            }
            else
            {
                this.newCpuLoad = this.hardware.getProcessor().getSystemCpuLoadBetweenTicks(this.cpuNewData) * 100;

                if (this.newCpuLoad != 0.0)
                {
                    this.oldCpuLoad = this.newCpuLoad;
                    this.cpuLoad = this.newCpuLoad;
                }
            }

            strings.add(String.format("CPU load: %.1f%%", this.cpuLoad));

            final long maxmemory = Runtime.getRuntime().maxMemory();
            final long totalmemory = Runtime.getRuntime().totalMemory();
            final long freememory = Runtime.getRuntime().freeMemory();

            strings.add(String.format("RAM usage: %2d%%", (totalmemory - freememory) * 100L / maxmemory));

            final String built = String.join("; ", strings);

            DrawableHelper.fill(matrixStack, 1, 0, 2 + client.textRenderer.getWidth(built) + 1, 2 + client.textRenderer.fontHeight, 0x55000000);

            client.textRenderer.drawWithShadow(matrixStack, built, 2, 2, 0xFFFFFF);
        });
    }
}
