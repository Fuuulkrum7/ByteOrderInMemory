package com.example.course_work.data_mapping;

import android.annotation.SuppressLint;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.example.course_work.MainActivity;
import com.example.course_work.StandardInputField;

public abstract class DataTypeMapping {
    public final static int height = 10;
    int width = 4;
    byte[][] memory_dump;
    TextView memory_text;

    EditText input_field;
    int x = 0, y = 0;
    boolean just_cleared = false;

    TextWatcher watcher;
    Button big_endian;
    boolean[][] real_memory_flags;

    public String allowed = "0123456789.-";
    public static StandardInputField mappingInputFilter;

    public DataTypeMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                           Button big_endian, String allowed) {
        this(memory_dump, memory_text, input_field, big_endian);

        this.allowed = allowed;
        mappingInputFilter = new StandardInputField(allowed);
    }

    public DataTypeMapping(byte[][] memory_dump, TextView memory_text, EditText input_field,
                           Button big_endian) {
        this.memory_dump = memory_dump;
        this.memory_text = memory_text;
        this.big_endian = big_endian;
        this.input_field = input_field;

        mappingInputFilter = new StandardInputField(allowed);

        big_endian.setOnClickListener(v -> {
            MainActivity.big_endian_flag = !MainActivity.big_endian_flag;
            if (MainActivity.big_endian_flag)
                big_endian.setText(MainActivity.BIG_ENDIAN);
            else
                big_endian.setText(MainActivity.LITTLE_ENDIAN);
            memory_text.setText(getAsMemoryDump());
        });
    }

    public StringBuilder getAsMemoryDump() {
        StringBuilder dump = new StringBuilder();

        int delta = 16 / width;
        boolean bn = MainActivity.big_endian_flag;

        // Перебираем все строки в памяти
        for (byte[] line: memory_dump) {
            // перебираем каждую ячейку памяти
            for (int i = 0; i < width; ++i) {
                // Теперь перебираем ячейку побайтово
                for (int j = i * delta; j < (i + 1) * delta; ++j) {
                    // Если используется big-endian, берем байты с конца, иначе с начала
                    // и добавляем 128, так как положительные от -128 до -1 по значениям,
                    // а отрицательные от 0 до 127. При добавлении 128 имеем диапазон от 0 до 255
                    // и за счет этого мы сразу переводим число в доп. код
                    int value = line[bn ? delta * (2 * i + 1) - j - 1: j] + 128;
                    // В 16-ричное число
                    String str = Integer.toHexString(value).toUpperCase();
                    // Если один символ, до добавляем 0, чтобыв не сместилась строка
                    if (str.length() == 1)
                        str = '0' + str;
                    dump.append(str);

                    // Если мы не в конце и не надо добавлять разделитель
                    if (j < (i + 1) * delta - 1)
                        dump.append(' ');
                }
                // Если мы на стыке между значениями, ставим разделитель
                if ((i + 1) * delta < line.length)
                    dump.append('|');
            }
            // Новая строка
            dump.append('\n');
        }

        return dump;
    }

    public InputFilter[] getInputFilter() {
        return new InputFilter[] {mappingInputFilter.getFilter()};
    }

    void updateAllowed() {
        mappingInputFilter.allowed = allowed;
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
