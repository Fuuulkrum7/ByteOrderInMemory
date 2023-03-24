package com.example.course_work;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.example.course_work.data_mapping.DataTypeMapping;
import com.example.course_work.data_mapping.IntMapping;
import com.example.course_work.data_mapping.LongMapping;

import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "CourseLog";
    public static final String APP_PREFERENCES = "course_work";
    public static final String APP_PREFERENCES_CYRILLIC = "cyrillic";
    public static final String APP_PREFERENCES_DATATYPE = "datatype_pos";
    public static final String APP_PREFERENCES_BIG_ENDIAN = "big_endian";
    LinearLayout input_field;
    EditText text_field;
    TextView memory_dump;
    TextView address;
    Spinner datatype, address_y, address_x;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch cyrillic, big_endian;
    byte[][] memory = new byte[11][16];
    int address_number = Integer.parseInt("100000", 16);
    DataTypeMapping dataTypeMapping = null;


    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        input_field = findViewById(R.id.input_field);
        text_field = findViewById(R.id.text_field);
        datatype = findViewById(R.id.datatype);
        cyrillic = findViewById(R.id.cyrillic);
        big_endian = findViewById(R.id.big_endian);

        address = findViewById(R.id.address);
        address_x = findViewById(R.id.address_x);
        address_y = findViewById(R.id.address_y);
        memory_dump = findViewById(R.id.memory_dump);

        StringBuilder dump = new StringBuilder();
        String[] addresses = new String[11];

        address_number += (new Random()).nextInt(Integer.parseInt("800000", 16));
        address_number -= address_number % 16;

        for (int j = 0; j < 11; ++j) {
            byte[] line = memory[j];
            Arrays.fill(line, (byte) 127);
            for (int i = 0; i < line.length; ++i) {
                dump.append(Integer.toHexString(line[i] + 128).toUpperCase());
                dump.append((i + 1) % 4 == 0 && i < line.length - 1 ? '|' : ' ');
            }
            dump.append('\n');

            StringBuilder adr = new StringBuilder((Integer.toHexString(address_number)).toUpperCase());
            adr.setCharAt(adr.length() - 1, '*');

            addresses[j] = adr.toString();

            address_number += 16;
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        addresses); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        address_y.setAdapter(spinnerArrayAdapter);

        SharedPreferences settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        // Загрузка значний перменных из постоянной памяти
        cyrillic.setChecked(settings.getBoolean(APP_PREFERENCES_CYRILLIC, false));
        big_endian.setChecked(settings.getBoolean(APP_PREFERENCES_BIG_ENDIAN, false));

        int val = settings.getInt(APP_PREFERENCES_DATATYPE, 0);
        datatype.setSelection(val);
        chooseMapping(val);

        address.setText(String.join("\n", addresses));

        datatype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chooseMapping(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        context = this;
    }

    protected void chooseMapping(int position) {
        int x = 0, y = 0, width = 0;
        boolean[][] old_memory = null;
        if (dataTypeMapping != null) {
            text_field.removeTextChangedListener(dataTypeMapping.getTextWatcher());
            x = dataTypeMapping.getX();
            y = dataTypeMapping.getY();
            width = dataTypeMapping.getWidth();
            old_memory = dataTypeMapping.getRealMemory();
        }

        switch (position) {
            case 1:
                dataTypeMapping = new LongMapping(
                        memory, memory_dump, text_field, big_endian, x, y, width
                );
                break;
            case 2:
            case 3:
            case 0:
            default:
                dataTypeMapping = new IntMapping(memory, memory_dump,
                        text_field, big_endian, x, y, width);
                break;
        }

        String[] values = new String[16 / dataTypeMapping.getWidth()];

        for (int i = 0, value = 0; i < values.length; value += dataTypeMapping.getWidth(), ++i)
            values[i] = Integer.toString(value);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        values);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        address_x.setAdapter(spinnerArrayAdapter);

        address_x.setOnItemSelectedListener(dataTypeMapping.getAddressXListener());
        address_y.setOnItemSelectedListener(dataTypeMapping.getAddressYListener());

        address_x.setSelection(dataTypeMapping.getX());

        text_field.setInputType(dataTypeMapping.getInputType());
        text_field.setFilters(dataTypeMapping.getInputFilter());
        text_field.addTextChangedListener(dataTypeMapping.getTextWatcher());

        dataTypeMapping.setBoolean(old_memory);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, datatype.getSelectedItemPosition() + "");
        SharedPreferences settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(APP_PREFERENCES_CYRILLIC, cyrillic.isChecked());
        editor.putBoolean(APP_PREFERENCES_BIG_ENDIAN, big_endian.isChecked());
        editor.putInt(APP_PREFERENCES_DATATYPE, datatype.getSelectedItemPosition());

        editor.apply();
    }

    public static Context getContext() {
        return context;
    }
}