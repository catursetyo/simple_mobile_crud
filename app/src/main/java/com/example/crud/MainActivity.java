package com.example.crud;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etNrp;
    private EditText etNama;
    private Button btnSimpan;
    private Button btnCari;
    private Button btnUpdate;
    private Button btnHapus;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase dbku;

    private static final String PREF_NAME = "PrefApp";
    private static final String KEY_LAST_NRP = "last_nrp";
    private static final String TABLE_NAME = "mhs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        dbHelper = new DatabaseHelper(this);
        dbku = dbHelper.getWritableDatabase();

        muatRiwayatNrp();

        btnSimpan.setOnClickListener(this);
        btnCari.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);
        btnHapus.setOnClickListener(this);
    }

    private void initViews() {
        etNrp = findViewById(R.id.nrp);
        etNama = findViewById(R.id.nama);
        btnSimpan = findViewById(R.id.btnSimpan);
        btnCari = findViewById(R.id.btnCari);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnHapus = findViewById(R.id.btnHapus);
    }

    private void muatRiwayatNrp() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastNrp = sp.getString(KEY_LAST_NRP, "");
        if (!TextUtils.isEmpty(lastNrp)) {
            etNrp.setText(formatNrp(lastNrp));
        }
    }

    private void simpanNrpKePreference(String nrp) {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_LAST_NRP, nrp);
        editor.apply();
    }

    private String formatNrp(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.length() > 3) {
            trimmed = trimmed.substring(trimmed.length() - 3);
        }
        try {
            int val = Integer.parseInt(trimmed);
            return String.format("%03d", val);
        } catch (NumberFormatException e) {
            return trimmed;
        }
    }

    @Override
    public void onClick(View v) {
        pastikanDatabaseTerbuka();

        int id = v.getId();
        if (id == R.id.btnSimpan) {
            operasiSimpan();
        } else if (id == R.id.btnCari) {
            operasiCari();
        } else if (id == R.id.btnUpdate) {
            operasiUpdate();
        } else if (id == R.id.btnHapus) {
            operasiHapus();
        }
    }

    private void operasiSimpan() {
        String rawNrp = etNrp.getText().toString();
        String namaInput = etNama.getText().toString().trim();

        if (TextUtils.isEmpty(rawNrp.trim())) {
            etNrp.setError("NRP tidak boleh kosong!");
            etNrp.requestFocus();
            return;
        }

        String nrpInput = formatNrp(rawNrp);
        etNrp.setText(nrpInput);

        if (TextUtils.isEmpty(namaInput)) {
            etNama.setError("Nama wajib diisi untuk simpan!");
            etNama.requestFocus();
            return;
        }

        ContentValues data = new ContentValues();
        data.put("nrp", nrpInput);
        data.put("nama", namaInput);

        long result = dbku.insert(TABLE_NAME, null, data);

        if (result != -1) {
            Toast.makeText(this, "Data Tersimpan", Toast.LENGTH_SHORT).show();
            bersihkanForm();
        } else {
            Toast.makeText(this, "Gagal Menyimpan (NRP sudah ada)", Toast.LENGTH_SHORT).show();
        }
    }

    private void operasiCari() {
        String rawNrp = etNrp.getText().toString();

        if (TextUtils.isEmpty(rawNrp.trim())) {
            etNrp.setError("Masukkan NRP untuk mencari!");
            etNrp.requestFocus();
            return;
        }

        etNama.setError(null);

        String nrpInput = formatNrp(rawNrp);
        etNrp.setText(nrpInput);

        simpanNrpKePreference(nrpInput);

        String query = "SELECT nrp, nama FROM " + TABLE_NAME + " WHERE nrp = ?";
        Cursor cur = null;

        try {
            cur = dbku.rawQuery(query, new String[]{nrpInput});

            if (cur != null && cur.getCount() > 0 && cur.moveToFirst()) {
                int colNamaIndex = cur.getColumnIndex("nama");
                String namaResult = cur.getString(colNamaIndex);

                etNama.setText(namaResult);
                Toast.makeText(this, "Data Ditemukan", Toast.LENGTH_SHORT).show();
            } else {
                etNama.setText("");
                Toast.makeText(this, "Data Tidak Ditemukan", Toast.LENGTH_SHORT).show();
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    private void operasiUpdate() {
        String rawNrp = etNrp.getText().toString();
        String namaInput = etNama.getText().toString().trim();

        if (TextUtils.isEmpty(rawNrp.trim())) {
            etNrp.setError("NRP tidak boleh kosong!");
            etNrp.requestFocus();
            return;
        }

        String nrpInput = formatNrp(rawNrp);
        etNrp.setText(nrpInput);

        if (TextUtils.isEmpty(namaInput)) {
            etNama.setError("Nama wajib diisi untuk update!");
            etNama.requestFocus();
            return;
        }

        ContentValues data = new ContentValues();
        data.put("nama", namaInput);

        int rowsAffected = dbku.update(TABLE_NAME, data, "nrp = ?", new String[]{nrpInput});

        if (rowsAffected > 0) {
            Toast.makeText(this, "Data Terupdate", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Data Tidak Ditemukan untuk Diupdate", Toast.LENGTH_SHORT).show();
        }
    }

    private void operasiHapus() {
        String rawNrp = etNrp.getText().toString();

        if (TextUtils.isEmpty(rawNrp.trim())) {
            etNrp.setError("Masukkan NRP yang akan dihapus!");
            etNrp.requestFocus();
            return;
        }

        etNama.setError(null);

        String nrpInput = formatNrp(rawNrp);
        etNrp.setText(nrpInput);

        int rowsDeleted = dbku.delete(TABLE_NAME, "nrp = ?", new String[]{nrpInput});

        if (rowsDeleted > 0) {
            bersihkanForm();
            Toast.makeText(this, "Data Terhapus", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Data Tidak Ditemukan untuk Dihapus", Toast.LENGTH_SHORT).show();
        }
    }

    private void bersihkanForm() {
        etNrp.setText("");
        etNama.setText("");
        etNrp.setError(null);
        etNama.setError(null);
        etNrp.clearFocus();
        etNama.clearFocus();
    }

    private void pastikanDatabaseTerbuka() {
        if (dbku == null || !dbku.isOpen()) {
            if (dbHelper == null) {
                dbHelper = new DatabaseHelper(this);
            }
            dbku = dbHelper.getWritableDatabase();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        pastikanDatabaseTerbuka();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dbku != null && dbku.isOpen()) {
            dbku.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "db_mahasiswa";
        private static final int DATABASE_VERSION = 2;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_NAME + " ("
                    + "nrp TEXT PRIMARY KEY, "
                    + "nama TEXT"
                    + ");";
            db.execSQL(createTable);
            seedDummyData(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            checkAndSeedData(db);
        }

        private static void seedDummyData(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                for (int i = 1; i <= 100; i++) {
                    String nrp = String.format("%03d", i);
                    String nama = "Mahasiswa " + nrp;
                    ContentValues cv = new ContentValues();
                    cv.put("nrp", nrp);
                    cv.put("nama", nama);
                    db.insert(TABLE_NAME, null, cv);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        private static void checkAndSeedData(SQLiteDatabase db) {
            Cursor c = null;
            try {
                c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
                if (c != null && c.moveToFirst()) {
                    int count = c.getInt(0);
                    if (count == 0) {
                        seedDummyData(db);
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }
}
