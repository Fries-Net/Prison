package tech.mcprison.prison.spigot.game.entity;

import com.cryptomorin.xseries.XEntityType;

import tech.mcprison.prison.internal.EntityType;

public class SpigotEntityType
	extends EntityType {
	
	private org.bukkit.entity.EntityType bEntityType;
	private XEntityType xEType;
	
	public SpigotEntityType( String entityType ) {
		super( entityType );
		
	}
	public SpigotEntityType( org.bukkit.entity.EntityType bEntityType ) {
		super( "" );
		
		this.bEntityType = bEntityType;
		
		XEntityType xEType = XEntityType.of( bEntityType );
		this.xEType = xEType;
		
		setEntityType( xEType.name() );
	}
	public SpigotEntityType( XEntityType xEType ) {
		super( xEType.name() );
		
		this.xEType = xEType;
		
		this.bEntityType = xEType.get();
	}
	
	public static SpigotEntityType getSpigotEntityType( EntityType entityType ) {
		SpigotEntityType results = null;
		
		XEntityType xEType = XEntityType.of( entityType.getEntityType().toUpperCase() ).orElse( null );

		if ( xEType != null ) {
			results = new SpigotEntityType( xEType );
		}
		
		return results;
	}
	
	public org.bukkit.entity.EntityType getbEntityType() {
		return bEntityType;
	}
	public void setbEntityType(org.bukkit.entity.EntityType bEntityType) {
		this.bEntityType = bEntityType;
	}
	
	public XEntityType getxEType() {
		return xEType;
	}
	public void setxEType(XEntityType xEType) {
		this.xEType = xEType;
	}

}
