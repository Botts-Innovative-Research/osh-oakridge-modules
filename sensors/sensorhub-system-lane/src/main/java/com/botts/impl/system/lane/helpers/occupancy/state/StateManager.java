package com.botts.impl.system.lane.helpers.occupancy.state;

import net.opengis.swe.v20.DataComponent;

import java.util.*;
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

    DataComponent dailyFile;
    State currentState;
    List<IStateListener> listeners = new ArrayList<IStateListener>();
    Map<State, Runnable[]> stateActionMap = new HashMap<>();
    //Map<State, Transition[]> transitionMap = new HashMap<State, Transition[]>();

    public StateManager() {
        currentState = State.NON_OCCUPANCY;
        initStates();
    }


    /**
     * Create states and their actions. Each "during" action should contain the state's transitions to other states using
     * {@link #transitionToState(State)} to invoke the transition. In this method, each state must be registered by
     * invoking {@link #registerState(State, Runnable[])}.
     */
    protected abstract void initStates();

    protected void registerState(State state, Runnable[] stateActions) {
        if (stateActions == null || stateActions.length != 3) {
            throw new IllegalArgumentException("State actions must have length 3, including to, during, and after actions.");
        }
        /*
        if (transitions == null || transitions.length == 0) {
            throw new IllegalArgumentException("Transition count must be greater than 0.");
        }
         */
        for (int i = TO_STATE; i <= FROM_STATE; i++) {
            if (stateActions[i] == null) {
                stateActions[i] = () -> {};
            }
        }
        stateActionMap.put(state, stateActions);
        //transitionMap.put(state, transitions);
    }

    public void updateDailyFile(DataComponent dailyFile) {
        this.dailyFile = dailyFile;
        parseDailyFile();
        stateActionMap.get(currentState)[DURING_STATE].run();

        /*
        // Check transition conditions. If transition occurs, run transition actions and notify listeners.
        for (Transition transition : transitionMap.get(currentState)) {
            if (transition.condition.getAsBoolean()) {
                stateActionMap.get(currentState)[FROM_STATE].run();
                stateActionMap.get(transition.newState)[TO_STATE].run();
                notifyStateTransition(currentState, transition.newState);
                currentState = transition.newState;
                break;
            }
        }
         */
    }

    protected abstract void parseDailyFile();

    protected void transitionToState(State newState) {
        stateActionMap.get(currentState)[FROM_STATE].run();
        stateActionMap.get(newState)[TO_STATE].run();
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