package com.example.course_work.data_mapping;

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.course_work.MainActivity;

import java.util.Arrays;

public class CharMapping extends DataTypeMapping{
    char real_memory[][];
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch cyrillic;

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

    public CharMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                       @SuppressLint("UseSwitchCompatOrMaterialCode") Switch big_endian,
                       @SuppressLint("UseSwitchCompatOrMaterialCode") Switch cyrillic,
                       int x, int y, int width) {
        this(memory_dump, memory_text, input_field, big_endian, cyrillic);

        if (width != 0) {
            if (width > getWidth())
                x = x * (width / getWidth());
            else
                x = x / (getWidth() / width);
        }

        this.x = x;
        this.y = y;
    }

    private boolean parseStringToArr(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (y + (x + i) / width >= 10)
                return false;
            real_memory[y + (x + i) / width][y + (x + i) % width] = s.charAt(i);
            real_memory_flags[y + (x + i) / width][y + (x + i) % width] = true;
        }

        return true;
    }

    public CharMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                       Button big_endian,
                       @SuppressLint("UseSwitchCompatOrMaterialCode") Switch cyrillic) {
        super(memory_dump, memory_text, input_field, big_endian);
        this.cyrillic = cyrillic;

        width = cyrillic.isChecked() ? 8 : 16;
        this.real_memory_flags = new boolean[height][width];
        this.real_memory = new char[height][width];

        for (char[] line : real_memory)
            Arrays.fill(line, '\0');

        cyrillic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                width = isChecked ? 8 : 16;
                real_memory_flags = new boolean[height][width];
                char[][] prev = real_memory.clone();
                real_memory = new char[height][width];

                int n = min(width, prev[0].length);
                for (int i = 0; i < height * n; ++i)
                    real_memory[i / width][i % width] = prev[i / prev[0].length][i % prev[0].length];
                fullUpdate();
            }
        });

        watcher = new TextWatcher() {
            String prev;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                prev = input_field.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = input_field.getText().toString();
                if (str.length() > 0 && (('0' <= str.charAt(0) && str.charAt(0) <= '9') || str.length() > 1)) {
                    real_memory_flags[y][x] = true;

                    if (!parseStringToArr(str)) {
                        parseStringToArr(prev);
                        input_field.setText(prev);
                        Toast.makeText(MainActivity.getContext(), "Text is too long",
                                Toast.LENGTH_SHORT).show();
                    }
                    updateByteArray(y, x);
                }
                else if (s.length() == 0 && !just_cleared) {
                    real_memory_flags[y][x] = false;
                    int size = 16 / real_memory[0].length;

                    for (int i = x * size; i < (x + 1) * size; ++i) {
                        memory_dump[y][i] = '\0';
                    }
                }

                if (just_cleared)
                    just_cleared = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                memory_text.setText(getAsMemoryDump());
            }
        };
    }

    private void updateByteArray(int i, int j) {
        // Получаем число байтов, которые будут задействованы при записи
        int size = 16 / width;

        int bits = Float.floatToIntBits(real_memory[i][j]);
        String digit = String.format("%8s", Integer.toHexString(bits)).replace(" ", "0");

        // Перебираем массив байтов
        for (int k = j * size, n = digit.length() - 1; k < (j + 1) * size && real_memory_flags[i][j]; ++k, n -= 2) {
            byte dgt = Byte.parseByte(String.valueOf(digit.charAt(n - 1)), 16);
            dgt <<= 4;
            dgt += Byte.parseByte(String.valueOf(digit.charAt(n)), 16);

            dgt -= 128;

            memory_dump[i][k] = dgt;
        }
    }

    private void fullUpdate() {
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                updateByteArray(i, j);
            }
        }
        memory_text.setText(getAsMemoryDump());
    }

    @Override
    public StringBuilder getAsMemoryDump() {
        return null;
    }

    @Override
    public InputFilter[] getInputFilter() {
        return new InputFilter[] {new InputFilter.LengthFilter(16)};
    }

    @Override
    public int getInputType() {
        return InputType.TYPE_CLASS_TEXT;
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
