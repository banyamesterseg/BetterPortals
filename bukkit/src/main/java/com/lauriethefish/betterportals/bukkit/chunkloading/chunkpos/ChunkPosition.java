package com.lauriethefish.betterportals.bukkit.chunkloading.chunkpos;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.ReflectUtils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

// A wrapper over the coordinates of a chunk that automatically converts them to the NMS version
// This can also just be used to hold the coordinates of a chunk
// Contains various utility functions for dealing with chunk coordinates
@Getter 
@Setter
public class ChunkPosition implements Cloneable {
    public World world; // This can be null
    public int x;
    public int z;

    public ChunkPosition(World world, int chunkX, int chunkZ) {
        this.world = world;
        x = chunkX;
        z = chunkZ;
    }

    public ChunkPosition(Location location) {
        x = location.getBlockX() >> 4;
        z = location.getBlockZ() >> 4;
        world = location.getWorld();
    }

    public ChunkPosition(Vector location) {
        x = location.getBlockX() >> 4;
        z = location.getBlockZ() >> 4;
    }

    public ChunkPosition(Chunk chunk)   {
        x = chunk.getX();
        z = chunk.getZ();
        world = chunk.getWorld();
    }

    public Chunk getChunk() {
        return world.getChunkAt(x, z);
    }

    public static class ChunkAreaIterator implements Iterator<ChunkPosition>, Iterable<ChunkPosition>, Cloneable {
        private ChunkPosition low;
        private ChunkPosition high;
        private ChunkPosition currentPos;

        private ChunkAreaIterator(ChunkPosition low, ChunkPosition high) {
            this.low = low; this.high = high;
            currentPos = low.clone();
        }

        @Override
        public boolean hasNext() {
            return currentPos.x < high.x || currentPos.z < high.z;
        }

        @Override
        public ChunkPosition next() {
            if(currentPos.x < high.x) {
                currentPos.x++; // If we are not at the end of a row, move us 1 across
            }   else if(currentPos.z < high.z) { // If we are at the end of a row, but there a columns left
                // Increment the column, and set the row to the start
                currentPos.z++;
                currentPos.x = low.x;
            }   else    {
                throw new NoSuchElementException();
            }

            return currentPos.clone();
        }

        // Returns a new area iterator with the same initial parameters as this one
        @Override
        public ChunkAreaIterator clone() {
            return new ChunkAreaIterator(low, high);
        }

        // Adds all of the chunks in this area to a set
        public void addAll(Set<ChunkPosition> set) {
            while(this.hasNext()) {
                set.add(this.next());
            }
        }

        // Revoves all of the chunks in this area to a set
        public void removeAll(Set<ChunkPosition> set) {
            while(this.hasNext()) {
                set.remove(this.next());
            }
        }

        @Override
        public Iterator<ChunkPosition> iterator() {
            return this;
        }
    }

    public static ChunkAreaIterator areaIterator(Location a, Location b)   {
        // Find the coordinates of the two locations
        ChunkPosition low = new ChunkPosition(a);
        ChunkPosition high = new ChunkPosition(b);

        return areaIterator(low, high);
    }

    public static ChunkAreaIterator areaIterator(ChunkPosition low, ChunkPosition high) {
        return new ChunkAreaIterator(low, high);
    }

    // Makes an NMS ChunkCoordIntPair from this object
    public Object toNMS()   {
        return ReflectUtils.newInstance("ChunkCoordIntPair", new Class[]{int.class, int.class}, new Object[]{x, z});
    }

    // Finds if this chunk has been generated before. This has the side effect of loading the chunk if it exists
    // Throws NullPointerException if world is null
    public boolean isGenerated()    {
        return world.loadChunk(x, z, false);
    }

    // Checks if the chunks at these coordinates is currently loaded
    public boolean isLoaded() {
        return world.isChunkLoaded(x, z);
    }

    // Gets the location of the bottom left position of this chunk
    public Location getBottomLeft() {
        return new Location(world, x * 16, 0, z * 16);
    }

    // Automatically generated
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ChunkPosition)) {
            return false;
        }
        ChunkPosition chunkPosition = (ChunkPosition) o;
        return x == chunkPosition.x && z == chunkPosition.z;
    }

    @Override
    public String toString()    {
        return String.format("x: %d, z: %d", x, z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public ChunkPosition clone() {
        return new ChunkPosition(world, x, z);
    }
}