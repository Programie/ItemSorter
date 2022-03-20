package com.selfcoders.itemsorter;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BlockHelper {
    private static final BlockFace[] BLOCK_FACES = {
            BlockFace.DOWN,
            BlockFace.UP,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };

    private static void getConnectedBlocks(Block block, Set<Block> results, List<Block> todo) {
        for (BlockFace face : BLOCK_FACES) {
            Block otherBlock = block.getRelative(face);
            if (otherBlock.getType() == block.getType()) {
                if (results.add(otherBlock)) {
                    todo.add(otherBlock);
                }
            }
        }
    }

    public static Set<Block> getConnectedBlocks(Block block, int maxBlocks) {
        Set<Block> set = new HashSet<>();
        LinkedList<Block> list = new LinkedList<>();

        list.add(block);

        while ((block = list.poll()) != null && (maxBlocks == 0 || set.size() < maxBlocks)) {
            getConnectedBlocks(block, set, list);
        }

        return set;
    }

    public static boolean isChunkLoaded(Block block) {
        return block.getWorld().isChunkLoaded(block.getX() / 16, block.getZ() / 16);
    }
}