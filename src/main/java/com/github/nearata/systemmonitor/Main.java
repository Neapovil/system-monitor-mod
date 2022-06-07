package com.github.nearata.systemmonitor;

import java.util.ArrayList;
import java.util.List;

import com.github.nearata.systemmonitor.mixin.MinecraftClientMixin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.FormatUtil;

public final class Main implements ClientModInitializer
{
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = this.systemInfo.getHardware();
    private int ticks = 0;
    private long[] cpuOldData = {};
    private long[] cpuNewData = {};
    private double oldCpuLoad = 0.0;
    private double newCpuLoad = 0.0;
    private double cpuLoad = 0.0;
    private final long ramTotal = this.hardware.getMemory().getTotal();
    private long ramAvailable = 0;

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
                this.ramAvailable = this.hardware.getMemory().getAvailable();

                this.ticks = 0;
            }
        });

        HudRenderCallback.EVENT.register((matrixStack, delta) -> {
            final MinecraftClient client = MinecraftClient.getInstance();

            final List<String> strings = new ArrayList<>();

            strings.add(((MinecraftClientMixin) client).getCurrentFps() + " FPS");

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

            final String usedram = FormatUtil.formatBytes(this.ramTotal - this.ramAvailable);
            final String totalram = FormatUtil.formatBytes(this.ramTotal);

            strings.add("RAM: " + usedram.split(" ")[0] + "/" + totalram.split(" ")[0] + "GB");

            client.textRenderer.drawWithShadow(matrixStack, String.join("; ", strings), 2, 2, 0xFFFFFF);
        });
    }
}
