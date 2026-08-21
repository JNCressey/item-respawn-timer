package com.itemrespawntimer.worldhopper;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.WorldUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Singleton
public class WorldHopper {
    //region head
    @Inject
    private Client client;

    @Inject
    private ItemRespawnTimerConfig config;

    @Inject
    private WorldService worldService;

    @Inject
    private ActiveTimers activeTimers;

    @Inject
    private KeyManager keyManager;

    @Inject
    private ClientThread clientThread;
    //endregion


    //region hotkeys
    public void registerKeyListeners() {
        keyManager.registerKeyListener(quickHopTopListener);
        keyManager.registerKeyListener(quickHopNextListener);
    }


    private final HotkeyListener quickHopTopListener = new HotkeyListener(() -> config.hotkeyQuickHopTop())
    {
        @Override
        public void hotkeyPressed()
        {
            clientThread.invoke(() -> hopTop());
        }
    };


    private final HotkeyListener quickHopNextListener = new HotkeyListener(() -> config.hotkeyQuickHopNext())
    {
        @Override
        public void hotkeyPressed()
        {
            clientThread.invoke(() -> hopNext());
        }
    };
    //endregion


    //region timersToSkip
    /**
     * The timers visited by quick hopping.
     */
    private final Set<RespawnTimer> timersToSkip = new HashSet<>();

    private final Set<Integer> worldsToAddToSkipAfterHop = new HashSet<>();

    
    private void addTimersToSkip(int hoppedToWorldId){
        timersToSkip.removeIf(RespawnTimer::isDeleted);
        activeTimers.getActiveTimers().stream()
                .filter(timer -> timer.getWorldId()==hoppedToWorldId)
                .forEach(timersToSkip::add);
    }

    private void addTimersToSkip(){
        timersToSkip.removeIf(RespawnTimer::isDeleted);
        activeTimers.getActiveTimers().stream()
                .filter(timer -> worldsToAddToSkipAfterHop.has(timer.getWorldId()))
                .forEach(timersToSkip::add);
        worldsToAddToSkipAfterHop.clear();
    }


    public void joinedWorld(){
        int currentWorldId = client.getWorld();
        worldsToAddToSkipAfterHop.add(currentWorldId);
        addTimersToSkip();
    }
    //endregion


    /**
     * Hop to the world that is at the top of the timers list (skipping timers for the current world).
     * @see #hop(int)
     */
    private void hopTop(){
        timersToSkip.clear();
        if (activeTimers.getActiveTimers().isEmpty()) { return; }

        int currentWorldId = client.getWorld();
        int firstTimerWorldId = activeTimers.getActiveTimers().getFirst().getWorldId();
        if (firstTimerWorldId!=currentWorldId){
            hop(firstTimerWorldId);
        } else {
            addTimersToSkip(firstTimerWorldId);
            findNextWorld().ifPresent(this::hop);
        }
    }


    /**
     * Hop to the world that is next in the timers list (skipping visited timers and current world).
     * Or, if all timers have been visisted, hop as by {@link #hopTop}.
     * @see #hop(int)
     */
    private void hopNext(){
        findNextWorld()
                .ifPresentOrElse(
                        this::hop,
                        this::hopTop
                );
    }

    private void hopToPositionInTimerList(int index){
        timers = activeTimers.getActiveTimers();
        if (timers.size() < index + 1){ return; }
        
        int currentWorldId = client.getWorld();
        int targetWorldId = timers.get(index).getWorldId();
        if (targetWorldId == currentWorldId){ return: }

        worldsToAddToSkipAfterHop.add(currentWorldId);
        timers.subList(0,index).stream()
            .forEach(worldsToAddToSkipAfterHop::add);
        
        hop(targetWorld);
            
    }

    


    /**
     * Find the next world that isn't the current world or in {@link #timersToSkip}.
     * @return the worldId if such exists
     */
    private Optional<Integer> findNextWorld(){
        int currentWorldId = client.getWorld();
        return activeTimers.getActiveTimers().stream()
                .filter(timer -> timer.getWorldId()!=currentWorldId)
                .filter(timer -> !timersToSkip.contains(timer))
                .findFirst()
                .map(RespawnTimer::getWorldId);
    }


    //region hop(int worldId)
    /**
     * Get world from worldId and if present add timers to {@link #timersToSkip} and hop as {@link #hop(net.runelite.api.World)}
     * @param worldId the world to hop to
     */
    private void hop(int worldId){
        Optional<World> world = Optional.ofNullable(getWorldFromId(worldId));

        world.ifPresent(this::hop);
    }


    /**
     * hop to world or change world if at login screen
     * @param world the world to hop to
     */
    private void hop(@Nonnull net.runelite.api.World world){
        int currentWorldId = client.getWorld();
        worldsToAddToSkipAfterHop.add(currentWorldId);
        
        assert client.isClientThread();
        if (client.getGameState() == GameState.LOGIN_SCREEN) {
            client.changeWorld(world);
        } else {
            client.hopToWorld(world);
        }
    }


    /**
     *
     * @param worldId The world to get.
     * @return The world, or null if no such world.
     */
    @Nullable
    private net.runelite.api.World getWorldFromId(int worldId){
        return Optional.ofNullable(worldService.getWorlds())
                .map(r -> r.findWorld(worldId))
                .map(world -> { // convert to the other world type
                    final net.runelite.api.World rsWorld = client.createWorld();
                    rsWorld.setActivity(world.getActivity());
                    rsWorld.setAddress(world.getAddress());
                    rsWorld.setId(world.getId());
                    rsWorld.setPlayerCount(world.getPlayers());
                    rsWorld.setLocation(world.getLocation());
                    rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
                    return rsWorld;
                })
                .orElse(null);
    }
    //endregion

}
