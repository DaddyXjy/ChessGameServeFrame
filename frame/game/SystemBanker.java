package frame.game;

import frame.Callback;
import frame.socket.BaseResponse;
import frame.socket.Request;
import frame.socket.common.proto.Type.Server_Type;
import lombok.Getter;

/**
* @author KAZER
* @version 创建时间：2019年7月13日 下午6:06:10
* 类说明
*/
@Getter
public class SystemBanker extends Role{
	/**
	 * 单例
	 */
    private static SystemBanker instance = null;
	
	
	private SystemBanker() {
		nickName = "系统当庄";
		money = 999999999999l;
		userId = -999999;  //
		uniqueId = -999999; // 发给客户的是 uniqueId
		portrait = "";
	}
    public static SystemBanker instance(){
        if(null == instance){
            instance = new SystemBanker();
        }
        return instance;
    }
	
	@Override
	public void send2Route(BaseResponse response, Server_Type targetServerType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send2Route(BaseResponse response, Server_Type targetServerType, Callback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reply2Route(Request receivedRequest, BaseResponse response) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(BaseResponse response) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMsg(Request request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isPairOnGaming() {
		// TODO Auto-generated method stub
		return false;
	}

}
