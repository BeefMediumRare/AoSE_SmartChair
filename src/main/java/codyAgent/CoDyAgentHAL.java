package codyAgent;

import helper.Direction;
import helper.Point;

import javax.annotation.Nonnull;

public interface CoDyAgentHAL {

    /**
     * @param direction the direction to make one step to
     **/ //@param callback  a callback that will be called once this movement is completed
    void move(@Nonnull Direction direction/*, @Nonnull Runnable callback*/);

    /**
     * @return the current position
     */
    @Nonnull
    Point getCurrentPos();

}