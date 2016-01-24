package com.josephcmontgomery.bookscanner;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.josephcmontgomery.bookscanner.Database.BookScannerContract;
import com.josephcmontgomery.bookscanner.Tools.ImageFetcher;

public class DataCursorAdapter extends CursorAdapter {
    public DataCursorAdapter(Context context, Cursor cursor, int flags){
        super(context, cursor, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if(cursor.getPosition()%2==1) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.darkView));
        }
        else{
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.lightView));
        }
        setImage(view, cursor);
        setLocation(view, cursor);
        setTitle(view, cursor);
    }

    private void setImage(View view, Cursor cursor){
        final ImageView icon = (ImageView) view.findViewById(R.id.book_cover_image);
        final String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(BookScannerContract.Books.COLUMN_NAME_IMAGE_URL));
        ImageFetcher.loadImage(imageUrl, icon);
    }

    private void setLocation(View view, Cursor cursor){
        TextView location = (TextView) view.findViewById(R.id.book_location_entry);
        String strLocation = cursor.getString(cursor.getColumnIndexOrThrow(BookScannerContract.Books.COLUMN_NAME_LOCATION));
        location.setText(strLocation);
    }

    private void setTitle(View view, Cursor cursor){
        TextView title = (TextView) view.findViewById(R.id.book_title_entry);
        String strTitle = cursor.getString(cursor.getColumnIndexOrThrow(BookScannerContract.Books.COLUMN_NAME_TITLE));
        title.setText(strTitle);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }
}
