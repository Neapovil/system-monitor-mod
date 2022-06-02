package com.github.nearata.systemmonitor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin
{
    @Accessor("currentFps")
    int getCurrentFps();
}
