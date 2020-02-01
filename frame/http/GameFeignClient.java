//package frame.http;
//
//import frame.game;
//import frame.model.UserGame;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@FeignClient(name = "GAME-SERVICE")
//public interface GameFeignClient {
//
//	@PostMapping("/game/getWildGameRoomConfigVo")
//	@Deprecated
//	GlobeResponse<List<RoomConfigurationVO>> getWildGameRoomConfigVo(@RequestParam("siteId") Long siteId,
//                                                                     @RequestParam("gameId") Integer gameId);
//
//	@PostMapping("/game/getWildGameRoomConfigVo2")
//	GlobeResponse<Object> getAllSiteGame(@RequestParam("gameId") Integer gameId);
//
//	@PostMapping("/game/updateMemberGameInfo")
//	GlobeResponse<Object> updateMemberGameInfo(@RequestBody UserGame userGame);
//
//	@PostMapping("/gameAccount/getSiteGameInventoryForGame")
//	GlobeResponse<Object> getSiteGameInventoryForGame(@RequestBody GameFrameInventory gameFrameInventory);
//
//	@PostMapping("/gameAccount/updateIventoryForGame")
//	GlobeResponse<Object> updateIventoryForGame(@RequestParam("siteId")Integer siteId, @RequestParam("money")BigDecimal money);
//}
