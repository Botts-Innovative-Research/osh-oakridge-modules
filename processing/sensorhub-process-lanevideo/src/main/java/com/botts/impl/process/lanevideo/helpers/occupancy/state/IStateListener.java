package com.botts.impl.process.lanevideo.helpers.occupancy.state;

import static com.botts.impl.process.lanevideo.helpers.occupancy.state.StateManager.State;

public interface IStateListener {
    public void onTransition(State from, State to);
}
