package espertolabs.esperto_ble_watch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.ghedeon.AwsInterceptor;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

public class ApiGatewayHandler {
    //send heart rate request - just for prototyping for now
    //BasicAWSCredentials credentials = new BasicAWSCredentials("AKIAI5TPBKZTQJBU523Q", "TTRcNw6ch3OD0wIwD+rRWQAY0pudufuPIImwqUoA");
    //AWSCredentialsProvider credentialsProvider = new StaticCredentialsProvider(credentials);
    private CognitoCachingCredentialsProvider credentialsProvider;
    private AwsInterceptor awsInterceptor;

    ApiGatewayHandler (Context appContext){
        credentialsProvider = new CognitoCachingCredentialsProvider(appContext, "us-east-1:8393f422-c4d0-4448-a71f-cddadb939273", Regions.fromName("us-east-1"));
        Map<String, String> logins = new HashMap<String, String>();
        //todo: cognitoIdToken shouldn't be in constructor, but rather stored locally when user logs in
        //todo: and retrieved here
        SharedPreferences sharedPref = appContext.getSharedPreferences("userId", Context.MODE_PRIVATE);
        String cognitoIdToken = sharedPref.getString("USER_TOKEN", "");
        logins.put("cognito-idp.us-east-1.amazonaws.com/us-east-1_RL95jbC5g", cognitoIdToken);
        credentialsProvider.setLogins(logins);
        awsInterceptor = new AwsInterceptor(credentialsProvider, "execute-api", "us-east-1");
    }

    public OkHttpClient getHttpClient(){
        final OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(awsInterceptor)
                .build();

        return client;
    }


    public String getHeartRate(String userId){
        final String invokeUrl = "https://75pp5et7e7.execute-api.us-east-1.amazonaws.com/prod/heartRate/" + userId;
        String body = "";
        try {
            okhttp3.Request request2 = new okhttp3.Request.Builder()
                    .url(invokeUrl)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            okhttp3.Response response = null;
            response = getHttpClient().newCall(request2).execute();
            body = response.body().string();
            Log.d("HR_resp_body", "response " + body);
        } catch (Exception e) {
            Log.d("HR_resp_error", "error " + e);
        }
        return body;
    }

    public String getStepCount(String userId){
        final String invokeUrl = "https://75pp5et7e7.execute-api.us-east-1.amazonaws.com/prod/stepCount/" + userId;
        String body = "";
        try {
            okhttp3.Request request2 = new okhttp3.Request.Builder()
                    .url(invokeUrl)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            okhttp3.Response response = null;
            response = getHttpClient().newCall(request2).execute();
            body = response.body().string();
            Log.d("SC_resp_body", "response " + body);
        } catch (Exception e) {
            Log.d("SC_resp_error", "error " + e);
        }
        return body;
    }

    public String getUserInfo(String userId){
        final String invokeUrl = "https://75pp5et7e7.execute-api.us-east-1.amazonaws.com/prod/userInfo/" + userId;
        String body = "";
        try {
            okhttp3.Request request2 = new okhttp3.Request.Builder()
                    .url(invokeUrl)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            okhttp3.Response response = null;
            response = getHttpClient().newCall(request2).execute();
            body = response.body().string();
            Log.d("UI_resp_body", "response " + body);
        } catch (Exception e) {
            Log.d("UI_resp_error", "error " + e);
        }
        return body;
    }
}
