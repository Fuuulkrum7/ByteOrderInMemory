package com.example.course_work.data_mapping;

import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

public class CharMapping extends DataTypeMapping{
    AdapterView.OnItemSelectedListener listener_x = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    AdapterView.OnItemSelectedListener listener_y = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    public StringBuilder getAsMemoryDump() {
        return null;
    }

    @Override
    public InputFilter[] getInputFilter() {
        return new InputFilter[0];
    }

    @Override
    public int getInputType() {
        return 0;
    }

    @Override
    public TextWatcher getTextWatcher() {
        return null;
    }

    @Override
    public void setBoolean(boolean[][] old_memory) {

    }

    @Override
    public AdapterView.OnItemSelectedListener getAddressXListener() {
        return listener_x;
    }

    @Override
    public AdapterView.OnItemSelectedListener getAddressYListener() {
        return listener_y;
    }
}
