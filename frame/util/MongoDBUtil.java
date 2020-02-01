//package frame.util;
//
//import com.mongodb.BasicDBObject;
//import com.mongodb.MongoClient;
//import com.mongodb.MongoClientOptions;
//import com.mongodb.MongoClientOptions.Builder;
//import com.mongodb.WriteConcern;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoCursor;
//import com.mongodb.client.MongoDatabase;
//import com.mongodb.client.MongoIterable;
//import com.mongodb.client.model.Filters;
//import com.mongodb.client.result.DeleteResult;
//import org.bson.Document;
//import org.bson.conversions.Bson;
//import org.bson.types.ObjectId;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class MongoDBUtil {
//
//
//    private MongoCollection<Document> mongoCollection;
//
//    private static MongoClient mongoClient;
//
//
//    static {
//        //logger.info("===============MongoDBUtil初始化========================");
//        //mongoClient = new MongoClient(Config.MONGO_HOST, Config.MONGO_PORT);
//        mongoClient = new MongoClient("172.20.101.68", 27017);
//        // 大部分用户使用mongodb都在安全内网下，但如果将mongodb设为安全验证模式，就需要在客户端提供用户名和密码：
//        // boolean auth = db.authenticate(myUserName, myPassword);
//        Builder options = new MongoClientOptions.Builder();
//        // options.autoConnectRetry(true);// 自动重连true
//        // options.maxAutoConnectRetryTime(10); // the maximum auto connect
//        // retry time
//        options.connectionsPerHost(100);// 连接池设置为300个连接,默认为100
//        options.connectTimeout(15000);// 连接超时，推荐>3000毫秒
//        options.maxWaitTime(5000); //
//        options.socketTimeout(0);// 套接字超时时间，0无限制
//        // 线程队列数，如果连接线程排满了队列就会抛出“Out of semaphores to get db”错误。
//        options.threadsAllowedToBlockForConnectionMultiplier(5000);
//        options.writeConcern(WriteConcern.SAFE);
//        options.build();
//    }
//
//
//    /**
//     * 获取DB实例 - 指定DB
//     *
//     * @param dbName
//     * @return
//     */
//    public static MongoDatabase getDB(String dbName) {
//        if (dbName != null && !"".equals(dbName)) {
//            MongoDatabase database = mongoClient.getDatabase(dbName);
//            return database;
//        }
//        return null;
//    }
//
//
//    /**
//     * 获取collection对象 - 指定Collection
//     *
//     * @param collName
//     * @return
//     */
//    public static MongoCollection<Document> getCollection(String dbName,
//                                                          String collName) {
//        if (null == collName || "".equals(collName)) {
//            return null;
//        }
//        if (null == dbName || "".equals(dbName)) {
//            return null;
//        }
//        MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collName);
//        return collection;
//    }
//
//
//    /**
//     * 查询DB下的所有表名
//     */
//    public static List<String> getAllCollections(String dbName) {
//        MongoIterable<String> colls = getDB(dbName).listCollectionNames();
//        List<String> _list = new ArrayList<String>();
//        for (String s : colls) {
//            _list.add(s);
//        }
//        return _list;
//    }
//
//
//    /**
//     * 获取所有数据库名称列表
//     *
//     * @return
//     */
//    public static MongoIterable<String> getAllDBNames() {
//        MongoIterable<String> s = mongoClient.listDatabaseNames();
//        return s;
//    }
//
//
//    /**
//     * 删除一个数据库
//     */
//    public static void dropDB(String dbName) {
//        getDB(dbName).drop();
//    }
//
//
//    /***
//     * 删除文档
//     *
//     * @param dbName
//     * @param collName
//     */
//    public static void dropCollection(String dbName, String collName) {
//        getDB(dbName).getCollection(collName).drop();
//    }
//
//
//    /**
//     * 查找对象 - 根据主键_id
//     *
//     * @param id
//     * @return
//     */
//    public static Document findById(MongoCollection<Document> coll, String id) {
//        try {
//            ObjectId _id = null;
//            try {
//                _id = new ObjectId(id);
//            } catch (Exception e) {
//                return null;
//            }
//            Document myDoc = coll.find(Filters.eq("_id", _id)).first();
//            return myDoc;
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//
//    }
//
//    /***
//     * 条件查询对象
//     * @param coll
//     * @param filter
//     * @return
//     */
//    public static Document findByNames(MongoCollection<Document> coll, Bson filter) {
//        try {
//            return coll.find(filter).first();
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//
//    }
//
//    /***
//     * 多条件查询对象
//     * @param coll
//     * @return
//     */
//    public static Document findByNames(MongoCollection<Document> coll, Map<String, Object> map) {
//        try {
//            return coll.find(new BasicDBObject(map)).first();
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//
//    }
//
//    /**
//     * 统计数
//     */
//    public static int getCount(MongoCollection<Document> coll) {
//        try {
//            int count = (int) coll.count();
//            return count;
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return 0;
//
//    }
//
//    /**
//     * 查询 多个集合文档
//     */
//    public static MongoCursor<Document> find(MongoCollection<Document> coll, Bson filter) {
//        try {
//            return coll.find(filter).iterator();
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//    }
//
//    /**
//     * map集合 多条件查询
//     *
//     * @param coll
//     * @param map
//     * @return
//     */
//    public static MongoCursor<Document> find(MongoCollection<Document> coll, Map<String, Object> map) {
//        try {
//            return coll.find(new BasicDBObject(map)).iterator();
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//
//    }
//
//
//    /***
//     * 分页查询     默认按_id字段降序
//     * @param coll
//     * @param map
//     * @param pageNo
//     * @param pageSize
//     * @return
//     */
//    public static MongoCursor<Document> findByPage(MongoCollection<Document> coll, Map<String, Object> map, int pageNo, int pageSize) {
//        try {
//            Bson orderBy = new BasicDBObject("_id", -1);
//            return coll.find(new BasicDBObject(map)).sort(orderBy).skip((pageNo - 1) * pageSize).limit(pageSize).iterator();
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//
//    }
//
//    /**
//     * 分页查询 自定义排序
//     *
//     * @param coll
//     * @param sorting
//     * @param name
//     * @param map
//     * @param pageNo
//     * @param pageSize
//     * @return
//     */
//    public static MongoCursor<Document> findByPage(MongoCollection<Document> coll, String sorting, String name,
//                                                   Map<String, Object> map, int pageNo, int pageSize) {
//        try {
//            Bson orderBy = null;
//            //降序
//            if (sorting.equals("desc")) {
//                orderBy = new BasicDBObject(name, -1);
//            } else {
//                orderBy = new BasicDBObject(name, 1);
//            }
//            return coll.find(new BasicDBObject(map)).sort(orderBy).skip((pageNo - 1) * pageSize).limit(pageSize).iterator();
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//
//    }
//
//
//    /**
//     * 通过ID删除
//     *
//     * @param coll
//     * @param id
//     * @return
//     */
//    public static int deleteById(MongoCollection<Document> coll, String id) {
//        try {
//            int count = 0;
//            ObjectId _id = null;
//            _id = new ObjectId(id);
//            Bson filter = Filters.eq("_id", _id);
//            DeleteResult deleteResult = coll.deleteOne(filter);
//            count = (int) deleteResult.getDeletedCount();
//            return count;
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return 0;
//    }
//
//    /**
//     * 修改
//     *
//     * @param coll
//     * @param id
//     * @param newdoc
//     * @return
//     */
//    public static Document updateById(MongoCollection<Document> coll, String id, Document newdoc) {
//        ObjectId _idobj = null;
//        try {
//            _idobj = new ObjectId(id);
//            Bson filter = Filters.eq("_id", _idobj);
//            // coll.replaceOne(filter, newdoc); // 完全替代
//            coll.updateOne(filter, new Document("$set", newdoc));
//            return newdoc;
//        } catch (Exception e) {
//            e.printStackTrace();
//            close();
//        }
//        return null;
//    }
//
//    /**
//     * 添加
//     *
//     * @param coll
//     * @param doc
//     * @return
//     */
//    public static boolean save(MongoCollection<Document> coll, Document doc) {
//        boolean falg = false;
//        try {
//            coll.insertOne(doc);
//            falg = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            //logger.error("添加异常，异常信息：", e);
//        } finally {
//            close();
//        }
//        return falg;
//
//    }
//
//    /**
//     * 关闭Mongodb
//     */
//    public static void close() {
//        if (mongoClient != null) {
//            mongoClient.close();
//            mongoClient = null;
//        }
//    }
//
//
//    public static void main(String[] args) {
////        MongoCollection<Document> list = getCollection("test2", "userEntity");
////        Document d = list.find().first();
////        System.out.println(d.get("userName"));
////        System.out.println(d);
//
////        ;
////        BsonDocument bsonDocument = new BsonDocument();
////        bsonDocument.append("userName", "3");
//
////        Bson eq = Filters.eq("userName", "1");
////        Bson eq2 = Filters.eq("passWord", "11");
////
////        Document first = list.find(eq).filter(eq2).first();
////        System.out.println(first);
//
//
//        //__________________________________________________________________
//
////        Map<String, Object> map = new HashMap<>();
////        map.put("userName", "1");
////        map.put("passWord", "11");
////        list.find(new BasicDBObject(map));
//
//
////        MongoCursor<Document> documentMongoCursor = MongoDBUtil.find(list, map);
////        while (documentMongoCursor.hasNext()){
////            Document next = documentMongoCursor.next();
////            System.out.println(next.toJson());
////            String s = next.toJson();
////            GameClose gameClose1 = JSON.parseObject(s, GameClose.class);
////            System.out.println(gameClose1);
////        }
//
//
//
////        MongoCollection<Document> collection = getCollection("slot_game", "gameClose");
////        Map<String, String> map = new HashMap<>();
////        map.put("gameId", "1");
////        Document first = collection.find(new BasicDBObject(map)).first();
////        Object close = first.get("close");
////        System.out.println(close);
//
//        MongoCollection<Document> list = MongoDBUtil.getCollection("slot_game", "gameClose");
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("close", "1");
//        Document first = list.find(new BasicDBObject(map)).first();
//        System.out.println(first.size());
//        System.out.println(first.get("close"));
//
//    }
//
//
//
//    /**
//     * 测试入口
//     *
//     */
////    public static void main(String[] args) {
////
////        String dbName = "test1";
////        String collName = "userEntity";
////        MongoCollection<Document> coll = MongoDBUtil.getCollection(dbName, collName);
////
////        // 插入多条
////        // for (int i = 1; i <= 4; i++) {
////        // Document doc = new Document();
////        // doc.put("name", "zhoulf");
////        // doc.put("school", "NEFU" + i);
////        // Document interests = new Document();
////        // interests.put("game", "game" + i);
////        // interests.put("ball", "ball" + i);
////        // doc.put("interests", interests);
////        // coll.insertOne(doc);
////        // }
////
////        // // 根据ID查询
////        // String id = "556925f34711371df0ddfd4b";
////        // Document doc = MongoDBUtil2.findById(coll, id);
////        // System.out.println(doc);
////
////        // 查询多个
////        // MongoCursor<Document> cursor1 = coll.find(Filters.eq("name", "zhoulf")).iterator();
////        // while (cursor1.hasNext()) {
////        // org.bson.Document _doc = (Document) cursor1.next();
////        // System.out.println(_doc.toString());
////        // }
////        // cursor1.close();
////
////        // 查询多个
////        // MongoCursor<Person> cursor2 = coll.find(Person.class).iterator();
////
////
////        // 修改数据
////        // String id = "556949504711371c60601b5a";
////        // Document newdoc = new Document();
////        // newdoc.put("name", "时候");
////        // MongoDBUtil.updateById(coll, id, newdoc);
////
////        // 统计表
////        // System.out.println(MongoDBUtil.getCount(coll));
////
////        // 查询所有
////        Bson eq = Filters.eq("userName", "1");
////        MongoCursor<Document> documentMongoCursor = MongoDBUtil.find(coll, eq);
////        while (documentMongoCursor.hasNext()) {
////            Document next = documentMongoCursor.next();
////            System.out.println(next.toJson());
////        }
////
////    }
//
//
//    public MongoCollection<Document> getMongoCollection() {
//        return mongoCollection;
//    }
//}
//
