package com.lauriethefish.betterportals.bukkit.events;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.config.MiscConfig;
import com.lauriethefish.betterportals.bukkit.config.PortalSpawnConfig;
import com.lauriethefish.betterportals.bukkit.player.IPlayerData;
import com.lauriethefish.betterportals.bukkit.player.IPlayerDataManager;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * We cancel regular nether portal teleportation since we override it.
 */
public class PortalTeleportationEvents implements Listener {
    private final PortalSpawnConfig spawnConfig;
    private final IPortalManager portalManager;
    private final MessageConfig messageConfig;
    private final MiscConfig miscConfig;
    private final IPlayerDataManager playerDataManager;

    @Inject
    public PortalTeleportationEvents(IEventRegistrar eventRegistrar, IPortalManager portalManager, PortalSpawnConfig spawnConfig, MessageConfig messageConfig, MiscConfig miscConfig, IPlayerDataManager playerDataManager) {
        this.portalManager = portalManager;
        this.messageConfig = messageConfig;
        this.spawnConfig = spawnConfig;
        this.miscConfig = miscConfig;
        this.playerDataManager = playerDataManager;

        eventRegistrar.register(this);
    }

    private boolean isPluginNetherPortal(@NotNull Entity entity) {
        Vector maxPortalSize = spawnConfig.getMaxPortalSize();
        double portalExistenceRadius = Math.max(maxPortalSize.getX(), maxPortalSize.getY()) + 2;

        IPortal portal = portalManager.findClosestPortal(entity.getLocation(), portalExistenceRadius);
        return portal != null && portal.isNetherPortal();
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        if(isPluginNetherPortal(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {

        boolean isNetherPortal = event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL;
        if(isPluginNetherPortal(event.getPlayer()) && isNetherPortal) {
            event.setCancelled(true);
        }   else if(isNetherPortal) {
            if(spawnConfig.isWorldDisabled(event.getFrom().getWorld()) || spawnConfig.isWorldDisabled(event.getTo().getWorld())) {
                return;
            }

            // Send a warning for vanilla nether portals, since players might not realise that they need relighting
            IPlayerData playerData = playerDataManager.getPlayerData(event.getPlayer());
            if (!playerData.getPermanentData().getBoolean("createVanillaPortals")) {
                if (!(miscConfig.isShowVanillaPortalWarningFirstTimeOnly() && !playerData.getShowVanillaPortalWarning())) {
                    String warning = messageConfig.getWarningMessage("vanillaPortal");
                    if(!warning.isEmpty()) {
                        event.getPlayer().sendMessage(warning);
                    }
                    if (miscConfig.isShowVanillaPortalWarningFirstTimeOnly()) {
                        playerData.setShowVanillaPortalWarning(false);
                    }
                }
            }
        }
    }
}
