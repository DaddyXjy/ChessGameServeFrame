package frame;

import java.util.HashMap;
import java.util.Map;

public class CallRegisterMgr {
    private HashMap<String, CallbackFactory> urls = new HashMap<>();

    public void register(String url, CallbackFactory callbackClass) {
        urls.put(url, callbackClass);
    }

    public Call create(String url, Map<String, Object> params) {
        CallbackFactory callCls = urls.get(url);
        if (callCls == null) {
            return null;
        }

        Callback urlCall = callCls.create();
        if (urlCall == null) {
            return null;
        }
        urlCall.setData(params);
        Call req = new Call();
        req.setCall(urlCall);
        return req;
    }
}