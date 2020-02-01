package frame.config;

import lombok.Data;

@Data
public class Config {

    public static String MONGO_HOST;
    public static Integer MONGO_PORT;


    public static String ACCOUNT_HOST;
    public static String GAME_HOST;
    public static String ACCOUNT_PORT;
    public static String GAME_PORT;

    private static String DBNAME;
//    private static String collName;

}
