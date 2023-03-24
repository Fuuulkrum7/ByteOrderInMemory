package com.example.course_work.data_mapping;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.course_work.MainActivity;
import com.example.course_work.StandardInputField;

public class IntMapping extends DataTypeMapping{

    int[][] real_memory;

    AdapterView.OnItemSelectedListener listener_x = new AdapterView.OnItemSelectedListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            x = Integer.parseInt(parent.getItemAtPosition(position).toString()) / (16 / width);

            if (real_memory_flags[y][x]) {
                input_field.setText(Integer.toString(real_memory[y][x]));
            }
            else {
                just_cleared = true;
                input_field.setText("");
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    AdapterView.OnItemSelectedListener listener_y = new AdapterView.OnItemSelectedListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            y = position;

            if (real_memory_flags[y][x]) {
                input_field.setText(Integer.toString(real_memory[y][x]));
            }
            else {
                just_cleared = true;
                input_field.setText("");
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };
    public IntMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                       @SuppressLint("UseSwitchCompatOrMaterialCode") Switch big_endian,
                      int x, int y, int width) {
        this(memory_dump, memory_text, input_field, big_endian);

        if (width != 0) {
            if (width > getWidth())
                x = x * (width / getWidth());
            else
                x = x / (getWidth() / width);
        }

        this.x = x;
        this.y = y;
    }
    public IntMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                      @SuppressLint("UseSwitchCompatOrMaterialCode") Switch big_endian) {

        super();

        width = 4;
        this.real_memory_flags = new boolean[height][width];
        this.real_memory = new int[height][width];

        this.memory_dump = memory_dump;
        this.memory_text = memory_text;
        this.big_endian = big_endian;
        this.input_field = input_field;

        big_endian.setOnCheckedChangeListener((buttonView, isChecked) -> memory_text.setText(getAsMemoryDump()));

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

                    try {
                        real_memory[y][x] = Integer.parseInt(str);
                    }
                    catch (NumberFormatException e) {
                        real_memory[y][x] = Integer.parseInt(prev);
                        input_field.setText(prev);
                        Toast.makeText(MainActivity.getContext(), "Number is to big! " +
                                "Try type long instead", Toast.LENGTH_SHORT).show();
                    }
                    updateByteArray();
                }
                else if (s.length() == 0 && !just_cleared) {
                    real_memory_flags[y][x] = false;
                    int size = 16 / real_memory[0].length;

                    for (int i = x * size; i < (x + 1) * size; ++i) {
                        memory_dump[y][i] = 127;
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

    private void updateByteArray() {
        long val = real_memory[y][x];
        int size = 16 / width;

        for (int k = x * size; k < (x + 1) * size && real_memory_flags[y][x]; ++k) {
            if (abs(val) > 0) {
                memory_dump[y][k] = (byte) (val % 256 - 128);
                val >>= 8;
            }
            else if (real_memory[y][x] < 0)
                memory_dump[y][k] = 127;
            else
                memory_dump[y][k] = -128;
        }
    }

    @Override
    public StringBuilder getAsMemoryDump() {
        StringBuilder dump = new StringBuilder();

        int delta = memory_dump[0].length / real_memory[0].length;
        boolean bn = big_endian.isChecked();

        for (byte[] line: memory_dump) {
            for (int i = 0; i < real_memory[0].length; ++i) {
                for (int j = i * delta; j < (i + 1) * delta; ++j) {
                    int value = line[bn ? delta * (2 * i + 1) - j - 1: j] + 128;
                    String str = Integer.toHexString(value).toUpperCase();
                    if (str.length() == 1)
                        str = '0' + str;
                    dump.append(str);

                    if (j < (i + 1) * delta - 1)
                        dump.append(' ');
                }
                if ((i + i) * delta <= line.length)
                    dump.append('|');
            }
            dump.append('\n');
        }

        return dump;
    }

    @Override
    public void setBoolean(boolean[][] old_memory) {
        if (old_memory == null)
            return;

        if (old_memory[0].length > real_memory_flags[0].length) {
            // Длина требуемой строки
            int full_len = 16 / real_memory[0].length;
            // Итерируемся по каждой строке памяти
            for (int line = 0; line < real_memory.length; ++line) {

                // теперь перебираем каждый столбец в памяти (зависит их число от типа данных,
                // это 16 делить на число байтов, отводимых под тип данных)
                for (int i = 0; i < real_memory[0].length; ++i) {
                    // Коэфициент, одна из степеней 256 (при считывании каждого байта умножаем на 2)
                    int coef = 1;
                    // Считанные их памяити данные
                    int data = 0;

                    // С помощью или считываем, были ли какие-то данные в одной из ячеек, отводимых под число ранее
                    // То есть, у нас был инт, у него 4 ячйки по 4 байта
                    // Читаем их в 2 ячейки у лонг, каждые две читаем в одну
                    boolean flag = false;
                    // Отношение длин старой и новой ячеек (4 / 2, к примеру)
                    int sub_len = old_memory[0].length / real_memory_flags[0].length;

                    // Перебираем все старые ячейки
                    for (int bool_idx = i * sub_len; bool_idx < (i + 1) * sub_len && old_memory[line][bool_idx]; ++bool_idx) {
                        // перебираем их же, но уже побайтово
                        for (int sub_i = bool_idx * (full_len / sub_len); sub_i < (bool_idx + 1) * (full_len / sub_len); ++sub_i) {
                            // Считываем данные в число
                            data += (memory_dump[line][sub_i] + 128) * coef;
                            // Увеличиваем коэфициент (двигаем в следующий разряд)
                            coef *= 256;
                        }
                        // Через или получаем инфу о том, есть ли что в ячейках (через проверку data
                        // не вариант, там мог быть в памяти 0 записан)
                        flag = flag || old_memory[line][bool_idx];
                    }

                    // Записываем считанные данные
                    real_memory[line][i] = data;
                    real_memory_flags[line][i] = flag;
                }
            }
        }
        else {
            // Длина требуемой строки
            int full_len = 16 / real_memory[0].length;
            // Итерируемся по каждой строке памяти
            for (int line = 0; line < real_memory.length; ++line) {

                // теперь перебираем каждый столбец в памяти
                for (int i = 0; i < old_memory[0].length; ++i) {
                    // Отношение длин старой и новой ячеек (4 / 2, к примеру)
                    int sub_len = real_memory_flags[0].length / old_memory[0].length;

                    // Перебираем все старые ячейки
                    for (int bool_idx = i * sub_len; bool_idx < (i + 1) * sub_len && old_memory[line][i]; ++bool_idx) {
                        // Коэфициент, одна из степеней 256 (при считывании каждого байта умножаем на 2)
                        int coef = 1;
                        // Считанные их памяити данные
                        int data = 0;

                        // перебираем их же, но уже побайтово
                        for (int sub_i = bool_idx * full_len; sub_i < (bool_idx + 1) * full_len; ++sub_i) {
                            // Считываем данные в число
                            data += (memory_dump[line][sub_i] + 128) * coef;
                            // Увеличиваем коэфициент (двигаем в следующий разряд)
                            coef <<= 8;
                        }

                        // Записываем считанные данные
                        real_memory[line][bool_idx] = data;
                        real_memory_flags[line][bool_idx] = old_memory[line][i];
                    }
                }
            }
        }

        updateByteArray();
        memory_text.setText(getAsMemoryDump());
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
