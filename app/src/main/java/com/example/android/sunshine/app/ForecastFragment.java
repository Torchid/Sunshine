package com.example.android.sunshine.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * Created by Rachel on 9/26/2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>  {

    public ForecastAdapter foreCastEntriesAdapter;
    private final int  LOADER_ID = 0;
    private final String POSITION_KEY = "posKey";
    int listPosition = 0;
    private ListView listOfForecasts;
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_view_map) {
            openLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openLocationInMap()
    {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if ( null != foreCastEntriesAdapter ) {
            Cursor c = foreCastEntriesAdapter.getCursor();
            if ( null != c ) {
                c.moveToPosition(0);
                String posLat = c.getString(WeatherContract.COL_COORD_LAT);
                String posLong = c.getString(WeatherContract.COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

        foreCastEntriesAdapter = new ForecastAdapter(getActivity(),null, 0);
        listOfForecasts = (ListView) rootView.findViewById(R.id.listview_forecast);
        listOfForecasts.setAdapter(foreCastEntriesAdapter);

        if(savedInstanceState != null)
            listPosition = savedInstanceState.getInt(POSITION_KEY);

        getLoaderManager().initLoader(LOADER_ID, null, this);

        listOfForecasts.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                listPosition = position;

                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(WeatherContract.COL_WEATHER_DATE)
                            ));
                }
            }
        });

        updateWeather();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(listPosition != ListView.INVALID_POSITION)
            outState.putInt(POSITION_KEY, listPosition);
    }

    private void updateWeather(){
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public void onLocationChanged(){
        updateWeather();
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                                weatherForLocationUri,
                                WeatherContract.FORECAST_COLUMNS,
                                null,
                                null,
                                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        foreCastEntriesAdapter.swapCursor(data);

        if (listPosition != ListView.INVALID_POSITION)
            listOfForecasts.smoothScrollToPosition(listPosition);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        foreCastEntriesAdapter.swapCursor(null);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onItemSelected(Uri dateUri);
    }
}
