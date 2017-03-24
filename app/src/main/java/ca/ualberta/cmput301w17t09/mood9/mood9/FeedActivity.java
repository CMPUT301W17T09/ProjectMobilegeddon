package ca.ualberta.cmput301w17t09.mood9.mood9;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Originally created by :
 * Modified by cdkushni on 3/5/17 and 3/8/17 to implement MoodListAdapter and data bundle receipt from addMood. Also made to inflate layout to a listview in the feed.
 * Modified by cdkushni on 3/10/17 to access the global application for global Models, changed over to using resource files for emotions and social situations,
 * started updating MoodModel along with linkedList of moods
 * Modified by cdkushni on 3/18/17 to incorporate a expandable search action bar item to search queries in elastic search
 * Modified by cdkushni on 3/20/17 to return search queries to main feed and load it into the display adapter, also encapsulated default mood load for easy reloads
 * Also, kept using moodLinkedList so that we can use the linkedList as a displayer that can be cleared when searching without affecting the moodModel which holds the default moods
 * Fixed some bugs with shared preferences that came up upon new accounts after a clear data
 * Disabled editing mood events while searching. Instead clicking on a mood will bring up a dialog window with username, trigger and social description.
 * Modified by cdkushni on 3/20/17 to start a new function that will take input queries and convert it to usable id queries
 */
public class FeedActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private MoodListAdapter moodListAdapter;
    private LinkedList<Mood> moodLinkedList;
    private Mood9Application mApplication;
    private int searching;
    Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMood();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //This changes the title of the toolbar - need to change according to feed shown
        toolbar.setTitle(R.string.universal_feed);

        // set up list view adapter
        context = this;
        mApplication = (Mood9Application) getApplicationContext();
        moodLinkedList = mApplication.getMoodLinkedList();
        searching = 0;
        ArrayList<Mood> temp = mApplication.getMoodModel().getUniversalUserMoods(null);
        //LOADING FROM ELASTIC SEARCH
        for (int i = 0; i < temp.size(); i++) {
            moodLinkedList.add(temp.get(i));
        }

        ListView moodListView = (ListView) findViewById(R.id.moodList);
        moodListAdapter = new MoodListAdapter(this, moodLinkedList, mApplication);
        moodListView.setAdapter(moodListAdapter);

        moodListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //for list item clicked
                if (searching == 0) {
                    Intent editMoodIntent = new Intent(FeedActivity.this, AddMoodActivity.class);
                    editMoodIntent.putExtra("editCheck", 1);
                    editMoodIntent.putExtra("moodIndex", position);
                    startActivityForResult(editMoodIntent, 1);
                } else {
                    // TODO: open up a detail view of mood data
                    String trigger = moodLinkedList.get(position).getTrigger();
                    String socialSit = mApplication.getSocialSituationModel()
                            .getSocialSituation(moodLinkedList.get(position).getSocialSituationId())
                            .getDescription();
                    AlertDialog.Builder detailBuild = new AlertDialog.Builder(context)
                            .setTitle(UserModel.getUserProfile(moodLinkedList.get(position)
                                    .getUser_id()).getName())
                            .setMessage(trigger + "\n" + socialSit)
                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog detailDialog = detailBuild.create();
                    detailDialog.show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feed, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (null != searchView) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
        }
        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered
                return true;
            }
            public boolean onQueryTextSubmit(String query) {
                searching = 1;
                String returnConversion = queryConverter(query);
                // TODO: move this call to the queryConverter so that you
                // TODO: can grab multiple possible queries in one search,
                // TODO: so query every time something is found and add to list
                HashMap<String, String> queryHash = new HashMap<>();
                queryHash.put(returnConversion.substring(0,returnConversion.indexOf(':')),
                        returnConversion.substring(returnConversion.indexOf(':')+1));
                moodLinkedList.clear();
                ArrayList<Mood> reloadedMoods = mApplication.getMoodModel()
                        .getUniversalUserMoods(queryHash);
                populateFromMoodLoad(reloadedMoods);
                moodListAdapter.notifyDataSetChanged();
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                moodLinkedList.clear();
                mApplication.getMoodModel().getCurrentUserMoods().clear();
                ArrayList<Mood> reloadedMoods = mApplication.getMoodModel().getCurrentUserMoods();
                populateFromMoodLoad(reloadedMoods);
                moodListAdapter.notifyDataSetChanged();
                searching = 0;
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.search) {
            //TODO Add functionality to the search button
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.personal) {
            // Handle the camera action
        } else if (id == R.id.followed) {

        } else if (id == R.id.universal) {

        } else if (id == R.id.near_me) {
            Intent mapIntent = new Intent(this, MapsActivity.class);
            mapIntent.putExtra("moodList", moodLinkedList);
            startActivity(mapIntent);
        } else if (id == R.id.profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileIntent);
        } else if (id == R.id.about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void addMood() {
        Intent addMoodIntent = new Intent(this, AddMoodActivity.class);
        addMoodIntent.putExtra("editCheck", 0);
        startActivityForResult(addMoodIntent, 0);
    }

    private void populateFromMoodLoad(ArrayList<Mood> newMoods) {
        //LOADING FROM ELASTIC SEARCH
        for (int i = 0; i < newMoods.size(); i++) {
            moodLinkedList.add(newMoods.get(i));
        }
    }
    private String queryConverter(String query) {
        // Convert user queries into usable id search queries
        String queryConverted = query;
        ArrayList<Mood> universalMoods = mApplication.getMoodModel().getUniversalUserMoods(null);
        ArrayList<String> names = new ArrayList<String>();
        names = UserModel.getAllUsers();/*
        for (Mood item : universalMoods) {
            String currentName = UserModel.getUserProfile(item.getUser_id()).getName();
            if (names.size() > 0) {
                int old = 0;
                for (int i = 0; i < names.size(); i++) {
                    if (currentName == names.get(i)) {
                        old = 1;
                        break;
                    }
                }
                if (old == 0) {
                    names.add(currentName);
                }
            } else {
                names.add(currentName);
            }
        }*/

        for (Map.Entry<String, SocialSituation> entry : mApplication.getSocialSituationModel()
                .getSocialSituations().entrySet()) {
            if (entry.getValue().getName().toLowerCase()
                    .contains(query.toLowerCase()) || entry.getValue().getDescription()
                    .toLowerCase().contains(query.toLowerCase())) {
                queryConverted = "socialSituationId:"+entry.getValue().getId();
                return queryConverted;
            }
        }
        for (Map.Entry<String, Emotion> entry: mApplication.getEmotionModel()
                .getEmotions().entrySet()) {
            if (entry.getValue().getName().toLowerCase()
                    .contains(query.toLowerCase()) || entry.getValue().getDescription()
                    .toLowerCase().contains(query.toLowerCase())) {
                queryConverted = "emotionId:"+entry.getValue().getId();
                return queryConverted;
            }
        }
        for (String name : names) {
            if (name.toLowerCase().contains(query.toLowerCase())) {
                queryConverted = "user_id:"+UserModel.getUserID(name);
                return queryConverted;
            }
        }
        queryConverted = "trigger:"+query;

        return queryConverted;
    }

    // Code Documentation found here: https://developer.android.com/reference/android/app/Activity.html
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // requestcode 0 is from adding a new mood
        if (requestCode == 0) {

            LayoutInflater inflater = (LayoutInflater)this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.fragment_mood,null);

            TextView username = (TextView)view.findViewById(R.id.username);
            username.setTypeface(null, Typeface.BOLD);
            moodListAdapter.notifyDataSetChanged();

        } else {
            if (requestCode == 1) {
                moodListAdapter.notifyDataSetChanged();
            }
        }
    }
}
