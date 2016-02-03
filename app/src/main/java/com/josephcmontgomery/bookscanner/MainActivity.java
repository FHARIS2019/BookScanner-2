package com.josephcmontgomery.bookscanner;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.josephcmontgomery.bookscanner.Tools.BookInformation;
import com.josephcmontgomery.bookscanner.Tools.BookJsonParser;
import com.josephcmontgomery.bookscanner.Tools.ViewMode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

//TODO: Handle database with non-isbn barcodes. Inform user of book not found. Add barcode library.
//TODO: Adjust for different screen sizes. Deal with no internet connection.
//TODO: Figure out activity result fail error. Figure out error on exiting app.
//TODO: Guard against SQL injection. Change database to persist.
//TODO: Check security issues for mobile apps.
//TODO: Get API key for Google Books. Check how it handles a lot of requests.
//TODO: Profile performance on memory and cpu, and download size.
//TODO: Add way to manually add book. Add way to add location and view all books scanned.
//TODO: Make sure back button doesn't take to previous screens on book location editing screen.
public class MainActivity extends AppCompatActivity{
    ArrayList<BookInformation> books;
    private final int CONTINUE_SCANNING = 1;
    private final int BACK_TO_MAIN_MENU = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpToolbar();
        setUpMenuButtons();
        books = new ArrayList<>();
    }

    private void setUpToolbar(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setUpMenuButtons(){
        setUpScanButton();
        setUpViewButton();
    }

    private void setUpScanButton(){
        Button scanBtn = (Button)findViewById(R.id.scan_button);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.scan_button) {
                    startScan(MainActivity.this);
                }
            }
        });
    }

    private void setUpViewButton(){
        Button viewBtn = (Button)findViewById(R.id.view_books_button);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.view_books_button) {
                    Intent bookViewerIntent = new Intent(MainActivity.this, BookViewerActivity.class);
                    startActivity(bookViewerIntent);
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(scanningResult != null){
            processScanResult(resultCode, scanningResult);
        }
        else if (requestCode == CONTINUE_SCANNING){
            startScan(MainActivity.this);
        }
        else if (requestCode == BACK_TO_MAIN_MENU){
            books.clear();
        }
        else{
            Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void processScanResult(int resultCode, IntentResult scanningResult){
        if(resultCode == RESULT_OK) {
            String isbn = scanningResult.getContents();
            new GetBookByISBN().execute(isbn);
        }
        else if(resultCode == RESULT_CANCELED && !books.isEmpty()){
            Intent bookViewerIntent = new Intent(MainActivity.this, BookViewerActivity.class);
            bookViewerIntent.putExtra("books", books);
            bookViewerIntent.putExtra("options", ViewMode.EDIT_MODE | ViewMode.LIST_MODE);
            startActivityForResult(bookViewerIntent, BACK_TO_MAIN_MENU);
        }
    }

    private void startScan(Activity launchActivity){
        IntentIntegrator scanIntegrator = new IntentIntegrator(launchActivity);
        scanIntegrator.initiateScan();
    }

    private class GetBookByISBN extends AsyncTask<String,Void,BookInformation>{
        protected BookInformation doInBackground(String... isbns) {
            InputStream inStream;
            BookInformation book = null;
            for(String isbn: isbns) {
                try {
                    String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
                    inStream = getBookSearchResults(url);
                    book = parseJsonStream(inStream, isbn);
                } catch (Exception e) {
                    if (e.getMessage() != null) {
                        Log.e("EXCEPTION", e.getMessage());
                    }
                }
            }
            return book;
        }

        @Override
        protected void onPostExecute(BookInformation book) {
            if(book.title.trim().isEmpty()){
                ArrayList<BookInformation> singleBook = new ArrayList<>();
                singleBook.add(book);
                Intent bookViewerIntent = new Intent(MainActivity.this, BookViewerActivity.class);
                bookViewerIntent.putExtra("options", ViewMode.EDIT_MODE);
                bookViewerIntent.putExtra("books", singleBook);
                startActivityForResult(bookViewerIntent, CONTINUE_SCANNING);
            }
            else{
                books.add(book);
                onActivityResult(CONTINUE_SCANNING, RESULT_OK, null);
            }
        }

        private BookInformation parseJsonStream(InputStream inStream, String isbn) throws Exception {
            JsonReader reader = new JsonReader(new InputStreamReader(inStream, "UTF-8"));
            try {
                return BookJsonParser.processSearchResult(reader, isbn);
            }
            finally{
                reader.close();
            }
        }

        private InputStream getBookSearchResults(String inUrl) throws Exception{
            HttpURLConnection conn = setUpHttpConnection(inUrl);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("RESPONSE CODE", "The response is: " + response);
            return conn.getInputStream();
        }

        private HttpURLConnection setUpHttpConnection(String url) throws Exception {
            URL outUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) outUrl.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            return conn;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.save_button){
            Log.e("SAVE PRESSED", "Pressed save button");
        }
        if(id == R.id.delete_button){
            Log.e("DELETE PRESSED", "Pressed delete button");
        }

        return super.onOptionsItemSelected(item);
    }
}
