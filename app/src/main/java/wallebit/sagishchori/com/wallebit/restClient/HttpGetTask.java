package wallebit.sagishchori.com.wallebit.restClient;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import wallebit.sagishchori.com.wallebit.utils.ConnectionUtils;

public class HttpGetTask extends AsyncTask<RestAPIClient, Void, String>
{
    private final Context context;
    private String stringUrl;
    private HttpTaskResponse httpTaskResponse;

    public HttpGetTask(String stringUrl, HttpTaskResponse httpTaskResponse, Context context)
    {
        this.stringUrl = stringUrl;
        this.httpTaskResponse = httpTaskResponse;
        this.context = context;
    }

    @Override
    protected void onPreExecute()
    {
        if (!ConnectionUtils.isOnLine(context))
        {
            if (httpTaskResponse != null)
                httpTaskResponse.onTaskFailed();

            cancel(true);
        }
    }

    @Override
    protected String doInBackground(RestAPIClient... restAPIClients)
    {
        HttpsURLConnection connection = null;
        InputStream stream = null;
        String result = null;

        try {
            URL url = new URL(stringUrl);
            connection = (HttpsURLConnection) url.openConnection();

            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);

            connection.connect();

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                stream = connection.getInputStream();

                result = readStream(stream, 500);
            }
            else
                throw new IOException("HTTP error code: " + responseCode);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            httpTaskResponse.onTaskFailed();
        } catch (IOException e) {
            e.printStackTrace();
            httpTaskResponse.onTaskFailed();
        }
        finally {
            connection.disconnect();
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(String s)
    {
        super.onPostExecute(s);

        if (httpTaskResponse != null && s != null && !s.isEmpty())
            httpTaskResponse.onTaskCompleted();
        else
            httpTaskResponse.onTaskFailed();
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    public String readStream(InputStream stream, int maxReadSize) throws IOException, UnsupportedEncodingException
    {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] rawBuffer = new char[maxReadSize];
        int readSize;
        StringBuffer buffer = new StringBuffer();
        while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
            if (readSize > maxReadSize) {
                readSize = maxReadSize;
            }
            buffer.append(rawBuffer, 0, readSize);
            maxReadSize -= readSize;
        }
        return buffer.toString();
    }
}