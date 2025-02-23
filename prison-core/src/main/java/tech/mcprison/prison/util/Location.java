/*
 *  Prison is a Minecraft plugin for the prison game mode.
 *  Copyright (C) 2017-2020 The Prison Team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.mcprison.prison.util;

import java.text.DecimalFormat;
import java.util.Optional;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.bombs.MineBombs.AnimationArmorStandItemLocation;
import tech.mcprison.prison.internal.ArmorStand;
import tech.mcprison.prison.internal.Entity;
import tech.mcprison.prison.internal.EntityType;
import tech.mcprison.prison.internal.World;
import tech.mcprison.prison.internal.block.Block;
import tech.mcprison.prison.internal.block.PrisonBlock;

/**
 * Represents a location in a Minecraft world.
 *
 * @author Faizaan A. Datoo
 * @since API 1.0
 */
public class Location {

    private transient World world;
    private String worldName;
    
    private double x, y, z;
    private float pitch, yaw;
    
    private Vector direction;
    
    private boolean isEdge;
    private boolean isCorner;

    public Location(World world, double x, double y, double z, float pitch, float yaw, Vector direction) {
    	this.world = world;
    	this.worldName = world == null ? null : world.getName();
    	
    	this.x = x;
    	this.y = y;
    	this.z = z;
    	this.pitch = pitch;
    	this.yaw = yaw;
    	this.direction = direction;
    }
    public Location(World world, double x, double y, double z, float pitch, float yaw) {
    	this( world, x, y, z, pitch, yaw, new Vector() );
    }

    public Location(World world, double x, double y, double z) {
    	this( world, x, y, z, 0.0f, 0.0f);
    }

    public Location(String worldName, int x, int y, int z) {
    	this( Prison.get().getPlatform().getWorld( worldName ).orElse( null ), (double) x, (double) y, (double) z );
    }
    
    public Location(Location clone) {
    	this( clone.getWorld(), clone.getX(), clone.getY(), clone.getZ(), 
    			clone.getPitch(), clone.getYaw(), clone.getDirection());
    }
    
    public Location() {
    }

    public Location clone() {
    	return new Location( this );
    }
    
    
	/**
	 * <p>This function should be called after loading a mine from 
	 * storage, and this function should reconnect all dynamic objects 
	 * that could not be stored with the core Mine data.
	 * </p>
	 * 
	 * <p>Examples: World objects.
	 * </p>
	 */
	public void reconnectObects() {
		if ( getWorld() == null ) {
			String worldName = getWorldName();
			
			Optional<World> worldOpt = Prison.get().getPlatform().getWorld(worldName);
			
			if ( worldOpt.isPresent() ) {
				World world = worldOpt.get();
				
				setWorld( world );
			}
		}
	}
    
    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public String getWorldName() {
		return worldName;
	}
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public Vector getDirection() {
		return direction;
	}
    
    /**
     * Sets the {@link #getYaw() yaw} and {@link #getPitch() pitch} to point
     * in the direction of the vector.
     * 
     * @param vector the direction vector
     * @return the same location
     */
	public void setDirection( Vector direction ) {
		this.direction = direction;
	}
    
	public int getBlockX() {
        return Math.toIntExact(Math.round(getX()));
    }

    public int getBlockY() {
        return Math.toIntExact(Math.round(getY()));
    }

    public int getBlockZ() {
        return Math.toIntExact(Math.round(getZ()));
    }

    public Block getBlockAt() {
        return world.getBlockAt(this);
    }
    
    public Block getBlockAt( boolean containsCustomBlocks ) {
    	return world.getBlockAt( this, containsCustomBlocks );
    }
    
    public void setBlockAsync( PrisonBlock prisonBlock ) {
    	world.setBlockAsync( prisonBlock, this );
    }

    /**
     * <p>The edge identifies if a block is along the edge, or corner, of a mine. 
     * It's these blocks that becomes the pink glass tracer blocks when 
     * when enabling the tracer on a mine.
     * </p>
     * 
     * @return
     */
    public boolean isEdge() {
		return isEdge;
	}
	public void setEdge( boolean isEdge ) {
		this.isEdge = isEdge;
	}
	
	public boolean isCorner() {
		return isCorner;
	}
	public void setCorner(boolean isCorner) {
		this.isCorner = isCorner;
	}
	
	@Override 
	public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location)) {
            return false;
        }

        Location location = (Location) o;

        return Double.compare(location.x, x) == 0 && Double.compare(location.y, y) == 0
            && Double.compare(location.z, z) == 0 && Float.compare(location.pitch, pitch) == 0
            && Float.compare(location.yaw, yaw) == 0 && (world != null ?
            world.getName().equals(location.world.getName()) :
            location.world == null);

    }

    @Override 
    public int hashCode() {
        int result;
        long temp;
        result = world != null ? world.hashCode() : 0;
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (pitch != +0.0f ? Float.floatToIntBits(pitch) : 0);
        result = 31 * result + (yaw != +0.0f ? Float.floatToIntBits(yaw) : 0);
        return result;
    }

    @Override 
    public String toString() {
    	
    	DecimalFormat dFmt = new DecimalFormat( "0.00" );
    	
        return "Location{" + "world=" + world.getName() + ", "
        		+ "x=" + dFmt.format(x) + ", "
        		+ "y=" + dFmt.format(y) + ", "
        		+ "z=" + dFmt.format(z) + ", "
        		+ "pitch=" + dFmt.format(pitch) + ", "
        		+ "yaw=" + dFmt.format(yaw) + '}';
    }

    /**
     * Returns the values in coordinate (x, y, z) format.
     *
     * @return The {@link String} containing coordinates.
     */
    public String toCoordinates() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    
    public String toWorldCoordinates() {
    	return "(" + world.getName() + "," + ((int) x) + "," + ((int) y) + "," + ((int) z) + ")";
    }

    public static Location decodeWorldCoordinates( String worldCoordinats ) {
    	Location results = null;
    	String[] d = worldCoordinats.replaceAll( "\\(|\\)", "" ).split( "," );
    	
    	if ( d != null && d.length == 4 ) {
    		results = new Location( d[0], Integer.parseInt( d[1] ), Integer.parseInt( d[2] ), Integer.parseInt( d[3] ) );
    	}
    	return results;
    }
    
    /**
     * Returns the values in coordinate (x, y, z) format, to the nearest block (i.e. no decimals).
     *
     * @return The {@link String} containing coordinates.
     */
    public String toBlockCoordinates() {
        return "(" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + ")";
    }
    
	public Location add( Vector direction )
	{
		Location results = new Location( this );
		results.setDirection( direction );
		
		results.setX( results.getX() + direction.getX() );
		results.setY( results.getY() + direction.getY() );
		results.setZ( results.getZ() + direction.getZ() );

		return results;
	}
	
	/** 
	 * This returns a vector based upon the current location.
	 * 
	 * Note that this is based upon org.bucket.location.Location.toVector() and
	 * it does not use yaw.
	 * 
	 * @return
	 */
	public Vector toVector() {
		Vector results = new Vector( getX(), getY(), getZ() );
		return results;
	}
	
	/**
	 * <p>This function will clone the current location object and then add/subtract the amount of
	 * x, y, and/or z to that location.  To keep the same value for one or more of these coordinates
	 * then use a value of zero.
	 * </p>
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Location getLocationAtDelta( int x, int y, int z )
	{
		Location results = new Location( this );
		
		results.setX( results.getX() + x );
		results.setY( results.getY() + y );
		results.setZ( results.getZ() + z );
		
		return results;
	}
	
	/**
	 * <p>Returns the block that is at the location of this object, offset by x, y, and/or z deltas.
	 * </p>
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Block getBlockAtDelta( int x, int y, int z )
	{
		Location results = getLocationAtDelta( x, y, z );
		
		return getWorld().getBlockAt( results );
	}
	
	public Entity spawnEntity( EntityType entityType ) {
		return getWorld().spawnEntity( this, entityType );
	}

	
	public ArmorStand spawnArmorStand() {
		return getWorld().spawnArmorStand( this );
	}
	
	public ArmorStand spawnArmorStand( String itemName, AnimationArmorStandItemLocation asLocation ) {
		
		return getWorld().spawnArmorStand( this, itemName, asLocation );
	}
	
	
	
}
