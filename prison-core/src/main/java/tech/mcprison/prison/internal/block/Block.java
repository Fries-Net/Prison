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

package tech.mcprison.prison.internal.block;

import java.util.List;

import tech.mcprison.prison.internal.ItemStack;
import tech.mcprison.prison.util.Location;

/**
 * Represents a block. Only one block may exist for a location in the world.
 *
 * @author Faizaan A. Datoo
 * @since API 1.0
 */
public interface Block {

    /**
     * Returns the location that this block represents.
     *
     * @return The {@link Location} of the block.
     */
    public Location getLocation();

    /**
     * Returns the {@link Block} at the position relative to this one.
     *
     * @param face The {@link BlockFace} that the relative block touches.
     * @return The {@link Block} relative to this one.
     */
    public Block getRelative(BlockFace face);

//    /**
//     * Returns the type of this block.
//     *
//     * @return The {@link BlockType}.
//     */
//    BlockType getType();
    
    
    public PrisonBlock getPrisonBlock();
    

//    /**
//     * Sets the block to a different type.
//     *
//     * @param type The new {@link BlockType}.
//     */
//    void setType(BlockType type);
    
    
    public void setPrisonBlock( PrisonBlock prisonBlock );
    
    /**
     * This function will set the current BlockFace for the block.
     * This is needed to properly set blocks such as ladders.
     * 
     * @param blockFace
     */
    public void setBlockFace( BlockFace blockFace );

    
    /**
     * Returns a snapshot of the state of this block, which you can edit to your liking.
     *
     * @return The current {@link BlockState}.
     */
    public BlockState getState();

    /**
     * Returns whether the block is empty (i.e. the type is air).
     *
     * @return true if the block is empty, false otherwise.
     */
    default boolean isEmpty() {
        return getPrisonBlock().isAir();
    }

    /**
     * Breaks the block as if the player has broken it, with drops included.
     *
     * @return true if the is broken, false otherwise.
     */
    public boolean breakNaturally();

    /**
     * Returns a list of items which would be dropped by destroying this block.
     *
     * @return A list of dropped items for this type of block
     */
    public List<ItemStack> getDrops();

    /**
     * Returns a list of items which would be dropped by destroying this block with
     * a specific tool.
     *
     * @param tool The tool or item in hand used for breaking the block.
     * @return A list of dropped items for this type of block.
     */
    public List<ItemStack> getDrops(ItemStack tool);

    
	public String getBlockName();
	
	public void setBlockName( String blockName );
	
	
	/**
	 * <p>The sale price is the amount that is paid to the player when they
	 * sell their items. A null indicates that no price was set.
	 * </p>
	 * 
	 * @return
	 */
	public Double getSalePrice();
	
	/**
	 * <p>The sale price is the amount that is paid to the player when they
	 * sell their items. A null indicates that no price was set.
	 * </p>
	 * 
	 * @param price
	 */
	public void setSalePrice( Double price );
	
	/**
	 * <p>The purchase price is what player pays to purchase the items.
	 *  A null indicates that no price was set.
	 * </p>
	 * 
	 * @return
	 */
	public Double getPurchasePrice();
	
	/**
	 * <p>The purchase price is what player pays to purchase the items.,
	 *  A null indicates that no price was set.
	 * </p>
	 * 
	 * @param price
	 */
	public void setPurchasePrice( Double price );
	
	
}
