package frame;

import lombok.Getter;
import lombok.Setter;

public abstract class Callback {
    private @Getter @Setter Object data;

    public abstract void func();
}