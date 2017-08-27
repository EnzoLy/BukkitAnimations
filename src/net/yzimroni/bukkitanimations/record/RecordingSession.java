package net.yzimroni.bukkitanimations.record;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.gson.Gson;

import net.yzimroni.bukkitanimations.BukkitAnimationsPlugin;
import net.yzimroni.bukkitanimations.data.Animation;
import net.yzimroni.bukkitanimations.data.action.ActionData;
import net.yzimroni.bukkitanimations.data.action.ActionType;
import net.yzimroni.bukkitanimations.utils.Utils;

public class RecordingSession {

	private Animation animation;
	private boolean running;

	private int tick = 1;

	private Location minLocation;
	private Location maxLocation;

	private EventRecorder eventRecorder;

	private List<ActionData> actions = new ArrayList<ActionData>();
	private List<Integer> trackedEntities = new ArrayList<Integer>();

	public RecordingSession(String name, UUID uuid, Location min, Location max) {
		this.animation = new Animation(name, uuid);
		Preconditions.checkArgument(min.getWorld().equals(max.getWorld()), "World must be same");
		this.minLocation = new Location(min.getWorld(), Math.min(min.getBlockX(), max.getBlockX()),
				Math.min(min.getBlockY(), max.getBlockY()), Math.min(min.getBlockZ(), max.getBlockZ()));
		this.maxLocation = new Location(min.getWorld(), Math.max(min.getBlockX(), max.getBlockX()),
				Math.max(min.getBlockY(), max.getBlockY()), Math.max(min.getBlockZ(), max.getBlockZ()));

	}

	public void start() {
		if (isRunning()) {
			return;
		}
		running = true;
		eventRecorder = new EventRecorder(this);
		Bukkit.getPluginManager().registerEvents(eventRecorder, BukkitAnimationsPlugin.get());
		onStart();
		RecordingManager.get().onStart(this);
	}

	private void onStart() {
		minLocation.getWorld().getEntities().stream().filter(e -> isInside(e.getLocation())).forEach(e -> {
			if (e instanceof Player) {
				Player p = (Player) e;

				ActionData action = new ActionData(ActionType.SPAWN_PLAYER).spawnEntity(p);
				trackedEntities.add(p.getEntityId());
				addAction(action);
			}

		});
	}

	public boolean isInside(Location location) {
		return Utils.isInside(location, minLocation, maxLocation);
	}

	public void addAction(ActionData action) {
		if (action.getTick() == -1) {
			action.setTick(tick);
		}
		actions.add(action);
		System.out.println(action);
	}

	protected void tick() {
		if (!isRunning()) {
			return;
		}
		tick++;
	}

	public boolean isEntityTracked(int id) {
		return trackedEntities.contains(id);
	}

	public void addTrackedEntity(int id) {
		trackedEntities.add(id);
	}

	public void removeTrackedEntity(int id) {
		// If called without "new Integer", it will call List#remove(int index)
		trackedEntities.remove(new Integer(id));
	}

	public void stop() {
		if (!isRunning()) {
			return;
		}
		if (eventRecorder != null) {
			HandlerList.unregisterAll(eventRecorder);
			eventRecorder = null;
		}
		running = false;
		RecordingManager.get().onStop(this);

		try {
			Files.write(new Gson().toJson(actions), new File(animation.getName() + ".mcanimation"),
					Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isRunning() {
		return running;
	}

	public Animation getAnimation() {
		return animation;
	}

}
