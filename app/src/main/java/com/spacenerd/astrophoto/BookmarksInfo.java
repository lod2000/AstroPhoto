package com.spacenerd.astrophoto;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class BookmarksInfo extends Activity{

    public static final String TAG = BookmarksInfo.class.getSimpleName();

    private List<String> mBookmarkDateList;
    private List<String> mBookmarkTitleList;
    private List<String> mBookmarkURLList;
    private int mBookmarkCount;
    private String sep = ";";

    private SharedPreferences mBookmarkPreferences;

    public void getBookmarkPreferences(Context context) {
        mBookmarkPreferences = context.getSharedPreferences(String.valueOf(R.string.bookmarks_file_key), Context.MODE_PRIVATE);
        getBookmarkLists();
    }

    public void getBookmarkLists(){
        mBookmarkCount = mBookmarkPreferences.getInt("bookmarkCount", 0);
        mBookmarkDateList = new ArrayList<>(Arrays.asList(mBookmarkPreferences.getString("bookmarkDates", "").split(sep)));
        mBookmarkTitleList = new ArrayList<>(Arrays.asList(mBookmarkPreferences.getString("bookmarkTitles", "").split(sep)));
        mBookmarkURLList = new ArrayList<>(Arrays.asList(mBookmarkPreferences.getString("bookmarkURLs", "").split(sep)));
    }

    public void setBookmarkPreferences() {
        android.content.SharedPreferences.Editor editor = mBookmarkPreferences.edit();
        editor.putString("bookmarkDates", concatList(mBookmarkDateList));
        editor.putString("bookmarkTitles", concatList(mBookmarkTitleList));
        editor.putString("bookmarkURLs", concatList(mBookmarkURLList));
        editor.putInt("bookmarkCount", mBookmarkCount);
        editor.apply();
    }

    public void clearBookmarks(){
        SharedPreferences.Editor editor = mBookmarkPreferences.edit();
        editor.clear();
        editor.apply();

        getBookmarkLists();
    }

    public void addBookmark(String date, String title, String URL){
        mBookmarkDateList.add(mBookmarkCount, date);
        mBookmarkTitleList.add(mBookmarkCount, title);
        mBookmarkURLList.add(mBookmarkCount, URL);
        mBookmarkCount++;

        setBookmarkPreferences();

//        sortByDate(); //Untested!!!
    }

    public void removeBookmark(String date){
        int index = -1;
        for (int i = 0; i < mBookmarkCount; i++){
            if (mBookmarkDateList.get(i).equals(date)){
                index = i;
                break;
            }
        }
        if (index >= 0){
            mBookmarkDateList.remove(index);
            mBookmarkTitleList.remove(index);
            mBookmarkURLList.remove(index);
            mBookmarkCount--;
            setBookmarkPreferences();
        }
        else {
            Log.w(TAG, "Could not remove " + date + ", is not currently bookmarked");
        }
    }

    public boolean isBookmarked(String date, Context context){
        getBookmarkPreferences(context);
        boolean isBookmarked = false;
        for (int i = 0; i < mBookmarkCount; i++){
            if (mBookmarkDateList.get(i).equals(date)){
                isBookmarked = true;
                Log.d(TAG, "isBookmarked true");
                break;
            }
        }
        return isBookmarked;
    }

    public String concatList(List<String> list){
        if (mBookmarkCount > 0){
            String string = list.get(0);
            for (int i = 1; i < list.size(); i++){
                string+=sep;
                string+=list.get(i);
            }
            return string;
        }
        else {
            return null;
        }

    }

    public void sortByDate(){
        List<String> tempDates = mBookmarkDateList;
        List<String> tempTitles = new ArrayList<>();
        List<String> tempURLs = new ArrayList<>();
        java.util.Collections.sort(tempDates);
        for (int i = 0; i < mBookmarkCount; i++){ //tempDates
            for (int n = 0; n < mBookmarkCount; n++){ //mBookmarkDateList
                if (tempDates.get(i).equals(mBookmarkDateList.get(n))){
                    tempTitles.add(i, mBookmarkTitleList.get(n));
                    tempURLs.add(i, mBookmarkURLList.get(n));
                }
            }
        }
        mBookmarkDateList = tempDates;
        mBookmarkTitleList = tempTitles;
        mBookmarkURLList = tempURLs;

        setBookmarkPreferences();
    }

    public List<String> getBookmarkTitleList() {
        return mBookmarkTitleList;
    }

    public void setBookmarkTitleList(List<String> bookmarkTitleList) {
        mBookmarkTitleList = bookmarkTitleList;
    }

    public List<String> getBookmarkURLList() {
        return mBookmarkURLList;
    }

    public void setBookmarkURLList(List<String> bookmarkURLList) {
        mBookmarkURLList = bookmarkURLList;
    }

    public List<String> getBookmarkDateList() {
        return mBookmarkDateList;
    }

    public void setBookmarkDateList(List<String> bookmarkDateList) {
        mBookmarkDateList = bookmarkDateList;
    }

    public int getBookmarkCount(){
        return mBookmarkCount;
    }

    public void setBookmarkCount(int bookmarkCount){
        mBookmarkCount = bookmarkCount;
    }

}
