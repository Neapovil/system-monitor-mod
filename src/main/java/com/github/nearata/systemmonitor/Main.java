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

public final class Main implements ClientModInitializer
{
    private final SystemInfo si = new SystemInfo();
    private final HardwareAbstractionLayer hal = si.getHardware();
    private long[] oldData = {};
    private long[] newData = {};
    private int ticks = 0;
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

            ticks++;

            if (ticks >= 20)
            {
                this.newData = hal.getProcessor().getSystemCpuLoadTicks();
                ticks = 0;
            }
        });

        HudRenderCallback.EVENT.register((matrixStack, delta) -> {
            final MinecraftClient client = MinecraftClient.getInstance();

            final List<String> strings = new ArrayList<>();

            strings.add(((MinecraftClientMixin) client).getCurrentFps() + " FPS");

            if (this.newData.length == 0)
            {
                this.cpuLoad = 0.0;
            }
            else if (this.newData.equals(this.oldData))
            {
                this.cpuLoad = this.oldCpuLoad;
            }
            else
            {
                this.newCpuLoad = hal.getProcessor().getSystemCpuLoadBetweenTicks(this.newData) * 100;

                if (this.newCpuLoad != 0.0)
                {
                    this.oldCpuLoad = this.newCpuLoad;
                    this.cpuLoad = this.newCpuLoad;
                }
            }

            strings.add(String.format("CPU load: %.1f%%", this.cpuLoad));

            client.textRenderer.drawWithShadow(matrixStack, String.join("; ", strings), 2, 2, 0xFFFFFF);
        });
    }
}
