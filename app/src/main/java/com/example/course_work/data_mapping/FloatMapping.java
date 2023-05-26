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
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.course_work.MainActivity;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class FloatMapping extends DataTypeMapping {
    float[][] real_memory;

    AdapterView.OnItemSelectedListener listener_x = new AdapterView.OnItemSelectedListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            char val = parent.getItemAtPosition(position).toString().charAt(1);
            x = Integer.parseInt(String.valueOf(val), 16) / (16 / width);

            updateTextField();
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

            updateTextField();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    protected void updateTextField() {
        if (real_memory_flags[y][x]) {
            // Защита от "дурака", чтобы не получать NaN или бесконечность, их отобразить не выйдет
            if (Float.isInfinite(real_memory[y][x]) || Float.isNaN(real_memory[y][x])) {
                Toast.makeText(MainActivity.getContext(), "Oh, you've got NaN or infinity!",
                        Toast.LENGTH_SHORT).show();
                input_field.setText("0");
            }
            else {
                // Отображаем нашу дробь. Все отображаем как положено, в виде десятичной дроби
                // Чтобы она не сокращалась с использованием e, пришлось так сделать
                String res = new BigDecimal(real_memory[y][x]).toPlainString();
                int idx = res.indexOf('.');
                input_field.setText(res.substring(0, min(48 + idx, res.length())));
            }
        }
        else {
            // Если в выбранном месте ничего нет в памяти, чистим поле ввода
            just_cleared = true;
            input_field.setText("");
        }
    }

    public FloatMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                        Button big_endian,
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

    public FloatMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                        Button big_endian) {
        super(memory_dump, memory_text, input_field, big_endian);

        width = 4;
        this.real_memory_flags = new boolean[height][width];
        this.real_memory = new float[height][width];

        watcher = new TextWatcher() {
            String prev;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                prev = input_field.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = input_field.getText().toString();
                // Если мы можем спарсить то, что введено
                if (str.length() > 0 && (('0' <= str.charAt(0) && str.charAt(0) <= '9') || str.length() > 1)) {
                    real_memory_flags[y][x] = true;

                    try {
                        // Пробуем это сделать
                        real_memory[y][x] = Float.parseFloat(str);
                    }
                    catch (NumberFormatException e) {
                        // В случае ошибки возвращаемся к тому, что было введено ранее
                        real_memory[y][x] = Float.parseFloat(prev);
                        input_field.setText(prev);
                        Toast.makeText(MainActivity.getContext(), "Number is incorrect",
                                Toast.LENGTH_SHORT).show();
                    }
                    updateByteArray(y, x);
                }
                // Если ввод пустой и это не была очистка текстового поля
                else if (s.length() == 0 && !just_cleared) {
                    // Прекращаем отображать данные в ячейке
                    real_memory_flags[y][x] = false;
                }

                // В случае с очисткой меняем флаг обратно
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
    public void setBoolean(boolean[][] old_memory) {
        if (old_memory == null)
            return;

        int full_len = 16 / width;

        // в зависимости от того, сколько байт требовалось раньше на ячейку и сейчас, делим одно на другое
        // Главное не получить 0
        int coef = old_memory[0].length > real_memory_flags[0].length ?
                old_memory[0].length / real_memory_flags[0].length : real_memory_flags[0].length / old_memory[0].length;

        for (int line = 0; line < real_memory.length; ++line) {
            for (int i = 0; i < full_len; ++i) {
                // парсим память в требуемом порядке
                int bits = 0;
                for (int j = (i + 1) * full_len - 1; j >= i * full_len; --j) {
                    bits <<= 8;
                    bits += memory_dump[line][j] + 128;
                }
                // переводим биты в число
                real_memory[line][i] = Float.intBitsToFloat(bits);

                // И обновляем флаги
                if (old_memory[0].length > real_memory_flags[0].length) {
                    for (int k = i * coef; k < (i + 1) * coef; ++k)
                        real_memory_flags[line][i] |= old_memory[line][k];
                }
                else
                    real_memory_flags[line][i] = old_memory[line][i / coef];
            }
        }

        fullUpdate();
    }

    @Override
    public AdapterView.OnItemSelectedListener getAddressXListener() {
        return listener_x;
    }

    @Override
    public int getInputType() {
        return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
    }

    @Override
    public AdapterView.OnItemSelectedListener getAddressYListener() {
        return listener_y;
    }
}
