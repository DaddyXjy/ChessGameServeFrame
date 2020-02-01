//package frame.http;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.math.BigDecimal;
//
//@FeignClient(name = "ACCOUNT-SERVICE")
//public interface AccountFeignClient {
//
//    @PostMapping("/acc/getPlayer")
//    GlobeResponse<Object> getPlayer(@RequestParam("siteId") Long siteId, @RequestParam("account") String account);
//
//    @PostMapping("/acc/addMoney")
//    GlobeResponse<Object> addMoney(@RequestParam("siteId") Long siteId, @RequestParam("account") String account,
//                                   @RequestParam("money") BigDecimal money);
//}
