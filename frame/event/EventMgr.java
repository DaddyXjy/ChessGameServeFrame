package frame.event;

import frame.Callback;
import frame.Root;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class EventMgr{
    private Map<String,List<Event>> eventMap=new HashMap<>();
    private boolean del=false;
    List<Event> addList=new ArrayList<>();
    List<Event> removeList=new ArrayList<>();

    public Event regist(String eventName,Callback cb,Root tar){
        List<Event> list=eventMap.get(eventName);
        Event event=new Event(eventName,cb,tar);
        if(list==null){
            list=new ArrayList<>();
            eventMap.put(eventName, list);
        }else{
            if(del){
                addList.add(event);
            }else{
                list.add(event);
            }
        }
        return event;
    }
    public void emit(String eventName){
        List<Event> list=eventMap.get(eventName);
        if(list!=null){
            del=true;
            for(Event event : list){
                if(!event.emit()){
                    removeEvent(event);
                    removeList.add(event);
                }
            }
            del=false;
        }
        delOtherEvent();
    }

    private void delOtherEvent() {
        for(Event e : removeList){
            List<Event> list=eventMap.get(e.getName());
            list.remove(e);
        }
        for(Event e : addList){
            List<Event> list=eventMap.get(e.getName());
            list.add(e);
        }
        removeList.clear();
        addList.clear();
    }

    public void emit(String eventName,Object obj){
        List<Event> list=eventMap.get(eventName);
        if(list!=null){
            del=true;
            for(Event event : list){
                event.getCall().setData(obj);
                if(!event.emit(obj)){
                    removeList.add(event);
                }
            }
            del=false;
        }
    }

    public boolean removeEvent(Event event){
        if(del){
            return false;
        }else{
            List<Event> list=eventMap.get(event.getName());
            return list.remove(event);
        }
    }
    public boolean clearEvent(String eventName){
        if(del){
            return false;
        }else{
            List<Event> list=eventMap.get(eventName);
            list.clear();
            return true;
        }
     
    }
}