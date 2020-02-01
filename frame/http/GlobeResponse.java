package frame.http;

import java.io.Serializable;

public class GlobeResponse<T> implements Serializable {

    /**
     * code 是请求返回状态，默认200是成功，其他任何状态需要自己去设置代表其他情况！
     */
    private String code = "200";
    /**
     * msg 错误等描述，自由发挥，统一定义！
     */
    private String msg = "请求成功！";
    private T data;

    public GlobeResponse(){
    }

    public GlobeResponse(T data){
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setGlobeResponse(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
