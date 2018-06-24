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

import android.util.Base64;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.OkHttpClient;

public class ApiGatewayHandler {
    String encoded_access_key = "QUtJQUpSR08yUVlHRkNJUERSQlENCg==";
    String encoded_secret_key = "Q2tUNG5VcXZWc2ZGOXBLVzBZVDdqeGYxcE5ETXJlSmxaaU9FcnA1bw0K";

    byte[] decoded_access_key = Base64.decode(encoded_access_key, Base64.DEFAULT);
    String access_key = new String(decoded_access_key);

    byte[] decoded_secret_key = Base64.decode(encoded_secret_key, Base64.DEFAULT);
    String secret_key = new String(decoded_secret_key);

    BasicAWSCredentials credentials = new BasicAWSCredentials(access_key, secret_key);
    AWSCredentialsProvider credentialsProvider = new StaticCredentialsProvider(credentials);

    AwsInterceptor awsInterceptor = new AwsInterceptor(credentialsProvider, "execute-api", "us-east-1");
    final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(awsInterceptor)
            .build();

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
