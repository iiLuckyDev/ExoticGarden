package io.github.thebusybiscuit.exoticgarden.utils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedPlayerHead;

public final class HeadTextures {

    private HeadTextures() {
    }

    public static ItemStack getItemStack(String textureHash) {
        return VersionedPlayerHead.getItemStack(VersionedPlayerHead.hashToBase64(textureHash));
    }

    public static void setSkinFromHash(Block block, String textureHash, boolean applyPhysics) {
        UUID textureId = UUID.nameUUIDFromBytes(textureHash.getBytes(StandardCharsets.UTF_8));
        VersionedPlayerHead.setSkinFromHash(block, textureId, textureHash, applyPhysics);
    }
}
