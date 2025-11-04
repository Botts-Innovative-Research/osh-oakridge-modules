package com.botts.impl.system.lane.helpers.occupancy.state;

import net.opengis.swe.v20.DataComponent;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

public abstract class StateManager {

    public enum State {
        NON_OCCUPANCY,
        OCCUPANCY,
        ALARMING_OCCUPANCY
    }

    protected static class Transition {
        public final BooleanSupplier condition;
        public final State newState;
        Transition(BooleanSupplier condition, State to) {
            this.condition = Objects.requireNonNull(condition);
            this.newState = Objects.requireNonNull(to);
        }
    }

    public static final int TO_STATE = 0;
    public static final int DURING_STATE = 1;
    public static final int FROM_STATE = 2;

    final ReentrantLock lock = new ReentrantLock(true);
    DataComponent dailyFile;
    State currentState;
    List<IStateListener> listeners = new ArrayList<IStateListener>();
    Map<State, Runnable> stateActionMap = new HashMap<>();
    //Map<State, Transition[]> transitionMap = new HashMap<State, Transition[]>();

    public StateManager() {
        currentState = State.NON_OCCUPANCY;
        initStates();
    }

    protected abstract boolean isAlarming();

    protected abstract boolean isOccupied();

    protected abstract boolean isNonOccupied();


    /**
     * Create states and their actions. Each "during" action should contain the state's transitions to other states using
     * {@link #transitionToState(State)} to invoke the transition. In this method, each state must be registered by
     * invoking {@link #registerState(State, Runnable[])}.
     */
    private void initStates() {
        Runnable nonOccupancyTransition;
        Runnable occupancyTransition;
        Runnable alarmTransition;

        // The "to" and "from" transitional runnables may be unneeded. Keeping them only for the time being.
        // --------------------------- Non-occupancy actions ---------------------------
        nonOccupancyTransition = () -> {
            if (isOccupied())
                transitionToState(State.OCCUPANCY);
            else if (isAlarming()) {
                // Might be possible to receive alarm characters when previously not occupied? Including transition in case
                transitionToState(State.ALARMING_OCCUPANCY);
            }
        };

        // --------------------------- Non-alarming occupancy actions ---------------------------
        occupancyTransition = () -> {
            if (isAlarming())
                transitionToState(State.ALARMING_OCCUPANCY);
            else if (isNonOccupied())
                transitionToState(State.NON_OCCUPANCY);
        };

        // --------------------------- Alarming occupancy actions ---------------------------
        alarmTransition = () -> {
            if (isNonOccupied())
                transitionToState(State.NON_OCCUPANCY);
        };

        registerState(State.NON_OCCUPANCY,  nonOccupancyTransition);
        registerState(State.OCCUPANCY,  occupancyTransition);
        registerState(State.ALARMING_OCCUPANCY,  alarmTransition);
    }

    protected void registerState(State state, Runnable transitionFunction) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(transitionFunction);
        /*
        if (transitions == null || transitions.length == 0) {
            throw new IllegalArgumentException("Transition count must be greater than 0.");
        }
         */
        stateActionMap.put(state, transitionFunction);
        //transitionMap.put(state, transitions);
    }

    public void updateDailyFile(DataComponent dailyFile) {
        lock.lock();
        try {
            this.dailyFile = dailyFile;
            parseDailyFile();
            stateActionMap.get(currentState).run();
        } finally {
            lock.unlock();
        }
    }

    protected abstract void parseDailyFile();

    protected void transitionToState(State newState) {
        notifyStateTransition(currentState, newState);
        currentState = newState;
    }

    public void notifyStateTransition(State fromState, State toState) {
        for (IStateListener listener : listeners) {
            listener.onTransition(fromState, toState);
        }
    }

    public State getCurrentState() { return currentState; }
    public void addListener(IStateListener listener) { listeners.add(listener); }
    public void removeListener(IStateListener listener) { listeners.remove(listener); }
    public void clearListeners() { listeners.clear(); }
}