package frame.socket;

import java.util.Map;

public class Response extends BaseResponse {

  public Response(int msgType, byte[] protoMsg) {
    super(msgType, protoMsg);
  }

  /*
   * public Response(int msgType, String jsonMsg) { super(msgType, jsonMsg); }
   */

  public Response(int msgType) {
    super(msgType);
  }

  public Response(Request req) {
    super(req);
  }

}
