package gov.cdc.mmwrexpress;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import io.realm.OrderedRealmCollectionSnapshot;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;

/**JsonArticleParser
 * photon-droid
 *
 * Created by jtq6 on 10/28/14.
 * Copyright (c) 2015 Informatics Research and Development Lab. All rights reserved.
 */

public class JsonArticleParser {

    String json;
    Article article;

    // JSON Node names
    private static final String TAG_ISSUE_DATE = "issue-date";
    private static final String TAG_ISSUE_VOL = "issue-vol";
    private static final String TAG_ISSUE_NUM = "issue-no";
    private static final String TAG_TITLE = "title";
    private static final String TAG_ALREADY_KNOWN = "already_known";
    private static final String TAG_ADDED_BY_REPORT = "added_by_report";
    private static final String TAG_IMPLICATIONS = "implications";
    private static final String TAG_TAGS = "tags";
    private static final String TAG_TAG = "tag";
    private static final String TAG_URL = "url";
    private static final String TAG_CONTENT_VER = "content-ver";
    private static final String TAG_SCHEMA_VER = "schema-ver";
    private static final String TAG_COMMAND = "command";
    private static final String DELETE_COMMAND = "delete";

    public JsonArticleParser() {

    }

    private boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            Log.d("JSONArticleParser", "JSON Error with JSON String: " +test);
            ex.printStackTrace();
        }
        return true;
    }

    public Article parseJsonArticle(String jsonArticle) {
        if (jsonArticle == null)
            return null;

        if (!isJSONValid(jsonArticle)) {
            return null;
        }

        Realm realm = Realm.getDefaultInstance();
        // create new JSON object from string
        try {

            JSONObject jsonObject = new JSONObject(jsonArticle);
            //Log.d("JsonArticleParser", "Feed Article JSON object " +jsonObject.toString());
            int size = jsonObject.length();

            realm.beginTransaction();

            if(jsonObject.has(TAG_COMMAND)){
                if(jsonObject.getString(TAG_COMMAND).equals(DELETE_COMMAND)){
                    RealmQuery<Issue> issueQuery = realm.where(Issue.class)
                            .equalTo("volume", jsonObject.getInt(TAG_ISSUE_VOL))
                            .equalTo("number", jsonObject.getInt(TAG_ISSUE_NUM))
                            .equalTo("date", IssuesManager.getIssueDateFromString(jsonObject.getString(TAG_ISSUE_DATE)));

                    if(issueQuery.findFirst() != null) {
                        Issue issue = issueQuery.findFirst();
                        RealmList<Article> articles = issue.getArticles();
                        OrderedRealmCollectionSnapshot<Article> articlesSnapshot = articles.createSnapshot();

                        for (Article article : articlesSnapshot) {
                            if(article.getTitle().equals(jsonObject.getString(TAG_TITLE))){
                                AppManager.issuesManager.deleteArticle(article);
                            }
                        }
                        AppManager.issuesManager.removeUnusedIssue(issue);
                    }

                    realm.commitTransaction();
                    return null;
                }
            }
            // process issue found in RSS feed and find stored issue
            // if no stored issue create a new one
            Issue issue = AppManager.issuesManager.processRssIssue(jsonObject.getString(TAG_ISSUE_DATE),
                    jsonObject.getInt(TAG_ISSUE_VOL), jsonObject.getInt(TAG_ISSUE_NUM));


            // process article found in RSS feed and find stored issue
            // or in not there create a new one
            Article article = AppManager.issuesManager.processRssArticle(issue, jsonObject.getString(TAG_TITLE), jsonObject.getInt(TAG_CONTENT_VER));

            if (article != null) {

                article.setAlready_known(jsonObject.getString(TAG_ALREADY_KNOWN));
                article.setAdded_by_report(jsonObject.getString(TAG_ADDED_BY_REPORT));
                article.setImplications(jsonObject.getString(TAG_IMPLICATIONS));
                article.setUrl(jsonObject.getString(TAG_URL));
                article.setIssue(issue);
                //Log.d("Parser:", jsonObject.toString());

                try {
                    JSONArray keywordJsonArray = jsonObject.getJSONArray(TAG_TAGS);
                    JSONObject keywordJson;
                    String[] keywords = new String[keywordJsonArray.length()];
                    for (int i = 0; i < keywordJsonArray.length(); i++) {
                        keywordJson = (JSONObject) keywordJsonArray.get(i);
                        keywords[i] = keywordJson.getString(TAG_TAG);
                    }
                    AppManager.issuesManager.addArticleKeywords(keywords, article);

                } catch (JSONException ex) {
                    ex.printStackTrace();
                    Log.d("JsonArticleParser", "JSON Article title = " + jsonObject.getString(TAG_TITLE));
                }
            }

            realm.commitTransaction();

            //Log.d("JsonArticleParser", "JSON Article title = " + jsonObject.getString(TAG_TITLE));

            //Log.d("JsonArticleParser", "JSON Article size = " + String.valueOf(size));
            //Log.d("JsonArticleParser", "JSON issue date  = " + jsonObject.getString(TAG_ISSUE_DATE));
            realm.close();
            return article;

        } catch (JSONException ex) {
            ex.printStackTrace();
            realm.close();
            return null;
        }

    }

    public void parseEmbeddedArticles(String articles) {
        try {
            JSONArray articleArray = new JSONArray(articles);

            for(int i = 0; i < articleArray.length(); i++) {
                parseJsonArticle(articleArray.get(i).toString());
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }
}
