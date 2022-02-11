package com.lauriethefish.betterportals.bukkit.portal.blend;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.config.PortalSpawnConfig;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

@Singleton
public class DimensionBlendManager implements IDimensionBlendManager    {
    private static final double INITIAL_CHANCE = 1.0;
    private static final Material[] BLACKLISTED_COPY_BLOCKS = new Material[] {
    //banyamesterseg added
      //air and admin blocks
        Material.LIGHT,
        Material.END_PORTAL,
        Material.END_GATEWAY,
        Material.END_PORTAL_FRAME,
        Material.STRUCTURE_BLOCK,
        Material.STRUCTURE_VOID,
        Material.JIGSAW,
        Material.COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK,
      //containers
        Material.BARREL,
        Material.BREWING_STAND,
        Material.CHEST,
        Material.ENDER_CHEST,
        Material.TRAPPED_CHEST,
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.DISPENSER,
        Material.DROPPER,
        Material.HOPPER,
        Material.SMOKER,
        Material.SHULKER_BOX,
        Material.BLACK_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX,
        Material.ENCHANTING_TABLE,
        Material.JUKEBOX,
      //redstone stuff
        Material.REDSTONE,
        Material.REDSTONE_TORCH,
        Material.REDSTONE_BLOCK,
        Material.REDSTONE_LAMP,
        Material.NOTE_BLOCK,
        Material.SCULK_SENSOR,
        Material.DAYLIGHT_DETECTOR,
        Material.PISTON,
        Material.STICKY_PISTON,
        Material.REPEATER,
        Material.COMPARATOR,
        Material.TRIPWIRE,
        Material.TRIPWIRE_HOOK,
        Material.STONE_BUTTON,
        Material.POLISHED_BLACKSTONE_BUTTON,
        Material.OAK_BUTTON,
        Material.SPRUCE_BUTTON,
        Material.BIRCH_BUTTON,
        Material.JUNGLE_BUTTON,
        Material.ACACIA_BUTTON,
        Material.DARK_OAK_BUTTON,
        Material.CRIMSON_BUTTON,
        Material.WARPED_BUTTON,
        Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
        Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
        Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
        Material.OAK_PRESSURE_PLATE,
        Material.SPRUCE_PRESSURE_PLATE,
        Material.BIRCH_PRESSURE_PLATE,
        Material.JUNGLE_PRESSURE_PLATE,
        Material.ACACIA_PRESSURE_PLATE,
        Material.DARK_OAK_PRESSURE_PLATE,
        Material.CRIMSON_PRESSURE_PLATE,
        Material.WARPED_PRESSURE_PLATE,
        Material.IRON_DOOR,
        Material.IRON_TRAPDOOR,
        Material.OAK_DOOR,
        Material.OAK_TRAPDOOR,
        Material.OAK_FENCE_GATE,
        Material.SPRUCE_DOOR,
        Material.SPRUCE_TRAPDOOR,
        Material.SPRUCE_FENCE_GATE,
        Material.BIRCH_DOOR,
        Material.BIRCH_TRAPDOOR,
        Material.BIRCH_FENCE_GATE,
        Material.JUNGLE_DOOR,
        Material.JUNGLE_TRAPDOOR,
        Material.JUNGLE_FENCE_GATE,
        Material.ACACIA_DOOR,
        Material.ACACIA_TRAPDOOR,
        Material.ACACIA_FENCE_GATE,
        Material.DARK_OAK_DOOR,
        Material.DARK_OAK_TRAPDOOR,
        Material.DARK_OAK_FENCE_GATE,
        Material.CRIMSON_DOOR,
        Material.CRIMSON_TRAPDOOR,
        Material.CRIMSON_FENCE_GATE,
        Material.WARPED_DOOR,
        Material.WARPED_TRAPDOOR,
        Material.WARPED_FENCE_GATE,
        Material.RAIL,
        Material.ACTIVATOR_RAIL,
        Material.DETECTOR_RAIL,
        Material.POWERED_RAIL,
        Material.LEVER,
        Material.OAK_SIGN,
        Material.SPRUCE_SIGN,
        Material.BIRCH_SIGN,
        Material.JUNGLE_SIGN,
        Material.ACACIA_SIGN,
        Material.DARK_OAK_SIGN,
        Material.CRIMSON_SIGN,
        Material.WARPED_SIGN,
        Material.OAK_WALL_SIGN,
        Material.SPRUCE_WALL_SIGN,
        Material.BIRCH_WALL_SIGN,
        Material.ACACIA_WALL_SIGN,
        Material.JUNGLE_WALL_SIGN,
        Material.DARK_OAK_WALL_SIGN,
        Material.CRIMSON_WALL_SIGN,
        Material.WARPED_WALL_SIGN,
      //misc
        Material.TORCH,
        Material.GOLD_BLOCK,
        Material.RAW_GOLD_BLOCK,
        Material.COPPER_BLOCK,
        Material.RAW_COPPER_BLOCK,
        Material.RAW_IRON_BLOCK,
        Material.LAPIS_BLOCK,
    //original
        Material.OBSIDIAN,
        Material.BEDROCK,
        MaterialUtil.PORTAL_MATERIAL,
        Material.AIR,
        Material.BARRIER,
        Material.DIAMOND_BLOCK,
        Material.EMERALD_BLOCK,
        Material.IRON_BLOCK
    };

    private final PortalSpawnConfig spawnConfig;
    private final Random random = new Random();
    private final Logger logger;

    @Inject
    public DimensionBlendManager(PortalSpawnConfig spawnConfig, Logger logger) {
        this.spawnConfig = spawnConfig;
        this.logger = logger;
    }

    private @NotNull Material findFillInBlock(@NotNull Location destination) {
        switch(Objects.requireNonNull(destination.getWorld(), "World of destination location cannot be null").getEnvironment()) {
            case NETHER:
                return Material.NETHERRACK;
            case NORMAL:
                return Material.STONE;
            case THE_END:
                return VersionUtil.isMcVersionAtLeast("1.13.0") ? Material.valueOf("END_STONE") : Material.valueOf("ENDER_STONE");
            default:
                return Material.AIR;
        }
    }

    @Override
    public void performBlend(@NotNull Location origin, @NotNull Location destination) {
        logger.fine("Origin for blend: %s.", origin.toVector());
        int blockRadius = (int) (1.0 / spawnConfig.getBlendFallOff() + 4.0 + INITIAL_CHANCE);

        Material fillInBlock = findFillInBlock(destination);

        for(int z = -blockRadius; z < blockRadius; z++) {
            for(int y = -blockRadius; y < blockRadius; y++) {
                for(int x = -blockRadius; x < blockRadius; x++) {
                    Vector relativePos = new Vector(x, y, z);

                    double swapChance = calculateSwapChance(relativePos);
                    // Apply the random chance
                    if(random.nextDouble() > swapChance) {continue;}

                    Location originPos = origin.clone().add(relativePos);
                    Location destPos = destination.clone().add(applyRandomOffset(relativePos, 10.0));

                    Material originType = originPos.getBlock().getType();
                    Material destType = destPos.getBlock().getType();

                    if(!destType.isSolid()) {destType = fillInBlock;}

                    // Don't replace air or obsidian blocks so the portal doesn't get broken and we don't get blocks in the air.
                    boolean skip = false;
                    for(Material type : BLACKLISTED_COPY_BLOCKS) {
                        if(originType == type || destType == type) {
                            skip = true;
                            break;
                        }
                    }

                    if(skip) {continue;}

                    originPos.getBlock().setType(destType);
                }
            }
        }
    }

    /**
     * Moves each coordinate of <code>vec</code> a maximum of <code>power / 2</code> blocks higher or lower.
     * @param vec The vector to move
     * @param power The maximum deviation times two.
     * @return A new, offset vector.
     */
    private Vector applyRandomOffset(Vector vec, double power) {
        Vector other = new Vector();
        other.setX(vec.getX() + (random.nextDouble() - 0.5) * power);
        other.setY(vec.getY() + (random.nextDouble() - 0.5) * power);
        other.setZ(vec.getZ() + (random.nextDouble() - 0.5) * power);

        return other;
    }

    private double calculateSwapChance(Vector relativePos) {
        double distance = relativePos.length();
        return INITIAL_CHANCE - distance * spawnConfig.getBlendFallOff();
    }
}
