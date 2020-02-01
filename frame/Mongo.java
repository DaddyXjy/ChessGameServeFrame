// package frame;
//
// import com.alibaba.fastjson.JSON;
//import frame.util.MongoDBUtil;
// import com.mongodb.BasicDBObject;
// import com.mongodb.client.MongoCollection;
// import com.mongodb.client.MongoCursor;
// import frame.history.PlayerID_gameID;
// import org.bson.Document;
//
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// public class Mongo {
// MultiCallMgr callMgr;
// // private MongoTemplate mongoTemplate;
//
// private static MongoCollection<Document> collection;
//
// static {
// collection = MongoDBUtil.getCollection("", "");
// }
//
// public Mongo() {
// callMgr = new MultiCallMgr();
// callMgr.start();
// }
//
// public void save(Object object) {
// Call call = new Call();
// call.setCall(new Callback() {
//
// @Override
// public void func() {
// Class<?> aClass = object.getClass();
// String jsonString = JSON.toJSONString(object);
// Map map = JSON.parseObject(jsonString, Map.class);
// MongoDBUtil.save(collection, new Document(map));
// }
// });
// callMgr.call(call);
// }
//
// public <T> void find(Map<String, Object> map, Callback cb, Class<T> class1) {
// Call call = new Call();
// call.setCall(new Callback() {
//
// @Override
// public void func() {
//
// List<T> list = new ArrayList<>();
//
// MongoCursor<Document> documentMongoCursor = MongoDBUtil.find(collection,
// map);
// while (documentMongoCursor.hasNext()) {
// Document next = documentMongoCursor.next();
// String s = next.toJson();
// list.add(JSON.parseObject(s, class1));
// }
// cb.setData(list);
// cb.func();
// }
// });
// callMgr.call(call);
// }
//
// public <T> void findOne(Map<String, Object> map, Callback cb, Class<T>
// class1) {
// Call call = new Call();
// call.setCall(new Callback() {
//
// @Override
// public void func() {
// Document first = collection.find(new BasicDBObject(map)).first();
// String s = first.toJson();
// T t = JSON.parseObject(s, class1);
// cb.setData(t);
// cb.func();
// }
// });
// callMgr.call(call);
// }
//
// public <T> void upsert(Map<String, Object> query, Map<String, Object> date,
// Class<T> class1) {
// Call call = new Call();
// call.setCall(new Callback() {
// @Override
// public void func() {
// collection.updateMany(new BasicDBObject(query), new BasicDBObject(date));
// }
// });
// callMgr.call(call);
// }
//
// public <T> void count(Map<String, Object> que, Callback cb, Class<T> class1)
// {
// Call call = new Call();
// call.setCall(new Callback() {
//
// @Override
// public void func() {
// long l = collection.countDocuments(new BasicDBObject(que));
// cb.setData(l);
// cb.func();
// }
// });
// callMgr.call(call);
// }
//
// public enum FindType {
// ONE, FIND, COUNT
// }
//
// public class Option<T> {
// public Option(Map<String, Object> que, FindType tp, Class<T> cl) {
// query = que;
// type = tp;
// cls = cl;
// }
//
// Map<String, Object> query;
// FindType type;
// Class<T> cls;
// }
//
// public <T> void find(ArrayList<Option> options, Callback callback) {
// Call call = new Call();
// call.setCall(new Callback() {
//
// @Override
// public void func() {
// List<Object> data = new ArrayList<>();
// for (Option op : options) {
// if (op.type == FindType.COUNT) {
// long l = collection.countDocuments(new BasicDBObject(op.query));
// data.add(l);
// } else if (op.type == FindType.ONE) {
// data.add(collection.find(new BasicDBObject(op.query), op.cls).first());
// } else if (op.type == FindType.FIND) {
// data.add(collection.find(new BasicDBObject(op.query), op.cls));
// }
// }
// callback.setData(data);
// callback.func();
// }
// });
// callMgr.call(call);
// }
//
// // TODO 没写完整
// public <T> void findRecord(Map<String, Object> queCount, Map<String, Object>
// queGameID, String idName, Class<T> cls,
// Callback cb) {
// Call call = new Call();
// call.setCall(new Callback() {
//
// @Override
// public void func() {
//
// List<PlayerID_gameID> pp = new ArrayList<>();
//
// long count = collection.countDocuments(new BasicDBObject(queCount));
//
// MongoCursor<Document> documentMongoCursor = MongoDBUtil.find(collection,
// queGameID);
// while (documentMongoCursor.hasNext()) {
// Document next = documentMongoCursor.next();
// String s = next.toJson();
// PlayerID_gameID t = JSON.parseObject(s, PlayerID_gameID.class);
// pp.add(t);
// }
//
// Object games = null;
// // if (pp.size() > 0) {
// // Criteria[] criterias = new Criteria[pp.size()];
// // for (int i = 0; i < pp.size(); ++i) {
// // Criteria gameCri = Criteria.where(idName).is(pp.get(i).gameID);
// // criterias[i] = gameCri;
// // }
// // Criteria criteria = new Criteria();
// // criteria.orOperator(criterias);
// // Query gameQuery = new Query(criteria);
// // games = mongoTemplate.find(gameQuery.limit(pp.size()), cls);
// // }
// Map<String, Object> map = new HashMap<>();
// map.put("count", count);
// map.put("games", games);
// cb.setData(map);
// cb.func();
// }
// });
// callMgr.call(call);
// }
// }