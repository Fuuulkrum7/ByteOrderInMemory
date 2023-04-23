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
            char val = parent.getItemAtPosition(position).toString().charAt(1);
            int c = Integer.parseInt(String.valueOf(val), 16) / (16 / width);

            if (c == x)
                return;

            x = c;

            just_cleared = true;
            if (real_memory_flags[y][x])
                input_field.setText(fromArrayToString(y, x));
            else
                input_field.setText("");
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    AdapterView.OnItemSelectedListener listener_y = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            y = position;

            just_cleared = true;
            if (real_memory_flags[y][x])
                input_field.setText(fromArrayToString(y, x));
            else
                input_field.setText("");
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

        for (char[] chars : real_memory) Arrays.fill(chars, '\0');

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
                // Log.d(MainActivity.TAG, str + " " + prev);

                int delta = 1;
                if (y + (x + s.length() + 1) / width < 10 && str.length() > prev.length() &&
                        real_memory[y + (x + s.length()) / width][(x + s.length()) % width] != '\0')
                    delta = 0;

                if (!parseStringToArr(str, delta)) {
                    parseStringToArr(prev, delta);
                    just_cleared = true;

                    if (x == 15 && y == 9)
                        real_memory_flags[9][15] = false;

                    input_field.setText(prev);
                    Toast.makeText(MainActivity.getContext(), "Text is too long",
                            Toast.LENGTH_SHORT).show();
                }

                updateByteArray(y, x);

                if (str.length() < prev.length() && !just_cleared) {
                    removeOldBytes(y, x, str.length(), prev.length());
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

    private StringBuilder fromArrayToString(int i, int j) {
        StringBuilder str = new StringBuilder();

        for (int k = 0; real_memory[i + (j + k) / width][(j + k ) % width] != '\0'; ++k) {
            if (real_memory_flags[i + (j + k) / width][(j + k) % width])
                str.append(real_memory[i + (j + k) / width][(j + k) % width]);
        }

        return str;
    }

    private boolean parseStringToArr(String s, int delta) {
        for (int i = 0; i < s.length() + delta; ++i) {
            if (y + (x + i) / width >= 10)
                return false;

            if (i < s.length())
                real_memory[y + (x + i) / width][(x + i) % width] = s.charAt(i);
            else
                real_memory[y + (x + i) / width][(x + i) % width] = '\0';

            if (s.length() > 0)
                real_memory_flags[y + (x + i) / width][(x + i) % width] = true;
        }

        return true;
    }

    private void removeOldBytes(int i, int j, int start, int end) {
        // Получаем число байтов, которые будут задействованы при записи
        int size = 16 / width;

        if (start != 0 || (j > 0 && real_memory_flags[i + (j - 1) / width][(j - 1) % width]))
            start += 1;

        for (int k = start; k < end + 1; ++k) {
            if (real_memory_flags[i + (j + k) / width][(j + k) % width]) {
                real_memory[i + (j + k) / width][(j + k) % width] = '\0';
                real_memory_flags[i + (j + k) / width][(j + k) % width] = false;
                // Перебираем массив байтов
                for (int l = 0; l < size; ++l)
                    memory_dump[i + (j + k) / width][((j + k) % width) * size + l] = 127;
            }
        }
    }

    private void updateByteArray(int i, int j) {
        // Получаем число байтов, которые будут задействованы при записи
        int size = 16 / width;
        int k = 0;

        do {
            if (real_memory_flags[i + (j + k) / width][(j + k) % width]) {
                char chr = real_memory[i + (j + k) / width][(j + k) % width];
                // Перебираем массив байтов
                for (int l = 0; l < size; ++l) {
                    memory_dump[i + (j + k) / width][((j + k) % width) * size + l]
                            = (byte) (chr - 128);
                    chr >>= 8;
                }
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
        fullUpdate();
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
