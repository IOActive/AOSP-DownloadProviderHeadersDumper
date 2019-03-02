package com.ioactive.downloadProviderHeadersDumper;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    // Adjustable priority for the "dump headers" thread (-20 = maximum priority)
    private static final int THREAD_PRIORITY = -20;

    private static final String TAG = "DownProvHeadersDumper";
    private static final String LOG_SEPARATOR = "\n**********************************\n";

    private static final String HEADERS_URI_SEGMENT = "/headers";
    private static final String MY_DOWNLOADS_URI = "content://downloads/my_downloads/";
    //private static final String MY_DOWNLOADS_URI = "content://downloads/download/"; // Works as well

    private TextView mTextViewLog;
    private EditText mEditMinId;
    private EditText mEditMaxId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewLog = findViewById(R.id.textViewLog);
        mTextViewLog.setMovementMethod(new ScrollingMovementMethod());

        mEditMinId = findViewById(R.id.editMinId);
        mEditMaxId = findViewById(R.id.editMaxId);
    }

    private synchronized void log(final String text) {
        Log.d(TAG, text);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextViewLog.append(text + "\n");
            }
        });
    }

    public void buttonDumpHeaders_Click(View view) {
        new Thread(new Runnable() {
            public void run() {
                android.os.Process.setThreadPriority(THREAD_PRIORITY);
                int minId = Integer.parseInt(mEditMinId.getText().toString());
                int maxId = Integer.parseInt(mEditMaxId.getText().toString());

                try {
                    dumpRequestHeaders(minId, maxId);
                } catch (Exception e) {
                    Log.e(TAG, "Error", e);
                    log(e.toString());
                }
            }
        }).start();
    }

    private void dumpRequestHeaders(int minId, int maxId) {
        ContentResolver res = getContentResolver();

        for (int id = minId; id <= maxId; id++) {
            Uri uri = Uri.parse(MY_DOWNLOADS_URI + id + HEADERS_URI_SEGMENT);
            Cursor cur = res.query(uri, null, null, null, null);

            try {
                if (cur != null && cur.getCount() > 0) {
                    StringBuilder sb = new StringBuilder(LOG_SEPARATOR);
                    sb.append("HEADERS FOR DOWNLOAD ID ").append(id).append("\n");
                    while (cur.moveToNext()) {
                        String rowHeader = cur.getString(cur.getColumnIndex("header"));
                        String rowValue = cur.getString(cur.getColumnIndex("value"));
                        sb.append(rowHeader).append(": ").append(rowValue).append("\n\n");
                    }
                    log(sb.toString());
                }
            } finally {
                if (cur != null)
                    cur.close();
            }
        }
        log("\n\nDUMP FINISHED (IDs: " + minId + " to " + maxId + ")");
    }

}
