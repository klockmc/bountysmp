package io.bountysmp;

import java.util.UUID;

public final class BountyPlayerState {
    private int hearts;
    private UUID currentTarget;
    private UUID lastTarget;
    private boolean guiEnabled;
    private boolean completedWeeklyBounty;

    public BountyPlayerState() {
        this.hearts = 10;
        this.guiEnabled = true;
    }

    public int getHearts() {
        return hearts;
    }

    public void setHearts(int hearts) {
        this.hearts = hearts;
    }

    public UUID getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(UUID currentTarget) {
        this.currentTarget = currentTarget;
    }

    public UUID getLastTarget() {
        return lastTarget;
    }

    public void setLastTarget(UUID lastTarget) {
        this.lastTarget = lastTarget;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public void setGuiEnabled(boolean guiEnabled) {
        this.guiEnabled = guiEnabled;
    }

    public boolean isCompletedWeeklyBounty() {
        return completedWeeklyBounty;
    }

    public void setCompletedWeeklyBounty(boolean completedWeeklyBounty) {
        this.completedWeeklyBounty = completedWeeklyBounty;
    }
}
