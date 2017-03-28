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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
    private LinkedList<Mood> preSortList;
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


        //This changes the title of the toolbar to the default of universal
        setFeedName("universal");

        // set up list view adapter
        context = this;
        mApplication = (Mood9Application) getApplicationContext();
        moodLinkedList = mApplication.getMoodLinkedList();
        preSortList = new LinkedList<>();

        ListView moodListView = (ListView) findViewById(R.id.moodList);
        moodListAdapter = new MoodListAdapter(this, moodLinkedList, mApplication);
        moodListView.setAdapter(moodListAdapter);
        populateFromMoodLoad(mApplication.getMoodModel().getUniversalUserMoods(null));


        moodListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //for list item clicked
                Intent viewMoodIntent = new Intent(FeedActivity.this, MoodViewActivity.class);
                viewMoodIntent.putExtra("moodIndex", position);
                startActivityForResult(viewMoodIntent, 1);
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
            public boolean onQueryTextSubmit(String query) { // TODO: search should only search based on what feed you are in
                ArrayList<Mood> reloadedMoods = universalQuery(query);
                populateFromMoodLoad(reloadedMoods);
                preSortList.clear();
                preSortList.addAll(moodLinkedList);
                moodListAdapter.notifyDataSetChanged();
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                moodLinkedList.clear();
                ArrayList<Mood> reloadedMoods;
                if (toolbar.getTitle().toString().compareTo("Personal Feed") == 0) // TODO: implement if toolbar is on follows here
                    reloadedMoods = mApplication.getMoodModel().getCurrentUserMoods();
                else
                    reloadedMoods = mApplication.getMoodModel().getUniversalUserMoods(null);
                populateFromMoodLoad(reloadedMoods);
                preSortList.clear();
                moodListAdapter.notifyDataSetChanged();
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
        } else if (id == R.id.sort_last_day) { // TODO: need to be able to sort and resort without reloading the list we are sorting
            setMoodsByDate("day");
        } else if (id == R.id.sort_last_week) {
            setMoodsByDate("week");
        } else if (id == R.id.sort_last_month) {
            setMoodsByDate("month");
        } else if (id == R.id.sort_last_year) {
            setMoodsByDate("year");
        } else if (id == R.id.sort_forever) {
            preSortListUpdate();
            sortDisplayByDate();
        } else if (id == R.id.filter_emotions) {
            openFilterDialog("emotions");
        } else if (id == R.id.filter_socials) {
            openFilterDialog("socials");
        }

        return super.onOptionsItemSelected(item);
    }
    private ArrayList<Mood> personalQuery(String query) {
        String returnConversion = queryConverter(query);
        ArrayList<Mood> reloadedMoods = new ArrayList<>();
        if (returnConversion.substring(0, returnConversion.indexOf(':')).compareTo("emotionId") == 0) {
            for (Mood mood : moodLinkedList) {
                if (mood.getEmotionId().compareTo(returnConversion.substring(returnConversion.indexOf(':')+1)) == 0) {
                    reloadedMoods.add(mood);
                }
            }
        } else if (returnConversion.substring(0, returnConversion.indexOf(':')).compareTo("socialSituationId") == 0) {
            for (Mood mood : moodLinkedList) {
                if (mood.getSocialSituationId().compareTo(returnConversion.substring(returnConversion.indexOf(':') + 1)) == 0) {
                    reloadedMoods.add(mood);
                }
            }
        } else if (returnConversion.substring(0, returnConversion.indexOf(':')).compareTo("user_id") == 0) {
            for (Mood mood : moodLinkedList) {
                if (mood.getUser_id().compareTo(returnConversion.substring(returnConversion.indexOf(':') + 1)) == 0) {
                    reloadedMoods.add(mood);
                }
            }
        } else if (returnConversion.substring(0, returnConversion.indexOf(':')).compareTo("trigger") == 0) {
            for (Mood mood : moodLinkedList) {
                if (mood.getTrigger().compareTo(returnConversion.substring(returnConversion.indexOf(':') + 1)) == 0) {
                    reloadedMoods.add(mood);
                }
            }
        }
        return reloadedMoods;
    }

    private ArrayList<Mood> universalQuery(String query) {
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
        return reloadedMoods;
    }

    private void openFilterDialog(String filterType) {
        final String[] items = checkBoxDateInit(filterType);
        final ArrayList<String> selectedItems = new ArrayList();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Filter Parameters")
                .setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            selectedItems.add(items[which]);
                        } else if (selectedItems.contains(items[which])) {
                            selectedItems.remove(selectedItems.indexOf(items[which]));
                        }
                    }
                }).setPositiveButton("Filter", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // SAVE SELECTED ITEMS AND QUERY HERE
                        ArrayList<Mood> reloadedMoods = new ArrayList<Mood>();
                        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                        if (toolbar.getTitle().toString().compareTo("Universal Feed") == 0) {
                            for (int i = 0; i < selectedItems.size(); i++) {
                                reloadedMoods.addAll(universalQuery(selectedItems.get(i)));
                            }
                        } else { // We are in the personal feed so lets just search in the list, deal with followers later.
                            for (int i = 0; i < selectedItems.size(); i++) {
                                reloadedMoods.addAll(personalQuery(selectedItems.get(i))); // check this over, may need to fix reloading of prefilter personal list
                            }
                        }
                        populateFromMoodLoad(reloadedMoods);
                        preSortList.clear();
                        preSortList.addAll(moodLinkedList);
                        moodListAdapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // CANCELED SO BACK OUT
                    }
                }).create();
        dialog.show();
    }

    private String[] checkBoxDateInit(String dateType) {
        String[] checkBoxItems = new String[0];
        if (dateType.compareTo("emotions") == 0) {
            checkBoxItems = new String[mApplication.getEmotionModel().getEmotions().size()];
            for (Map.Entry<String, Emotion> entry : mApplication.getEmotionModel()
                    .getEmotions().entrySet()) {
                checkBoxItems[Integer.parseInt(entry.getKey())] = entry.getValue().getName();
            }
        } else if (dateType.compareTo("socials") == 0) {
            checkBoxItems = new String[mApplication.getSocialSituationModel()
                    .getSocialSituations().entrySet().size()];
            for (Map.Entry<String, SocialSituation> entry : mApplication.getSocialSituationModel()
                    .getSocialSituations().entrySet()) {
                checkBoxItems[Integer.parseInt(entry.getKey())] = entry.getValue().getName();
            }
        }
        return checkBoxItems;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.personal) {
            // Handle the camera action
            setFeedName("personal");
            ArrayList<Mood> temp = mApplication.getMoodModel().getCurrentUserMoods();
            populateFromMoodLoad(temp);
        } else if (id == R.id.followed) {
            setFeedName("followed");
            moodLinkedList.clear();
        } else if (id == R.id.universal) {
            setFeedName("universal");
            ArrayList<Mood> temp = mApplication.getMoodModel().getUniversalUserMoods(null);
            populateFromMoodLoad(temp);
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
        } else if (id == R.id.stats) {
            Intent statsActivity = new Intent(this, StatsActivity.class);
            startActivity(statsActivity);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setFeedName(String newName) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (newName == "universal")
            toolbar.setTitle(R.string.universal_feed);
        else if (newName == "personal")
            toolbar.setTitle(R.string.personal_feed);
        else if (newName == "followed")
            toolbar.setTitle(R.string.followed_feed);
        return;
    }

    private void preSortListUpdate() {
        moodLinkedList.clear();
        if (preSortList.size() > 0) { // this string of conditions sets it up so that the original list of moods is reloaded for resorting
            moodLinkedList.addAll(preSortList);
        } else {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            String title = toolbar.getTitle().toString();
            if (title.compareTo("Universal Feed") == 0) {
                moodLinkedList.addAll(mApplication.getMoodModel().getUniversalUserMoods(null));
            } else if (title.compareTo("Personal Feed") == 0) {
                moodLinkedList.addAll(mApplication.getMoodModel().getCurrentUserMoods());
            } // TODO: else get follower moods
        }
    }
    private void setMoodsByDate(String range) {
        preSortListUpdate();
        sortDisplayByDate();
        Date target = new Date();
        Calendar cal = Calendar.getInstance();
        if (range == "day") {
            cal.add(Calendar.DATE, -1);
            target = cal.getTime();
        } else if (range == "week") {
            cal.add(Calendar.DATE, -7);
            target = cal.getTime();
        } else if (range == "month") {
            cal.add(Calendar.MONTH, -1);
            target = cal.getTime();
        } else if (range == "year") {
            cal.add(Calendar.YEAR, -1);
            target = cal.getTime();
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        LinkedList<Mood> temp = new LinkedList<>();
        for (Mood entry : moodLinkedList) {
            try {
                if (dateFormat.parse(entry.getDate()).after(target)) {
                    temp.add(entry);
                }
            } catch (ParseException e) {
                // PARSE FAILED
            }
        }
        moodLinkedList.clear();
        moodLinkedList.addAll(temp);
        moodListAdapter.notifyDataSetChanged();
    }

    private void sortDisplayByDate() {
        Collections.sort(moodLinkedList, new Comparator<Mood>(){
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            @Override
            public int compare(Mood mood1, Mood mood2){
                try {
                    return dateFormat.parse(mood2.getDate()).compareTo(dateFormat.parse(mood1.getDate()));
                } catch (ParseException e) {
                    // PARSE FAILED
                }
                return 0;
            }
        });
        moodListAdapter.notifyDataSetChanged();
    }

    private void addMood() {
        Intent addMoodIntent = new Intent(this, AddMoodActivity.class);
        addMoodIntent.putExtra("editCheck", 0);
        startActivityForResult(addMoodIntent, 0);
    }

    private void populateFromMoodLoad(ArrayList<Mood> newMoods) {
        moodLinkedList.clear();
        //LOADING FROM ELASTIC SEARCH
        for (int i = 0; i < newMoods.size(); i++) {
            moodLinkedList.add(newMoods.get(i));
        }
        moodListAdapter.notifyDataSetChanged();
    }
    private String queryConverter(String query) {
        // Convert user queries into usable id search queries
        String queryConverted = query;
        ArrayList<Mood> universalMoods = mApplication.getMoodModel().getUniversalUserMoods(null);
        ArrayList<String> names = new ArrayList<String>();
        names = UserModel.getAllUsers();

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
