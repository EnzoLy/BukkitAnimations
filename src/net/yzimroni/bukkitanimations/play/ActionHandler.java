package net.yzimroni.bukkitanimations.play;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;
import net.yzimroni.bukkitanimations.BukkitAnimationsPlugin;
import net.yzimroni.bukkitanimations.data.action.ActionData;
import net.yzimroni.bukkitanimations.data.action.ActionType;
import net.yzimroni.bukkitanimations.data.manager.MinecraftDataManagers;
import net.yzimroni.bukkitanimations.utils.NMSUtils;
import net.yzimroni.bukkitanimations.utils.Utils;

public class ActionHandler {

	private static final HashMap<ActionType, BiConsumer<ReplayingSession, ActionData>> HANDLERS = new HashMap<>();

	static {
		registerDefaultHandlers();
	}

	@SuppressWarnings("unchecked")
	private static void registerDefaultHandlers() {
		register(ActionType.BLOCK_BREAK, (s, a) -> {
			a.getLocation(s).getBlock().setType(Material.AIR);
		});
		register(ActionType.BLOCK_PLACE, (s, a) -> {
			Block b = a.getLocation(s).getBlock();
			MinecraftDataManagers.getBlocks().load(a, b);
			MinecraftDataManagers.getBlocks().load(a, b.getState());
		});
		register(ActionType.UPDATE_BLOCKSTATE, (s, a) -> {
			Block b = a.getLocation(s).getBlock();
			MinecraftDataManagers.getBlocks().load(a, b.getState());
			b.getState().update(true);
		});
		register(ActionType.LIGHTNING_STRIKE, (s, a) -> {
			Location l = a.getLocation(s);
			l.getWorld().strikeLightningEffect(l);
		});

		register(ActionType.SPAWN_ENTITY, (s, a) -> {
			Location location = a.getLocation(s);
			String name = (String) a.getData("name");
			EntityType type = EntityType.valueOf((String) a.getData("type"));
			NPC npc = Utils.NPCREGISTRY.createNPC(type, name);

			int entityId = a.getEntityId();

			npc.spawn(location);
			MinecraftDataManagers.getEntities().load(a, npc.getEntity());
			Entity e = npc.getEntity();

			s.getEntityTracker().addOldToNewId(entityId, e.getEntityId());
			s.getEntityTracker().addNPC(npc);
		});

		register(ActionType.SHOOT_PROJECTILE, (s, a) -> {
			int entityId = a.getEntityId();
			int shooterId = a.getInt("shooterId");
			Entity shooter = s.getEntityTracker().getEntityForOldId(shooterId);
			EntityType type = EntityType.valueOf((String) a.getData("type"));
			Vector velocity = Vector.deserialize((Map<String, Object>) a.getData("velocity"));

			Projectile projectile = null;
			if (shooter != null && shooter instanceof ProjectileSource) {
				projectile = ((ProjectileSource) shooter)
						.launchProjectile((Class<? extends Projectile>) type.getEntityClass(), velocity);
			} else {
				Location location = a.getLocation(s);
				if (type == EntityType.ARROW) {
					projectile = location.getWorld().spawnArrow(location, velocity, (float) 0.6, 12);
				} else {
					projectile = (Projectile) location.getWorld().spawnEntity(location, type);
				}
			}
			MinecraftDataManagers.getEntities().load(a, projectile);
			s.getEntityTracker().addOldToNewId(entityId, projectile.getEntityId());
			s.getEntityTracker().addEntity(projectile);
		});

		register(ActionType.ENTITY_MOVE, (s, a) -> {
			int entityId = a.getEntityId();
			Location location = a.getLocation(s);
			Entity e = s.getEntityTracker().getEntityForOldId(entityId);
			e.teleport(location);
			if (e.hasMetadata("NPC")) {
				NMS.setHeadYaw(NMS.getHandle(e), location.getYaw());
			}
		});
		register(ActionType.ENTITY_DAMAGE, (s, a) -> {
			int entityId = a.getEntityId();
			Entity e = s.getEntityTracker().getEntityForOldId(entityId);
			e.playEffect(EntityEffect.HURT);
		});

		register(ActionType.PLAYER_ANIMATION, (s, a) -> {
			int entityId = a.getEntityId();
			Entity e = s.getEntityTracker().getEntityForOldId(entityId);
			PlayerAnimationType type = PlayerAnimationType.valueOf((String) a.getData("type"));
			if (type == PlayerAnimationType.ARM_SWING) {
				PlayerAnimation.ARM_SWING.play((Player) e);
			}
		});
		register(ActionType.ENTITY_PICKUP, (s, a) -> {
			int entityId = a.getEntityId();
			Entity pickedUp = s.getEntityTracker().getEntityForOldId(entityId);
			int playerId = a.getInt("playerId");
			Entity player = s.getEntityTracker().getEntityForOldId(playerId);
			if (player != null) {
				NMSUtils.pickUp(pickedUp, (Player) player);
			}
			handle(s, new ActionData(ActionType.DESPAWN_ENTITY).data("entityId", entityId));
		});
		register(ActionType.UPDATE_ENTITY, (s, a) -> {
			int entityId = a.getEntityId();
			Entity update = s.getEntityTracker().getEntityForOldId(entityId);
			MinecraftDataManagers.getEntities().load(a, update);
		});
		register(ActionType.UPDATE_EQUIPMENT, (s, a) -> {
			int entityId = a.getEntityId();
			Entity e = s.getEntityTracker().getEntityForOldId(entityId);
			MinecraftDataManagers.getEntities().load(a, e);
		});

		register(ActionType.ENTITY_DEATH, (s, a) -> {
			int entityId = a.getEntityId();
			Entity e = s.getEntityTracker().getEntityForOldId(entityId);
			e.playEffect(EntityEffect.DEATH);
			Bukkit.getScheduler().scheduleSyncDelayedTask(BukkitAnimationsPlugin.get(),
					() -> handle(s, new ActionData(ActionType.DESPAWN_ENTITY).data("entityId", entityId)), 20);
		});
		register(ActionType.DESPAWN_ENTITY, (s, a) -> {
			int entityId = ((Number) a.getData("entityId")).intValue();
			Entity e = s.getEntityTracker().getEntityForOldId(entityId);
			if (e == null) {
				return;
			}
			NPC npc = s.getEntityTracker().getNPC(e.getEntityId());
			e.remove();
			if (npc != null) {
				npc.despawn();
			}
			s.getEntityTracker().removeEntity(e.getEntityId());
			s.getEntityTracker().removeOldToNewId(entityId);
		});

		register(ActionType.WORLD_EFFECT, (s, a) -> {
			Effect effect = Effect.valueOf((String) a.getData("effect"));
			Location location = a.getLocation(s);
			int data = a.getInt("data");
			location.getWorld().playEffect(location, effect, data);
		});
	}

	public static void register(ActionType type, BiConsumer<ReplayingSession, ActionData> handler) {
		if (HANDLERS.containsKey(type)) {
			HANDLERS.remove(type);
		}
		HANDLERS.put(type, handler);
	}

	public static void handle(ReplayingSession session, ActionData action) {
		Optional.ofNullable(HANDLERS.get(action.getType()))
				.orElseThrow(() -> new IllegalStateException("Handler not found for ActionType " + action.getType()))
				.accept(session, action);
	}

}
