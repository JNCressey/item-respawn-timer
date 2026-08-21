package com.itemrespawntimer.worldhopper;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.WorldUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;


@Slf4j
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

    @Inject
    private ChatMessageManager chatMessageManager;
    //endregion


    //region hotkeys

    /**
     * register hotkeys
     */
    public void startUp() {
        keyManager.registerKeyListener(quickHopTopListener);
        keyManager.registerKeyListener(quickHopNextListener);
    }

    /**
     * unregister hotkeys
     */
    public void shutDown(){
        keyManager.unregisterKeyListener(quickHopTopListener);
        keyManager.unregisterKeyListener(quickHopNextListener);
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

    @SuppressWarnings("SpellCheckingInspection")
    private static final String HOPPER_COMMAND_STRING = "irthop";
    private static final String HOPPER_COMMAND_TOP_STRING = HOPPER_COMMAND_STRING + "top";
    private static final String HOPPER_COMMAND_NEXT_STRING = HOPPER_COMMAND_STRING + "next";

    //todo can i make this subscribed?
    public void onCommandExecuted(CommandExecuted commandExecuted) {
        if (HOPPER_COMMAND_STRING.equalsIgnoreCase(commandExecuted.getCommand()))
        {
            String[] arguments = commandExecuted.getArguments();

            if (
                    arguments.length==0 // if no argument, hop next
                            ||"next".equals(arguments[0]) //if argument is "next", top next
            ){
                hopNext();
                return;
            }

            if ("top".equals(arguments[0])){ // if argument is "top", hop top
                hopTop();
                return;
            }

            int index;
            try
            {
                index = Integer.parseInt(arguments[0]);
            }
            catch (NumberFormatException e) // warn of bad argument
            {
                String message = new ChatMessageBuilder()
                        .append("ItemRespawnTimer command usage:")
                        .append(String.format("\n::%s [index]", HOPPER_COMMAND_STRING      ))
                        .append(String.format("\n::%s top",     HOPPER_COMMAND_STRING      ))
                        .append(String.format("\n::%s next",    HOPPER_COMMAND_STRING      ))
                        .append(String.format("\n::%s",         HOPPER_COMMAND_TOP_STRING  ))
                        .append(String.format("\n::%s",         HOPPER_COMMAND_NEXT_STRING ))
                        .build();
                chatMessageManager.queue(QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(message)
                        .build());
                return;
            }
            hopToPositionInTimerList(index); // hop to index position

        }

        else if(HOPPER_COMMAND_TOP_STRING.equals(commandExecuted.getCommand())){
            hopTop();
        }

        else if(HOPPER_COMMAND_NEXT_STRING.equals(commandExecuted.getCommand())){
            hopNext();
        }
    }


    //region timersToSkip
    /**
     * The timers visited by quick hopping.
     */
    private final Set<RespawnTimer> timersToSkip = new HashSet<>();


    /**
     * The state of {@link #timersToSkip} when player joined the current world, for reverting to if the hop is canceled.
     */
    private final Set<RespawnTimer> timersToSkipBase = new HashSet<>();


    //region addTimersToSkip
    /**
     * Add all timers that match the given world id to {@link #timersToSkip}.
     * @param worldToSkip the worldId of the world to skip.
     */
    private void addTimersToSkip(int worldToSkip){
        addTimersToSkip(Collections.singletonList(worldToSkip));
    }


    /**
     * Add all timers that match any of the given world ids to {@link #timersToSkip}.
     * @param worldsToSkip The worldId of each of the worlds to skip.
     */
    private void addTimersToSkip(Collection<Integer> worldsToSkip){
        timersToSkip.removeIf(RespawnTimer::isDeleted);
        activeTimers.getActiveTimers().stream()
                .filter(timer -> worldsToSkip.contains(timer.getWorldId()))
                .forEach(timersToSkip::add);

    }
    //endregion

    /**
     * Clean up deleted timers from {@link #timersToSkip}, and save a copy to {@link #timersToSkipBase}.
     */
    public void joinedWorld(){//todo can i make this subscribed?
        timersToSkip.removeIf(RespawnTimer::isDeleted); // clean up deleted timers
        { // save state for reverting to
            timersToSkipBase.clear();
            timersToSkipBase.addAll(timersToSkip);
        }
    }
    //endregion


    /**
     * Hop to the world that is at the top of the timers list (skipping timers for the current world).
     * @see #hop(int)
     */
    private void hopTop(){
        if (activeTimers.getActiveTimers().isEmpty()) { return; } // do nothing if there are no active timers

        { // hop top clears the timersToSkip
            timersToSkip.clear();
        }

        int currentWorldId = client.getWorld();
        int firstTimerWorldId = activeTimers.getActiveTimers().getFirst().getWorldId();
        if (firstTimerWorldId!=currentWorldId){
            hop(firstTimerWorldId);
        } else { // skip the first world if that is the current world
            addTimersToSkip(firstTimerWorldId);
            findNextWorld().ifPresent(this::hop);
        }
    }


    /**
     * Hop to the world that is next in the timers list (skipping visited timers and current world).
     * Or, if all timers have been visited, hop as by {@link #hopTop}.
     * @see #hop(int)
     */
    private void hopNext(){
        { // revert any changes made by canceled hops
            timersToSkip.clear();
            timersToSkip.addAll(timersToSkipBase);
        }

        findNextWorld()
                .ifPresentOrElse(
                        this::hop,
                        this::hopTop
                );
    }


    /**
     * Hop to timer at given index, and {@link #timersToSkip} to be cleared and set with the worlds of the timers above the target timer.
     * @param index The index of the target timer.
     */
    private void hopToPositionInTimerList(int index){
        if (
                index<0
                ||index >= activeTimers.getActiveTimers().size()
        ) { return; } // if index out of bounds do nothing

        { // timersToSkip to be cleared and set with the worlds of the timers above the target index
            timersToSkip.clear();
            /*addTimersToSkip(
                    activeTimers.getActiveTimers()
                            .subList(0, index).stream()
                            .map(RespawnTimer::getWorldId)
                            .collect(Collectors.toCollection(HashSet::new))
            );*/
            for (int i=0; i<index; ++i){
                int worldIdToSkip = activeTimers.getActiveTimers().get(i).getWorldId();
                addTimersToSkip(worldIdToSkip);
            } //todo change to version without random access
        }

        int targetWorldId = activeTimers.getActiveTimers().get(index).getWorldId();
        hop(targetWorldId);
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
        addTimersToSkip(world.getId()); // following hops will skip the target of this hop

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
