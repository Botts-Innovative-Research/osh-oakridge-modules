package com.botts.impl.system.lane.helpers.occupancy.state;

import static com.botts.impl.system.lane.helpers.occupancy.state.StateManager.State;

public interface IStateListener {
    public void onTransition(State from, State to);
}
