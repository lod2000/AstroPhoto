package com.spacenerd.astrophoto;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import android.view.*;
import android.widget.*;

import com.squareup.picasso.Picasso;

import org.json.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.*;
import uk.co.senab.photoview.PhotoViewAttacher;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private PhotoInfo mPhotoInfo;

    private BookmarksInfo mBookmarksInfo;

    ViewFlipper mViewFlipper;
    private boolean onFullImage;
    private ImageView mFullImageView;
    private boolean onFullDescription;
    private TextView mFullDescription;

    private ImageView mPhotoImageView;
    private TextView mTitleLabel;
    private TextView mDescriptionLabel;
    private TextView mDateLabel;
    private TextView mLinkView;
    private ImageView mBookmarkStarEmpty;
    private ImageView mBookmarkStarFilled;

    private boolean useHDURL;

    private SimpleDateFormat mSimpleDateFormat;
    private SimpleDateFormat mHumanDateFormat;
    private Date todayDate;
    private String todayDateString;
    private Date currentDate;
    private Calendar cal;
    private String currentDateString;
    private String currentDateHuman;

    PhotoViewAttacher mAttacher;

    private ActionMode mActionMode;

    ListView mListView;
    List<String> combinedList;

    Context context;
//    ImageView mThumbnailView;

    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = MainActivity.this;

        useHDURL = false;

        //Initialize the ViewFlipper
        mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        onFullImage = false;
        onFullDescription = false;

        //Initialize ImageViews and TextViews in mainLayout
        mPhotoImageView = (ImageView) findViewById(R.id.photoImageView);
        mTitleLabel = (TextView) findViewById(R.id.titleLabel);
        mDescriptionLabel = (TextView) findViewById(R.id.descriptionLabel);
        mDateLabel = (TextView) findViewById(R.id.dateLabel);
        mLinkView = (TextView) findViewById(R.id.linkView);

        //Initialize TextView for video links
        mLinkView.setMovementMethod(LinkMovementMethod.getInstance());
        mLinkView.setClickable(true);
        mLinkView.setVisibility(View.INVISIBLE);

        //Initialize ImageView for full-screen image and zooming
        mFullImageView = (ImageView) findViewById(R.id.fullImageView);
        mAttacher = new PhotoViewAttacher(mFullImageView);

        //Initialize full description TextView
        mFullDescription = (TextView) findViewById(R.id.fullDescription);
        mFullDescription.setMovementMethod(new ScrollingMovementMethod());

        //Assign drawables to ImageViews for empty and filled bookmark stars
        mBookmarkStarEmpty = (ImageView) findViewById(R.id.bookmarkStarEmpty);
        mBookmarkStarFilled = (ImageView) findViewById(R.id.bookmarkStarFilled);
        mBookmarkStarFilled.setVisibility(View.INVISIBLE);
        Drawable starEmptyDrawable = ContextCompat.getDrawable(this, R.drawable.ic_star_border_white_24dp);
        Drawable starFilledDrawable = ContextCompat.getDrawable(this, R.drawable.ic_star_white_24dp);
        mBookmarkStarEmpty.setImageDrawable(starEmptyDrawable);
        mBookmarkStarFilled.setImageDrawable(starFilledDrawable);

        cal = Calendar.getInstance();
        mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        //A human-readable date format, e.g. May 2, 2016
        mHumanDateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        todayDate = new Date();
        currentDate = todayDate;
        todayDateString = mSimpleDateFormat.format(todayDate);
        currentDateString = mSimpleDateFormat.format(currentDate);
        currentDateHuman = mHumanDateFormat.format(currentDate);

        mListView = (ListView) findViewById(R.id.listView);

        //mThumbnailView = (ImageView) findViewById(R.id.thumbnailView);

        //Assign drawables to ImageViews for the back buttons
        ImageView backButton = (ImageButton) findViewById(R.id.backButton);
        Drawable backDrawable = ContextCompat.getDrawable(this, R.drawable.arrow_left);
        backButton.setImageDrawable(backDrawable);

        //Set the back button to go back one day
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cal.setTime(currentDate);
                cal.add(Calendar.DATE, -1);
                currentDate = cal.getTime();
                currentDateString = mSimpleDateFormat.format(currentDate);
                currentDateHuman = mHumanDateFormat.format(currentDate);
                getPicture(currentDateString);
            }
        });

        //Assign drawable to ImageView for the forward button
        ImageView forwardButton = (ImageButton) findViewById(R.id.forwardButton);
        Drawable forwardDrawable = ContextCompat.getDrawable(this, R.drawable.arrow_right);
        forwardButton.setImageDrawable(forwardDrawable);

        //Set the forward button to go forward one day, unless it is on the current day
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cal.setTime(currentDate);
                cal.add(Calendar.DATE, 1);
                currentDate = cal.getTime();
                if (currentDate.getTime() <= todayDate.getTime()) {
                    currentDateString = mSimpleDateFormat.format(currentDate);
                    currentDateHuman = mHumanDateFormat.format(currentDate);
                    getPicture(currentDateString);

                } else { //View is already on the most current image
                    cal.add(Calendar.DATE, -1);
                    currentDate = cal.getTime();
                    Toast.makeText(context, "This is the latest APOD image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //If the empty bookmark star is clicked, bookmark the image and switch to filled star
        mBookmarkStarEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBookmarkStar();
                mBookmarksInfo.addBookmark(mPhotoInfo.getImageDate(), mPhotoInfo.getTitle(), mPhotoInfo.getImageURL());
                Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show();
                Log.v(TAG, "Bookmark added: " + currentDateString);
                Log.v(TAG, "Bookmarks remaining: " + mBookmarksInfo.getBookmarkCount());
            }
        });

        //If the filled bookmark star is clicked, un-bookmark the image and switch to empty star
        mBookmarkStarFilled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBookmarkStar();
                mBookmarksInfo.removeBookmark(currentDateString);
                Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show();
                Log.v(TAG, "Bookmark removed: " + currentDateString);
                Log.v(TAG, "Bookmarks remaining: " + mBookmarksInfo.getBookmarkCount());
            }
        });

        mPhotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPhotoClick();
                mActionMode = MainActivity.this.startActionMode(new ActionBarCallback());
            }
        });

        mDescriptionLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDescriptionClick();
                mActionMode = MainActivity.this.startActionMode(new ActionBarCallback());
            }
        });

        refresh();

    }

    private void refresh(){
        getPicture(todayDateString);

        mBookmarksInfo = getBookmarksInfo();

        if (mBookmarksInfo.isBookmarked(todayDateString, context)){
            showBookmarkStar();
        }
    }

    class ActionBarCallback implements ActionMode.Callback {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(mPhotoInfo.getTitle());

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (onFullImage){
                onPhotoClick();
            }
            else if (onFullDescription){
                onDescriptionClick();
            }
            mActionMode = null;
        }
    }

    class BookmarksActionBar implements ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("My Bookmarks");

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.bookmarks_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getPicture(currentDateString);
            updateDisplay();
            mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.mainLayout)));
            mActionMode = null;
        }
    }

//    @Override
    public Dialog networkUnavailableDialog() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.network_unavailable_message)
                .setPositiveButton(R.string.ok_response, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicks away dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.gotoToday:
                getPicture(todayDateString);
                currentDate = todayDate;
                currentDateString = mSimpleDateFormat.format(currentDate);
                if (mBookmarksInfo.isBookmarked(mPhotoInfo.getImageDate(), context)){
                    showBookmarkStar();
                }
                else {
                    hideBookmarkStar();
                }
                return true;
            case R.id.clearBookmarks:
                mBookmarksInfo.clearBookmarks();
                hideBookmarkStar();
                Toast.makeText(context, "Bookmarks cleared", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_save:
                if (mPhotoInfo.getMediaType().equals("image")){
                    saveToGallery();
                }
                else {
                    Toast.makeText(context, "Can't save: Not an image", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_view_bookmarks:
                showBookmarks();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void getPicture(String date){

        String astroURL = "https://api.nasa.gov/planetary/apod?api_key=" + getString(R.string.api_key) + "&date=" + date;

        if (isNetworkAvailable()){
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(astroURL).build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(context, "There was an error", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mPhotoInfo = getPhotoInfo(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "There was an error", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    } catch (IOException | JSONException e){
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });

        }
        else {
            Toast.makeText(this, "Network is Unavailable", Toast.LENGTH_LONG).show();
//            networkUnavailableDialog();
        }
    }

    private void updateDisplay(){
        if (mPhotoInfo.getMediaType().equals("image")){
            if (mPhotoImageView.getVisibility() == View.INVISIBLE){
                toggleImage();
            }
            Picasso.with(this).load(mPhotoInfo.getImageURL()).into(mPhotoImageView);
        }
        else {
            Log.v(TAG, "Video playback not supported, link displayed instead");
            toggleImage();
            String link = "<a href='" + mPhotoInfo.getImageURL() + "'> Video Link </a>";
            mLinkView.setText(Html.fromHtml(link));
        }
        mTitleLabel.setText(mPhotoInfo.getTitle());
        mDescriptionLabel.setText(mPhotoInfo.getExplanation());
        currentDateHuman = mHumanDateFormat.format(currentDate);
        mDateLabel.setText(currentDateHuman);

        //Show star if the date is bookmarked
        if (mBookmarksInfo.isBookmarked(currentDateString, context)) {
            showBookmarkStar();
        } else {
            hideBookmarkStar();
        }
    }

    private PhotoInfo getPhotoInfo(String jsonData) throws JSONException {
        JSONObject astroInfo = new JSONObject(jsonData);

        PhotoInfo photoInfo = new PhotoInfo();
        photoInfo.setExplanation(astroInfo.getString("explanation"));
        photoInfo.setImageDate(astroInfo.getString("date"));
        photoInfo.setImageURL(astroInfo.getString("url"));
        photoInfo.setMediaType(astroInfo.getString("media_type"));
        photoInfo.setTitle(astroInfo.getString("title"));
        if (photoInfo.getMediaType().equals("image") && useHDURL){
            photoInfo.setImageHDURL(astroInfo.getString("hdurl"));
        }

        return photoInfo;
    }

    private BookmarksInfo getBookmarksInfo(){
        BookmarksInfo bookmarksInfo = new BookmarksInfo();
        bookmarksInfo.getBookmarkPreferences(context);

        return bookmarksInfo;
    }

    private void onPhotoClick(){
        if (onFullImage){ //If photo is fullscreen and is clicked
            mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.mainLayout)));
            onFullImage = false;
        }
        else { //If photo is not fullscreen and is clicked
            mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.fullImageLayout)));
            if (useHDURL){
                Picasso.with(this).load(mPhotoInfo.getImageHDURL()).into(mFullImageView);
            }
            else {
                Picasso.with(this).load(mPhotoInfo.getImageURL()).into(mFullImageView);
            }
            mAttacher.update();
            onFullImage = true;
        }
    }

    private void onDescriptionClick(){
        if (onFullDescription){
            mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.mainLayout)));
            onFullDescription = false;
        }
        else {
            mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.fullDescriptionLayout)));
            mFullDescription.setText(mPhotoInfo.getExplanation());
            onFullDescription = true;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

    private void toggleImage(){
        if (mLinkView.getVisibility() == View.INVISIBLE){
            mLinkView.setVisibility(View.VISIBLE);
            mPhotoImageView.setVisibility(View.INVISIBLE);
        }
        else {
            mLinkView.setVisibility(View.INVISIBLE);
            mPhotoImageView.setVisibility(View.VISIBLE);
        }
    }

    private void showBookmarkStar(){
        mBookmarkStarFilled.setVisibility(View.VISIBLE);
        mBookmarkStarEmpty.setVisibility(View.INVISIBLE);
    }

    private void hideBookmarkStar(){
        mBookmarkStarFilled.setVisibility(View.INVISIBLE);
        mBookmarkStarEmpty.setVisibility(View.VISIBLE);
    }

    private void showBookmarks() {
        mActionMode = MainActivity.this.startActionMode(new BookmarksActionBar());
        mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.bookmarksLayout)));

        mBookmarksInfo.getBookmarkPreferences(this);

//        List<String> combinedList = new ArrayList<>();
        combinedList = new ArrayList<>();
        for (int i = 0; i < mBookmarksInfo.getBookmarkCount(); i++){
            combinedList.add(i, mBookmarksInfo.getBookmarkDateList().get(i) + " - " + mBookmarksInfo.getBookmarkTitleList().get(i));
        }
        Collections.sort(combinedList);
        Collections.reverse(combinedList);

        ArrayAdapter adapter = new ArrayAdapter<>(this, R.layout.bookmarks_layout,
                R.id.bookmarkDate, combinedList);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.d(TAG, "Position: " + position);
//                String selected = listText.getText().toString();
                String selected = combinedList.get(position);
                currentDateString = selected.split(" - ")[0];
                try {
                    currentDate = mSimpleDateFormat.parse(currentDateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, selected);

//                mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.mainLayout)));
                mActionMode.finish();
                getPicture(currentDateString);
                updateDisplay();
            }
        });
    }

    private void saveToGallery(){
        /*mPhotoImageView.setDrawingCacheEnabled(true);
        Bitmap bitmap = mPhotoImageView.getDrawingCache();
        //MediaStore.Images.Media.insertImage(getContentResolver(), yourBitmap, yourTitle , yourDescription);
//        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
//
//        }
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, mPhotoInfo.getTitle(), mPhotoInfo.getExplanation());
        Log.v(TAG, "Image saved to gallery");
        Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show();*/
        Log.d(TAG, "Saving images is WIP");
        Toast.makeText(this, "Sorry, can't save pictures right now", Toast.LENGTH_SHORT).show();
    }

    private File createImageFile() throws IOException { //WIP
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
        String imageFileName = "JPEG_" + mPhotoInfo.getImageDate() + "_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void saveToGalleryWIP() { //WIP
        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Log.e(TAG, "Error attempting to create image File");
            Toast.makeText(this, "Could not save image", Toast.LENGTH_SHORT).show();
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            //Add image to gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
            Toast.makeText(this, "Image saved to Gallery", Toast.LENGTH_SHORT).show();
        }

    }
}
