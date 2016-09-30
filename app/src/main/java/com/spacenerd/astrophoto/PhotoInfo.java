package com.spacenerd.astrophoto;

public class PhotoInfo {
    private String mImageDate;
    private String mExplanation;
    private String mTitle;
    private String mImageURL;
    private String mCopyright;
    private String mMediaType;
    private String mImageHDURL;

    public String getImageDate() {
        return mImageDate;
    }

    public void setImageDate(String imageDate) {
        mImageDate = imageDate;
    }

    public String getExplanation() {
        return mExplanation;
    }

    public void setExplanation(String explanation) {
        mExplanation = explanation;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getImageURL() {
        return mImageURL;
    }

    public void setImageURL(String imageURL) {
        mImageURL = imageURL;
    }

    public String getCopyright() {
        return mCopyright;
    }

    public void setCopyright(String copyright) {
        mCopyright = copyright;
    }

    public String getMediaType() {
        return mMediaType;
    }

    public void setMediaType(String mediaType) {
        mMediaType = mediaType;
    }

    public String getImageHDURL() {
        return mImageHDURL;
    }

    public void setImageHDURL(String imageHDURL) {
        mImageHDURL = imageHDURL;
    }
}
