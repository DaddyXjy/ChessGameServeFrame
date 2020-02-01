package frame.http;

import lombok.Data;

import java.io.Serializable;

@Data
public class RoomConfigurationVO extends BaseModel implements Serializable {

	private static final long serialVersionUID = -1L;
	private TbGameRoom tbGameRoom;
	private TbGameRoomConfigurationBet tbRoomConfig;

}