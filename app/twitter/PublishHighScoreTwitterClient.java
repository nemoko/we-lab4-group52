package twitter;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

//import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import play.Logger;
import java.net.URLEncoder;
import java.util.Date;

public class PublishHighScoreTwitterClient implements ITwitterClient{

    static String consumerKeyStr = "GZ6tiy1XyB9W0P4xEJudQ";
    static String consumerSecretStr = "gaJDlW0vf7en46JwHAOkZsTHvtAiZ3QUd2mD1x26J9w";
    static String accessTokenStr = "1366513208-MutXEbBMAVOwrbFmZtj1r4Ih2vcoHGHE2207002";
    static String accessTokenSecretStr = "RMPWOePlus3xtURWRVnv1TgrjTyK7Zk33evp4KKyA";

    public PublishHighScoreTwitterClient() {
        super();
    }

    /*
    public static void main (String args[]) {
        PublishHighScoreTwitterClient client = new PublishHighScoreTwitterClient();
        TwitterStatusMessage message = new TwitterStatusMessage("me","777",new Date());
        try {
            client.publishUuid(message); // !
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */

    @Override
    public void publishUuid(TwitterStatusMessage message) throws Exception {
        OAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(consumerKeyStr, consumerSecretStr);
        oAuthConsumer.setTokenWithSecret(accessTokenStr, accessTokenSecretStr);

        String encodedMessage = URLEncoder.encode(message.getTwitterPublicationString(),"UTF-8");
        System.out.println(encodedMessage);
        HttpPost httpPost = new HttpPost("https://api.twitter.com/1.1/statuses/update.json?status="+ encodedMessage);

        oAuthConsumer.sign(httpPost);

        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(httpPost);

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        //System.out.println(statusCode + ':' + httpResponse.getStatusLine().getReasonPhrase());
        //System.out.println(IOUtils.toString(httpResponse.getEntity().getContent()));
        //System.out.println(statusCode);
        Logger.info("Status Code of Twitter Response: " + statusCode);
        if(statusCode == 200){
            // TODO notify user of success
        }
    }
}
