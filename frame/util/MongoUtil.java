//package frame.util;
//
//import frame.game;
//import frame.model.GameClose;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//
//
//public class MongoUtil {
//
//    public static void update( GameMain.Status status, int gameId) {
//        Query query = new Query();
//        query.addCriteria(Criteria.where("gameId").is(gameId));
//        Update update = new Update();
//        if (status == GameMain.Status.RUN) {
//            update.set("close", 1);
//        }
//        update.set("status", status);
//
//        GameMain.getInstance().getMongo().upsert(query, update, GameClose.class);
//        // mongoTemplate.upsert(query, update, GameClose.class);
//        log.info("更新服务器运行状态GameMain.Status={}", status);
//    }
//}
