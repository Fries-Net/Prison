/*
 * Prison is a Minecraft plugin for the prison game mode.
 * Copyright (C) 2017 The Prison Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.mcprison.prison.mines.data;

import tech.mcprison.prison.internal.block.PrisonBlock.PrisonBlockType;
import tech.mcprison.prison.internal.block.PrisonBlockStatusData;
import tech.mcprison.prison.util.ObsoleteBlockType;

/**
 * Represents a block in a mine
 * 
 * @deprecated
 */
@SuppressWarnings("deprecation")
public class BlockOld
			extends PrisonBlockStatusData
			implements Comparable<BlockOld> {
	
	public static final BlockOld AIR = new BlockOld( ObsoleteBlockType.AIR );
	public static final BlockOld IGNORE = new BlockOld( ObsoleteBlockType.IGNORE );
	public static final BlockOld NULL_BLOCK = new BlockOld( ObsoleteBlockType.NULL_BLOCK );

    /**
     * The {@link BlockType} represented by this {@link BlockOld}
     */
    private ObsoleteBlockType type; // = BlockType.AIR;
    /**
     * The chance of this block appearing in it's associated mine
     */
//    private double chance; // = 100.0d;

    
    protected BlockOld( ObsoleteBlockType block ) {
    	this( block, 0.0d, 0L );
    }
    
    /**
     * Assigns the type and chance
     */
    public BlockOld(ObsoleteBlockType block, double chance, long blockCountTotal) {
    	super( PrisonBlockType.minecraft, (block == null ? BlockOld.AIR.getBlockName() : block.name()), chance, blockCountTotal);
    			
        this.type = block;
//        this.chance = chance;
    }

    public BlockOld(String blockType, double chance, long blockCountTotal) {
    	super( PrisonBlockType.minecraft, blockType, chance, blockCountTotal);
    	
//    	this.chance = chance;
    	
    	ObsoleteBlockType block = ObsoleteBlockType.fromString( blockType );
    	this.type = block;
    	// Update blockName since mapping to BlockType may result in a different name:
    	setBlockName( block.name() );
    	
    }
    
	@Override
	public String toString() {
		return getType().name() + " " + Double.toString( getChance() );
	}

	
	@Override
	public boolean equals( Object block ) {
		boolean results = false;

		if ( block != null && block instanceof BlockOld) {
			results = getType() == ((BlockOld) block).getType();
		}
		
		return results;
	}
	
	@Override
	public int compareTo( BlockOld block )
	{
		int results = 0;
		
		if ( block == null ) {
			results = 1;
		}
		else {
			results = getBlockName().compareToIgnoreCase( block.getBlockName() );
		}
			
		return results;
	}
	
	public ObsoleteBlockType getType()
	{
		return type;
	}
	public void setType( ObsoleteBlockType type )
	{
		this.type = type;
	}
	
	@Override
	public boolean isAir() {
		return compareTo( AIR ) == 0;
	}

//	public double getChance()
//	{
//		return chance;
//	}
//	public void setChance( double chance )
//	{
//		this.chance = chance;
//	}
}
