package gov.cdc.mmwrexpress;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

/**ArticleDetailActivity.java
 * photon-droid
 *
 * Copyright (c) 2015 Informatics Research and Development Lab. All rights reserved.
 */

public class ArticleDetailActivity extends AppCompatActivity{
    private String title;
    private String pubDate;
    private int volume;
    private int number;
    private String link;

    public static Intent newIntent(Context packageContext, String title, String date, int volume, int number, String link){
        Intent intent = new Intent(packageContext, ArticleDetailActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("pubDate", date);
        intent.putExtra("volume", volume);
        intent.putExtra("number", number);
        intent.putExtra("link", link);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        pubDate = intent.getStringExtra("pubDate");
        volume = intent.getIntExtra("volume", -1);
        number = intent.getIntExtra("number", -1);
        link = intent.getStringExtra("link");

        addArticleDetailFragment();


    }

    private void share(){
        AppManager.sc.trackEvent(Constants.SC_EVENT_SHARE_BUTTON, Constants.SC_PAGE_TITLE_DETAILS, Constants.SC_SECTION_DETAILS);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "MMWR Weekly article via CDC's MMWR Express "
                + "mobile app.\n" + link);
        startActivity(shareIntent);
    }

    public void addArticleDetailFragment(){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(title, pubDate, volume, number, link);
        transaction.add(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_article_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_share) {
            AppManager.sc.trackEvent(Constants.SC_EVENT_SHARE_BUTTON, Constants.SC_PAGE_TITLE_DETAILS, Constants.SC_SECTION_DETAILS);
            share();
        }

        return super.onOptionsItemSelected(item);
    }
}
