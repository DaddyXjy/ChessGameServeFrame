package frame;

import lombok.Setter;

public abstract class Root {
    private @Setter boolean isDestroy;

    public boolean getIsDestroy() {
        return isDestroy;
    }
}