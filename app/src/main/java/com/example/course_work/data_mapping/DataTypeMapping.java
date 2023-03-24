package com.example.course_work.data_mapping;

import android.annotation.SuppressLint;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.example.course_work.StandardInputField;

public abstract class DataTypeMapping {
    int height = 11;
    int width = 4;
    byte[][] memory_dump;
    TextView memory_text;

    EditText input_field;
    int x = 0, y = 0;
    boolean just_cleared = false;

    TextWatcher watcher;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch big_endian;
    boolean[][] real_memory_flags;

    public static final String allowed = "0123456789-";
    public static final StandardInputField mappingInputFilter = new StandardInputField(allowed);

    public StringBuilder getAsMemoryDump() {
        return null;
    }

    public InputFilter[] getInputFilter() {
        return new InputFilter[] {mappingInputFilter.getFilter()};
    }

    public int getInputType() {
        return InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER;
    }

    public TextWatcher getTextWatcher() {
        return watcher;
    }

    public void setBoolean(boolean[][] old_memory) {

    }

    public int getWidth() {
        return 16 / width;
    }

    public AdapterView.OnItemSelectedListener getAddressXListener() {
        return null;
    }

    public AdapterView.OnItemSelectedListener getAddressYListener() {
        return null;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean[][] getRealMemory() {
        return real_memory_flags;
    }
}
