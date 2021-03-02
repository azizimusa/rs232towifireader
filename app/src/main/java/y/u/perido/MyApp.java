package y.u.perido;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class MyApp extends Application {

    private static MyApp instance;
    public static synchronized MyApp getInstance() {
        return instance;
    }
    private SharedPreferences sharedPreferences = null;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

    }

    public SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("gg", Context.MODE_PRIVATE);
        }

        return sharedPreferences;
    }
}
