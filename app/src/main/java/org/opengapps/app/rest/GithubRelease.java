package org.opengapps.app.rest;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by codekid on 28/07/17.
 */

public class GithubRelease {

    private final String TAG = getClass().getName();
    // REST API v3 opengapps release url
    private String URL_OPENGAPPS_RELEASE = "https://api.github.com/repos/opengapps/%arch/releases";

    //okhttp client
    private OkHttpClient client;

    //rest rx components
    private Observer mRestObserver;
    private io.reactivex.Observable<String> mRestObservable;
    private String jsonString;

    //parser
    GithubReleaseParser githubReleaseParser;

    public GithubReleaseParser getGithubReleaseParser() {
        return githubReleaseParser;
    }

    public void setGithubReleaseParser(GithubReleaseParser githubReleaseParser) {
        this.githubReleaseParser = githubReleaseParser;
    }

    public GithubRelease init() {
        client = new OkHttpClient();

        return this;
    }

    public GithubRelease constructFromArch(String architecture) {
        URL_OPENGAPPS_RELEASE = URL_OPENGAPPS_RELEASE.replace("%arch", architecture);
        return this;
    }

    public GithubRelease request() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request = buildRequest();
                Response response = null;
                try {
                     response = client.newCall(request).execute();
                    jsonString = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    githubReleaseParser = new GithubReleaseParser(jsonString);
                }
            }
        }).start();

        return this;
    }

    private Request buildRequest() {
        return new Request.Builder()
                .url(URL_OPENGAPPS_RELEASE)
                .build();
    }
}
