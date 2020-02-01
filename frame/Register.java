package frame;

import com.alibaba.fastjson.JSON;
import frame.game.*;
import frame.http.OkHttpDemo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.RequestBody;


import java.util.Map;

@Slf4j
class Register {

    static void calls() {
        CallRegisterMgr callMgr = UtilsMgr.getCallRegisterMgr();

        callMgr.register("/game/updateIventoryForGame", new CallbackFactory() {

            @Override
            public Callback create() {
                return new Callback() {
                    @Override
                    public void func() {
                        String url = OkHttpDemo.GAME_URL + "/game/updateIventoryForGame";
                        Map map = (Map) this.getData();
                        OkHttpDemo.postJson(url, JSON.toJSONString(map),this);

//                        Map map = (Map) this.getData();
//                        Integer siteId = (Integer) map.get("siteId");
//                        String money = (String) map.get("money");
//                        this.setData(gameFeignClient.updateIventoryForGame(siteId,new BigDecimal(money)));
                    }
                };
            }
        });

        callMgr.register("/game/updateMemberGameInfo", new CallbackFactory() {

            @Override
            public Callback create() {
                return new Callback() {
                    @Override
                    public void func() {
                        String url = OkHttpDemo.GAME_URL + "/game/updateMemberGameInfo";
                        Map map = (Map) this.getData();
                        OkHttpDemo.postJson(url, JSON.toJSONString(map),this);

//                        String account = (String) map.get("account");
//                        Long siteId = (Long) map.get("siteId");
//                        Integer gameStatus = (Integer) map.get("gameStatus");
//                        Integer onlineStatus = (Integer) map.get("onlineStatus");
//                        this.setData(gameFeignClient.updateMemberGameInfo(new UserGame(account, siteId, gameStatus, onlineStatus)));
                    }
                };
            }
        });

        callMgr.register("/game/getSiteGameInventoryForGame", new CallbackFactory() {

            @Override
            public Callback create() {
                return new Callback() {
                    @Override
                    public void func() {
                        String url = OkHttpDemo.GAME_URL + "/game/getSiteGameInventoryForGame";
                        Map map = (Map) this.getData();
                        OkHttpDemo.postJson(url, JSON.toJSONString(map),this);
//                        Map map = (Map) this.getData();
//                        Integer gameId = (Integer) map.get("gameId");
//                        this.setData(gameFeignClient.getSiteGameInventoryForGame(new GameFrameInventory(gameId)));
                    }
                };
            }
        });

        callMgr.register("/game/getWildGameRoomConfigVo", new CallbackFactory() {

            @Override
            public Callback create() {
                return new Callback() {
                    @Override
                    public void func() {
                        String url = OkHttpDemo.GAME_URL + "/game/getWildGameRoomConfigVo";
                        Map map = (Map) this.getData();

                        RequestBody body = new FormBody.Builder()
                                .add("siteId", String.valueOf(map.get("siteId")))
                                .add("gameId", String.valueOf(map.get("gameId")))
                                .build();

                        OkHttpDemo.postForm(body,url,this);
                    }
                };
            }
        });

        callMgr.register("/game/getWildGameRoomConfigVo2", new CallbackFactory() {

            @Override
            public Callback create() {
                return new Callback() {
                    @Override
                    public void func() {
                        String url = OkHttpDemo.GAME_URL + "/game/getWildGameRoomConfigVo2";
                        Map map = (Map) this.getData();

                        RequestBody body = new FormBody.Builder()
                                .add("gameId", String.valueOf(map.get("gameId")))
                                .build();

                        OkHttpDemo.postForm(body,url,this);
                    }
                };
            }
        });

        callMgr.register("/acc/getPlayer", new CallbackFactory() {

            @Override
            public Callback create() {
                return new Callback() {
                    @Override
                    public void func() {
                        String url = OkHttpDemo.ACCOUNT_URL + "/acc/getPlayer";
                        Map map = (Map) this.getData();

                        RequestBody body = new FormBody.Builder()
                                .add("siteId", String.valueOf(map.get("siteId")))
                                .add("account", String.valueOf(map.get("account")))
                                .build();

                        OkHttpDemo.postForm(body,url,this);
                    }
                };
            }
        });

        callMgr.register("/acc/addMoney", new CallbackFactory() {

            @Override
            public Callback create() {
                return new Callback() {
                    @Override
                    public void func() {
                        String url = OkHttpDemo.ACCOUNT_URL + "/acc/addMoney";
                        Map map = (Map) this.getData();

                        RequestBody body = new FormBody.Builder()
                                .add("siteId", String.valueOf(map.get("siteId")))
                                .add("account", String.valueOf(map.get("account")))
                                .add("money", String.valueOf(map.get("money")))
                                .build();

                        OkHttpDemo.postForm(body,url,this);
                    }
                };
            }
        });
    }
}