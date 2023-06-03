package com.release.course_work.data_mapping;

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.release.course_work.MainActivity;

import java.util.Arrays;

public class CharMapping extends DataTypeMapping{
    public final static int ALLOWED_BYTE = 256;
    public final static int HIGH_BYTE = 70000;
    char[][] real_memory;
    boolean class_just_init = true;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch cyrillic;
    Spinner address_x;

    AdapterView.OnItemSelectedListener listener_x = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            char val = parent.getItemAtPosition(position).toString().charAt(1);
            x = Integer.parseInt(String.valueOf(val), 16) / (16 / width);

            if (class_just_init)
                return;

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

            if (class_just_init)
                return;

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
                       Button big_endian, Spinner address_x,
                       @SuppressLint("UseSwitchCompatOrMaterialCode") Switch cyrillic,
                       int x, int y, int width) {
        this(memory_dump, memory_text, input_field, big_endian, address_x, cyrillic);

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
                       Button big_endian, Spinner address_x,
                       @SuppressLint("UseSwitchCompatOrMaterialCode") Switch cyrillic) {
        super(memory_dump, memory_text, input_field, big_endian,
                cyrillic.isChecked() ? HIGH_BYTE : ALLOWED_BYTE);
        this.cyrillic = cyrillic;
        this.address_x = address_x;

        width = cyrillic.isChecked() ? 8 : 16;
        this.real_memory_flags = new boolean[height][width];
        this.real_memory = new char[height][width];

        for (char[] chars : real_memory) Arrays.fill(chars, '\0');

        cyrillic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                width = isChecked ? 8 : 16;

                if (isChecked)
                    target = HIGH_BYTE;
                else
                    target = ALLOWED_BYTE;

                updateAllowed();

                boolean[][] prev_flags = real_memory_flags.clone();

                real_memory = new char[height][width];
                real_memory_flags = new boolean[height][width];

                setBoolean(prev_flags);
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
                class_just_init = false;
                String str = input_field.getText().toString();
                Log.d(MainActivity.TAG, "str change: " + str + " " + prev);

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

    private String fromArrayToString(int i, int j) {
        StringBuilder str = new StringBuilder();

        for (int k = 0; real_memory[i + (j + k) / width][(j + k) % width] != '\0'; ++k) {
            if (real_memory_flags[i + (j + k) / width][(j + k) % width])
                str.append(real_memory[i + (j + k) / width][(j + k) % width]);
        }

        return str.toString();
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
            for (int k = 0; k < memory_dump.length; ++k) {
                byte[] line = memory_dump[k];
                for (int i = 0; i < line.length; ++i) {
                    String str = Integer.toHexString(line[i] + 128).toUpperCase();
                    // Если один символ, до добавляем 0, чтобыв не сместилась строка
                    if (str.length() == 1)
                        str = '0' + str;

                    if (real_memory_flags[k][i])
                        dump.append(str);
                    else
                        dump.append("XX");

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
        if (old_memory == null)
            return;

        for (int i = 0; i < height; ++i) {
            int char_len = 16 / width;

            if (width > old_memory[0].length) {
                for (int sub_index = 0; sub_index < old_memory[0].length; ++sub_index) {
                    int delta = width / old_memory[0].length;
                    for (int j = 0; j < delta; ++j) {
                        int sub_width = sub_index * delta;
                        int real_j = j + sub_width;
                        int idx = (MainActivity.big_endian_flag ? delta - 1 - j : j) + sub_width;

                        char ch = 0;
                        for (int k = real_j * char_len; k < (real_j + 1) * char_len && old_memory[i][sub_index]; ++k) {
                            int t = MainActivity.big_endian_flag ? real_j * char_len * 2 - 1 + char_len - k : k;
                            ch <<= 8;
                            ch += memory_dump[i][t] + 128;
                        }

                        real_memory_flags[i][idx] = old_memory[i][sub_index];
                        real_memory[i][idx] = ch;
                    }
                }
            }
            else {
                int delta = old_memory[0].length / width;
                for (int sub_index = 0; sub_index < width; ++sub_index) {
                    char ch = 0;

                    for (int j = sub_index * delta; j < (sub_index + 1) * delta && old_memory[i][j]; ++j) {
                        int idx = MainActivity.big_endian_flag ?  j : 2 * sub_index * delta - 1 - j + delta;
                        ch <<= 8;
                        ch += memory_dump[i][idx] + 128;
                        real_memory_flags[i][sub_index] |= old_memory[i][idx];
                    }
                    real_memory[i][sub_index] = ch;
                }
            }
        }

        for (char[] line : real_memory)
            Log.d(MainActivity.TAG, Arrays.toString(line));

        if (width < old_memory[0].length) {
            int delta = old_memory[0].length / width;
            x = x / delta + x % delta;
        }
        else if (old_memory[0].length == 8)
            x *= 2;

        // Защита от ввода чего-либо в самую последнюю ячейку
        if (real_memory[height - 1][width - 1] != '\0') {
            real_memory[height - 1][width - 1] = '\0';
            if (real_memory[height - 1][width - 2] == '\0')
                real_memory_flags[height - 1][width - 1] = false;
        }

        just_cleared = true;

        if (real_memory_flags[y][x])
            input_field.setText(fromArrayToString(y, x));

        fullUpdate();
        updateAddress();
    }

    @Override
    public AdapterView.OnItemSelectedListener getAddressXListener() {
        return listener_x;
    }

    @Override
    public AdapterView.OnItemSelectedListener getAddressYListener() {
        return listener_y;
    }

    private void updateAddress() {
        String[] values = new String[16 / getWidth()];

        for (int i = 0, value = 0; i < values.length; value += getWidth(), ++i)
            values[i] = "*" + Integer.toHexString(value).toUpperCase();

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (MainActivity.getContext(), android.R.layout.simple_spinner_item,
                        values);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        address_x.setAdapter(spinnerArrayAdapter);


        address_x.setSelection(getX());
    }
}
