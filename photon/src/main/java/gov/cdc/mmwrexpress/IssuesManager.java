package gov.cdc.mmwrexpress;

import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**IssuesManager.java
 * photon-droid
 *
 * Created by jtq6 on 10/29/14.
 * Copyright (c) 2015 Informatics Research and Development Lab. All rights reserved.
 */

public class IssuesManager {

    RealmResults<Issue> issues;
    RealmResults<Keyword> keywords;
    public static UpdateFromFeedTask updateFromFeedTask;
    boolean hasIssues;
    private WeakReference<SwipeRefreshLayout> progressIndicatorReference;

    public IssuesManager() {

        this.hasIssues = false;
    }

    public Issue processRssIssue(String dateAsString, Integer volume, Integer number) {

        Realm realm = Realm.getDefaultInstance();
        Date date = getIssueDateFromString(dateAsString);
        RealmQuery<Issue> query = realm.where(Issue.class).equalTo("volume",volume).equalTo("number",number).equalTo("date",date);
        Issue foundIssue = query.findFirst();
        Issue results;
        if (foundIssue != null) {
            results = foundIssue;
        } else {
            Issue newIssue = realm.createObject(Issue.class);
            newIssue.setDate(date);
            newIssue.setVolume(volume);
            newIssue.setNumber(number);

            results = newIssue;
        }
        realm.close();
        return results;
    }

    public void registerProgressIndicator(SwipeRefreshLayout refreshLayout){
        progressIndicatorReference = new WeakReference<>(refreshLayout);
    }

    public void unregisterProgressIndicator(SwipeRefreshLayout refreshLayout){
        progressIndicatorReference = null;
    }

    public Article createArticleInIssue(Issue issue, String title, int version) {
        Realm realm = Realm.getDefaultInstance();
        Article newArticle = realm.createObject(Article.class);
        newArticle.setTitle(title);
        newArticle.setVersion(version);
        newArticle.setUnread(true);
        issue.getArticles().add(newArticle);
        realm.close();
        return newArticle;

    }


    // return a new Article reference only if needed
    // otherwise return null
    public Article processRssArticle(Issue issue, String title, Integer version) {

         // quick check for any articles at all
        if (issue.getArticles().size() == 0)
            return createArticleInIssue(issue, title, version);

        for (Article article: issue.getArticles()) {

            // check if article with this title already exists in current issue
            if (article.getTitle().equals(title)) {
                //Log.d("IssuesManager ", "processRssArticle: existing article found: " +title);
                // check if version number of stored article is less than
                // version number of article from RSS feed
                if (article.getVersion() < version) {
                    //Log.d("IssuesManager ", "processRssArticle: feed version is newer, replace existing article: " +title);
                    // delete stored article and create new one
                    article.deleteFromRealm();
                    removeUnusedKeywords();
                    return createArticleInIssue(issue, title, version);

                }

                 // if already have this article, return null
                if (article.getVersion() == version)
                    //Log.d("IssuesManager ", "processRssArticle: feed version is same or older, do nothing: " +title);
                    return null;

            }

        }

        // no article matches so create one
        return createArticleInIssue(issue, title, version);

    }

    private Keyword getKeywordWithText(String text) {
        Realm realm = Realm.getDefaultInstance();
        this.keywords = realm.where(Keyword.class).findAll();

        for (Keyword currKeyword : this.keywords) {

            if (currKeyword.getText().equalsIgnoreCase(text)) {
                realm.close();
                return currKeyword;
            }
        }
        realm.close();

        return null;

    }


    private Keyword createNewKeywordInArticle(String text, Article article) {
        Realm realm = Realm.getDefaultInstance();
        Keyword newKeyword = realm.createObject(Keyword.class);
        newKeyword.setText(text);
        newKeyword.getArticles().add(article);
        realm.close();
        return newKeyword;
    }


    public void addArticleKeywords(String[] foundKeywords, Article article) {
        Realm realm = Realm.getDefaultInstance();
        Keyword keyword;

        for (String currKeyword : foundKeywords) {

            if ((keyword = getKeywordWithText(currKeyword)) == null) {
                createNewKeywordInArticle(currKeyword, article);

            } else {
                if (article == null)
//                    Log.d("IssueManager", "Attempt to add null article to keyword " + keyword.getText());
                keyword.setText(currKeyword);
                keyword.getArticles().add(article);

            }
        }
        realm.close();
    }

    public void removeUnusedKeywords(){
        Realm realm = Realm.getDefaultInstance();
        this.keywords = realm.where(Keyword.class).findAll();
        ArrayList<Keyword> k = new ArrayList<Keyword>();
        for(Keyword keyword : keywords)
        {
            k.add(keyword);
        }
        for(Keyword keyword : k){
            if(keyword.getArticles().size()==0){
//                Log.d("Remove keyword: ", " " + keyword.getText());
                keyword.deleteFromRealm();
            }
        }
        realm.close();
    }

    // This method removes unused Issue objects in case all articles
    // of an existing issue are deleted.
    public void removeUnusedIssue(Issue issue){
        if(issue.getArticles().size() == 0){
//            Log.d("Remove issue: ", "VOL " + issue.getVolume() + " NO " +issue.getNumber());
            issue.deleteFromRealm();
        }
    }
    public static Date getIssueDateFromString(String dateAsString)
    {
        Date date = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = format.parse(dateAsString);
            //System.out.println(date);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return date;

    }

    public Article deleteArticle(Article article){
//        Log.d("Remove article: ", "Title: " + article.getTitle());
        Issue issue = article.getIssue();
        article.deleteFromRealm();
        removeUnusedKeywords();
        return null;
    }

    public void update() {
        if(progressIndicatorReference != null) {
            SwipeRefreshLayout progressIndicator = progressIndicatorReference.get();
            progressIndicator.setRefreshing(true);
        }
        updateFromFeedTask = new UpdateFromFeedTask();
        updateFromFeedTask.execute();
    }

    public void onRefreshComplete(Integer resultCode){
        if(progressIndicatorReference != null)
        {
            SwipeRefreshLayout progressIndicator = progressIndicatorReference.get();
            progressIndicator.setRefreshing(false);
            if (resultCode == 1) {
                Snackbar.make(progressIndicator, "Article list updated.", Snackbar.LENGTH_LONG)
                        .show();
            } else if (resultCode == 0) {
                Snackbar.make(progressIndicator, "Error accessing MMWR Feed.",
                        Snackbar.LENGTH_INDEFINITE)
                        .setActionTextColor(progressIndicator.getResources().getColor(R.color.light_yellow))
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                update();
                            }
                        })
                        .show();
            } else if (resultCode == 2) {
                Snackbar.make(progressIndicator, "Cancelled.", Snackbar.LENGTH_LONG).show();
            } else if (resultCode == 3) {
                Snackbar.make(progressIndicator, "An error has occurred. Check internet connection.",
                        Snackbar.LENGTH_INDEFINITE)
                        .setActionTextColor(progressIndicator.getResources().getColor(R.color.light_yellow))
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                update();
                            }
                        })
                        .show();
            }
        }

    }

}