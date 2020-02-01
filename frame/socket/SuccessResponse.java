package frame.socket;

public class SuccessResponse extends BaseResponse {
    public String msg;

    public SuccessResponse(int msgType, byte[] protoMsg) {
        super(msgType, protoMsg);
    }

    public SuccessResponse(int msgType) {
        super(msgType);
    }
}