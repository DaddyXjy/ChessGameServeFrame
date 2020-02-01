package frame.socket;

import lombok.Data;

@Data
public class BaseResponse {
  // 消息类型
  public int msgType;
  // proto数据
  public byte[] protoMsg;
  // json数据
  public String jsonMsg;
  // 数据类型 0 proto 1 json
  public int dataType = 0;

  public BaseResponse() {
  }

  public BaseResponse(int msgType, byte[] protoMsg) {
    this.msgType = msgType;
    this.protoMsg = protoMsg;
  }

  /*
   * public BaseResponse(int msgType, String jsonMsg) { this.msgType = msgType;
   * this.jsonMsg = jsonMsg; dataType = 1; }
   */

  public BaseResponse(int msgType) {
    this.msgType = msgType;
    this.protoMsg = new byte[0];
  }

  public BaseResponse(Request req) {
    this.msgType = req.msgType;
    this.dataType = req.dataType;
    this.protoMsg = req.protoMsg;
    this.jsonMsg = req.jsonMsg;
  }

}