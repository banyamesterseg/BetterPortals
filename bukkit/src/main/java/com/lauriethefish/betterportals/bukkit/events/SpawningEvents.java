package com.lauriethefish.betterportals.bukkit.events;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.api.PortalDirection;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.config.PortalSpawnConfig;
import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import com.lauriethefish.betterportals.bukkit.player.IPlayerData;
import com.lauriethefish.betterportals.bukkit.player.IPlayerDataManager;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import com.lauriethefish.betterportals.bukkit.portal.spawning.IPortalSpawner;
import com.lauriethefish.betterportals.bukkit.portal.spawning.PortalSpawnPosition;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.util.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class SpawningEvents implements Listener {
    // In 1.12 and 1.13 there's no way to get the entity that lit a portal, so we just send it to all players within a range.
    private static final double PORTAL_WARNING_SEND_RANGE = 10;

    private static final Method GET_BLOCKS_METHOD;
    private static final Method GET_ENTITY_METHOD;

    static {
        GET_BLOCKS_METHOD = ReflectionUtil.findMethod(PortalCreateEvent.class, "getBlocks");
        if(VersionUtil.isMcVersionAtLeast("1.14.0")) {
            GET_ENTITY_METHOD = ReflectionUtil.findMethod(PortalCreateEvent.class, "getEntity");
        }   else    {
            GET_ENTITY_METHOD = null;
        }
    }

    private final IPortalSpawner portalSpawnChecker;
    private final IPortalManager portalManager;
    private final IPortal.Factory portalFactory;
    private final PortalSpawnConfig spawnConfig;
    private final MessageConfig messageConfig;
    private final Logger logger;
    private final IPlayerDataManager playerDataManager;

    @Inject
    public SpawningEvents(IEventRegistrar eventRegistrar, IPortalSpawner portalSpawnChecker, IPortalManager portalManager, IPortal.Factory portalFactory, PortalSpawnConfig spawnConfig, MessageConfig messageConfig, Logger logger, IPlayerDataManager playerDataManager) {
        this.portalSpawnChecker = portalSpawnChecker;
        this.portalManager = portalManager;
        this.portalFactory = portalFactory;
        this.spawnConfig = spawnConfig;
        this.messageConfig = messageConfig;
        this.logger = logger;
        this.playerDataManager = playerDataManager;

        eventRegistrar.register(this);
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        // Some worlds can be configured to use vanilla portal logic
        if(spawnConfig.isWorldDisabled(event.getWorld())) {return;}
        if(event.getReason() != PortalCreateEvent.CreateReason.FIRE) {return;}
        
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            IPlayerData playerData = playerDataManager.getPlayerData((Player) entity);
            if (playerData.getPermanentData().getBoolean("createVanillaPortals")) {
                return;
            }
        }

        Vector highPosition = null;
        Vector lowPosition = null;

        // For some reason the event sometimes gets called with no blocks completely randomly
        List<?> blocks = (List<?>) ReflectionUtil.invokeMethod(event, GET_BLOCKS_METHOD);
        if(blocks.size() == 0) {return;}

        // Find the highest and lowest position to calculate the portal size
        for(Object obj : blocks) {
            // We have to do it this way since this is either an array of Block on 1.13 and under, or BlockState on 1.14 and up
            Block block;
            if(obj instanceof BlockState)   {
                block = ((BlockState) obj).getBlock();
            }   else    {
                block = (Block) obj;
            }

            if(block.getType() == Material.OBSIDIAN) {
                continue;
            }

            Vector position = block.getLocation().toVector();
            if(highPosition == null || MathUtil.greaterThanEq(position, highPosition)) {
                highPosition = position;
            }
            if(lowPosition == null || MathUtil.lessThanEq(position, lowPosition)) {
                lowPosition = position;
            }
        }

        assert highPosition != null;
        PortalDirection direction = findPortalDirection(highPosition, lowPosition);
        Vector size = findPortalSize(highPosition, lowPosition, direction);

        // Enforce the portal size limit. Tell the player to avoid confusion
        Vector maxSize =  spawnConfig.getMaxPortalSize();
        if(!MathUtil.lessThanEq(size, maxSize)) {
            event.setCancelled(true);
            logger.fine("Not spawning portal - too big: %s", size);

            // Make sure to replace the current size place holder
            String msg = messageConfig.getWarningMessage("portalTooBig");
            msg = msg.replace("{size}", String.format("%dx%d", maxSize.getBlockX(), maxSize.getBlockY()));
            sendMessageToLighter(event, msg);
            return;
        }

        // The bottom left portal block is one to the right and up of the bottom left frame block
        Location bottomLeftLocation = lowPosition.toLocation(event.getWorld());
        bottomLeftLocation.subtract(direction.swapVector(new Vector(1.0, 1.0, 0.0)));

        PortalSpawnPosition originPosition = new PortalSpawnPosition(bottomLeftLocation, size, direction);
        logger.fine("Attempting to spawn portal with origin %s", originPosition);

        boolean successful = portalSpawnChecker.findAndSpawnDestination(
                bottomLeftLocation,
                size,
                (destination) -> registerPortals(originPosition, destination, size)
        );

        // If unsuccessful (thrown when worlds are not linked), log for the purposes of debugging
        if(!successful) {
            logger.fine("Spawning was unsuccessful, blocking portal blocks from appearing!");
            sendMessageToLighter(event, messageConfig.getWarningMessage("noWorldLink"));
            event.setCancelled(true);
        }
    }

    // Registers both directions of the nether portal
    private void registerPortals(PortalSpawnPosition origin, PortalSpawnPosition destination, Vector size) {
        IPortal portal = portalFactory.create(origin.toPortalPosition(), destination.toPortalPosition(), size, false, UUID.randomUUID(), null, null, true);
        IPortal reversePortal = portalFactory.create(destination.toPortalPosition(), origin.toPortalPosition(), size, false, UUID.randomUUID(), null, null, true);

        portalManager.registerPortal(portal);
        portalManager.registerPortal(reversePortal);
    }

    @NotNull
    private Vector findPortalSize(Vector highPosition, Vector lowPosition, PortalDirection direction) {
        Vector portalSize = direction.swapVector(highPosition.clone().subtract(lowPosition));
        portalSize.add(new Vector(1.0, 1.0, 0.0)); // Add one since the top-right block's position isn't actually in the top-right of the block

        assert MathUtil.greaterThanEq(portalSize, new Vector(2.0, 3.0, 0.0)) : "PortalCreateEvent called on a portal under 2x3 in size";
        return portalSize;
    }

    @NotNull
    private PortalDirection findPortalDirection(Vector highPosition, Vector lowPosition) {
        if(highPosition.getX() == lowPosition.getX()) {return PortalDirection.EAST;}
        if(highPosition.getZ() == lowPosition.getZ()) {return PortalDirection.NORTH;}

        throw new IllegalStateException("Invalid PortalCreateEvent called by Bukkit. Portal was not on a valid plane");
    }


    /**
     * Sends a message to whoever can be best approximated as the portal lighter
     * Does nothing if given an empty message
     * @param event Event to find the lighter of
     * @param message Message to send. This will do nothing if it's empty
     */
    private void sendMessageToLighter(@NotNull PortalCreateEvent event, @NotNull String message) {
        if(message.isEmpty()) {return;}

        if(VersionUtil.isMcVersionAtLeast("1.14.0")) {
            // Yay! convenient method exists in these versions
            Entity lighter = (Entity) ReflectionUtil.invokeMethod(event, GET_ENTITY_METHOD);
            if(lighter instanceof Player) {
                lighter.sendMessage(message);
            }

            if(lighter != null) {
                return;
            }
        }

        // Loop through the nearby entities to find who to send the message to
        List<?> blocks = (List<?>) ReflectionUtil.invokeMethod(event, GET_BLOCKS_METHOD);
        Object firstBlock = blocks.get(0);

        Location portalLocation;
        if(firstBlock instanceof Block) {
            portalLocation = ((Block) firstBlock).getLocation();
        }   else if(firstBlock instanceof BlockState) {
            portalLocation = ((BlockState) firstBlock).getLocation();
        }   else    {
            return;
        }

        Collection<Entity> nearby = event.getWorld().getNearbyEntities(portalLocation, PORTAL_WARNING_SEND_RANGE, PORTAL_WARNING_SEND_RANGE, PORTAL_WARNING_SEND_RANGE);

        for(Entity entity : nearby) {
            if(entity instanceof Player) {
                entity.sendMessage(message);
            }
        }
    }
}
