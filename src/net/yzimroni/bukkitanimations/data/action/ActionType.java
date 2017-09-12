package net.yzimroni.bukkitanimations.data.action;

public enum ActionType {
	// Blocks
	BLOCK_PLACE, UPDATE_BLOCKSTATE, BLOCK_BREAK_ANIMATION, BLOCK_BREAK,

	// Entities
	SPAWN_ENTITY, SHOOT_PROJECTILE, LIGHTNING_STRIKE, ENTITY_MOVE, UPDATE_ENTITY, REMOVE_EFFECT, ENTITY_ITEM_USE,
	ENTITY_PICKUP, ENTITY_DAMAGE, ENTITY_DEATH, DESPAWN_ENTITY,

	// Players
	PLAYER_ANIMATION,

	// World
	WORLD_EFFECT, PARTICLE, EXPLOSION, SOUND;
}
