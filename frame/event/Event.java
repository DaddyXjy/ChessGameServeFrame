package frame.event;

import frame.Callback;
import frame.Root;
import lombok.Getter;

public class Event{
    private @Getter Root target;
    private @Getter Callback call;
    private @Getter String name;

    public Event(String name,Callback cb,Root tar){
        target=tar;
        call=cb;
        this.name=name;
    }

    public boolean emit(){
        return emit(null);
    }
    public boolean emit(Object o){
        if(target != null && !target.getIsDestroy()){
            call.setData(o);
            call.func();
            return true;
        }else{
            return false;
        }
    }
}