package com.example.course_work.data_mapping;

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
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
    char[][] real_memory;
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
                       Button big_endian,
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

    public CharMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                       Button big_endian,
                       @SuppressLint("UseSwitchCompatOrMaterialCode") Switch cyrillic) {
        super(memory_dump, memory_text, input_field, big_endian,
                "1234567890-_=+!@#$%^&*()qwertyuiopasdfghjklzxcvbnmm[];'./,<>?:\"\\|QWERTYUIOPASDFGHJKLZXCVBNM");
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

                char[][] prev = real_memory.clone();
                boolean[][] prev_flags = real_memory_flags.clone();

                real_memory = new char[height][width];
                real_memory_flags = new boolean[height][width];

                int n = min(width, prev[0].length);
                for (int i = 0; i < height * n; ++i) {
                    real_memory[i / width][i % width] = prev[i / prev[0].length][i % prev[0].length];
                    real_memory_flags[i / width][i % width] = prev_flags[i / prev[0].length][i % prev[0].length];
                }
                input_field.setInputType(getInputType());
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

                real_memory_flags[y][x] = str.length() > 0;

                if (!parseStringToArr(str)) {
                    parseStringToArr(prev);
                    input_field.setText(prev);
                    Toast.makeText(MainActivity.getContext(), "Text is too long",
                            Toast.LENGTH_SHORT).show();
                }
                updateByteArray(y, x);

                if (just_cleared)
                    just_cleared = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                memory_text.setText(getAsMemoryDump());
            }
        };
    }

    private boolean parseStringToArr(String s) {
        for (int i = 0; i < s.length() + 1; ++i) {
            if (i < s.length()) {
                if (y + (x + i) / width >= 10)
                    return false;
                real_memory[y + (x + i) / width][y + (x + i) % width] = s.charAt(i);
            }
            else
                real_memory[y + (x + i) / width][y + (x + i) % width] = '\0';

            real_memory_flags[y + (x + i) / width][y + (x + i) % width] = true;
        }

        return true;
    }

    private void updateByteArray(int i, int j) {
        // Получаем число байтов, которые будут задействованы при записи
        int size = 16 / width;
        int k = 0;

        do {
            char chr = real_memory[i + (j + k) / width][(j + k) % width];
            // Перебираем массив байтов
            for (int l = 0; l < size; ++l) {
                memory_dump[i + (j + k) / width][((j + k) % width) * size + l]
                        = (byte) (chr - 128);
                chr >>= 8;
            }
            ++k;
        } while (real_memory[i + (j + k - 1) / width][(j + k - 1) % width] != '\0');
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
        StringBuilder dump = new StringBuilder();
        if (width == 16) {
            for (byte[] line : memory_dump) {
                for (int i = 0; i < line.length; ++i) {
                    String str = Integer.toHexString(line[i] + 128).toUpperCase();
                    // Если один символ, до добавляем 0, чтобыв не сместилась строка
                    if (str.length() == 1)
                        str = '0' + str;
                    dump.append(str);

                    // Если мы не в конце и не надо добавлять разделитель
                    if (i < line.length - 1)
                        dump.append('|');
                }
                dump.append('\n');
            }
        }
        else {
            dump = super.getAsMemoryDump();
        }

        return dump;
    }

    @Override
    public InputFilter[] getInputFilter() {
        if (width == 8)
            return new InputFilter[] {new InputFilter.LengthFilter(40)};
        return super.getInputFilter();
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
