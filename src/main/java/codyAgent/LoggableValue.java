package codyAgent;

public enum LoggableValue {
    EPOCHS, MESSAGES_SEND,MOVED, BLOCK, FULL_BLOCK, FULL_BLOCK_EMERGENCY, PRIO_OVERFLOW, DIST_START_TARGET;

    @Override
    public String toString() {
        return super.toString().toLowerCase().replaceAll("_", " ");
    }
}
