package com.example.rad.moviesnow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A placeholder fragment containing a simple view.
 */
public class MoviesFragment extends Fragment {

    public static ArrayList<String> moviesPath = new ArrayList<String>();
    private ImageAdapter imagesMovie;
    private GridView moviesGrid;
    private String[] resultStrs;

    public MoviesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflater.inflate(R.menu.moviesfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_refresh) {
            updateMovies();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        imagesMovie = new ImageAdapter(getContext(), new ArrayList<String>());
        moviesGrid = (GridView) rootView.findViewById(R.id.gridview_movies);
        moviesGrid.setAdapter(imagesMovie);

        moviesGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String movieInfo =  resultStrs[position];
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, movieInfo);
                startActivity(intent);
            }
        });
        return rootView;

    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovies();
    }

    public void updateMovies(){
        FetchMovieTask fetchMovieTask = new FetchMovieTask();
        fetchMovieTask.execute("popular");
    }





    public class FetchMovieTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchMovieTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            //http://api.themoviedb.org/3/movie/popular?api_key=819f83791247478fee7f5e7beef93d07
            //http://api.themoviedb.org/3/movie/top_rated?api_key=819f83791247478fee7f5e7beef93d07
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sortType = sharedPref.getString(getString(R.string.pref_movie_sort_key),
                    getString(R.string.sort_popular));


            String moviesJsonStr = null;

            try{

                final String MOVIE_BASE_URL = "http://api.themoviedb.org/3/movie/" + sortType;
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(API_KEY_PARAM, BuildConfig.MOVIEDB_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                moviesJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Movie JSON string: " + moviesJsonStr);

            } catch (IOException e){
                Log.e(LOG_TAG, "Error ", e);
                moviesJsonStr = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            try {
                return getMovieDateFromJson(moviesJsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if(result != null) {
                moviesPath.clear();
                imagesMovie.clear();
                for (String movieStr : result) {
                    String path[] = movieStr.split(" - ");
                    moviesPath.add(path[0]);
                }
                imagesMovie.addAll(moviesPath);
            }
        }

        private String[] getMovieDateFromJson(String moviesJsonStr) throws JSONException {

            final String MDB_RESULTS = "results";
            final String MDB_POSTER_PATH = "poster_path";
            final String MDB_TITLE = "original_title";
            final String MDB_OVERVIEW = "overview";
            final String MDB_RELEASE = "release_date";
            final String MDB_RATING = "vote_average";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(MDB_RESULTS);
            resultStrs = new String[moviesArray.length()];

            for (int i = 0; i < moviesArray.length(); i++) {
                JSONObject movie = moviesArray.getJSONObject(i);
                resultStrs[i] = "http://image.tmdb.org/t/p/w500" + movie.getString(MDB_POSTER_PATH) + " - " + movie.getString(MDB_TITLE) + " - " + movie.getString(MDB_OVERVIEW) + " - " +
                        movie.getString(MDB_RELEASE) + " - "  + movie.getString(MDB_RATING);
            }
/*            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Movie entry: " + s);
            }*/
            return resultStrs;

        }
    }












    public class ImageAdapter extends ArrayAdapter {

        private Context context;
        private LayoutInflater inflater;

        private ArrayList<String> imageUrls;

        public ImageAdapter(Context context, ArrayList<String> imageUrls) {
            super(context, R.layout.grid_item_movie, imageUrls);

            this.context = context;
            this.imageUrls = imageUrls;

            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = inflater.inflate(R.layout.grid_item_movie, parent, false);
            }

            Picasso
                    .with(context)
                    .load(imageUrls.get(position))
                    .fit() // will explain later
                    .into((ImageView) convertView);

            return convertView;
        }
    }
}
