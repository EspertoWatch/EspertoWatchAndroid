package espertolabs.esperto_ble_watch;

import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.ghedeon.AwsInterceptor;

import okhttp3.OkHttpClient;

public class ApiGatewayHandler {
    //send heart rate request - just for prototyping for now
    BasicAWSCredentials credentials = new BasicAWSCredentials("AKIAI5TPBKZTQJBU523Q", "TTRcNw6ch3OD0wIwD+rRWQAY0pudufuPIImwqUoA");
    AWSCredentialsProvider credentialsProvider = new StaticCredentialsProvider(credentials);

    AwsInterceptor awsInterceptor = new AwsInterceptor(credentialsProvider, "execute-api", "us-east-1");
    final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(awsInterceptor)
            .build();

    public void getHeartRate(String userId){
        final String invokeUrl = "https://75pp5et7e7.execute-api.us-east-1.amazonaws.com/prod/heartRate/" + userId;
        try {
            okhttp3.Request request2 = new okhttp3.Request.Builder()
                    .url(invokeUrl)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            okhttp3.Response response = null;
            response = client.newCall(request2).execute();
            String body = response.body().string();
            Log.d("HR_resp_body", "response " + body);
        } catch (Exception e) {
            Log.d("HR_resp_error", "error " + e);
        }
    }

    public void getStepCount(String userId){
        final String invokeUrl = "https://75pp5et7e7.execute-api.us-east-1.amazonaws.com/prod/stepCount/" + userId;
        try {
            okhttp3.Request request2 = new okhttp3.Request.Builder()
                    .url(invokeUrl)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            okhttp3.Response response = null;
            response = client.newCall(request2).execute();
            String body = response.body().string();
            Log.d("SC_resp_body", "response " + body);
        } catch (Exception e) {
            Log.d("SC_resp_error", "error " + e);
        }
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
            response = client.newCall(request2).execute();
            body = response.body().string();
            Log.d("UI_resp_body", "response " + body);
        } catch (Exception e) {
            Log.d("UI_resp_error", "error " + e);
        }
        return body;
    }
}
