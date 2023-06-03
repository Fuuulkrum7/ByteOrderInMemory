package com.release.course_work;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.release.course_work.data_mapping.CharMapping;
import com.release.course_work.data_mapping.DataTypeMapping;
import com.release.course_work.data_mapping.FloatMapping;
import com.release.course_work.data_mapping.IntMapping;
import com.release.course_work.data_mapping.LongMapping;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    // Флаги и имена, требуемые для сохранения данных в память
    public static final String TAG = "CourseLog";
    public static final String APP_PREFERENCES = "course_work";
    public static final String APP_PREFERENCES_CYRILLIC = "cyrillic";
    public static final String APP_PREFERENCES_DATATYPE = "datatype_pos";
    public static final String APP_PREFERENCES_BIG_ENDIAN = "big_endian";
    public static final String BIG_ENDIAN = "Big";
    public static final String LITTLE_ENDIAN = "Little";
    // Поле ввода
    EditText input_field;
    // Текстовое поле для вывода дампа памяти
    TextView memory_dump;
    // Тут мы будем отображать данные как значение по оси у
    TextView address;
    // Выпадающие списки для выбора адреса памяти
    Spinner datatype, address_y, address_x;
    // Переключатель между 1 byte и 2 byte char
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch cyrillic;
    // Кнопка выбора порядка байтов
    Button big_endian;
    // Высота массива дампа памяти
    int height = DataTypeMapping.height;
    // Сам дамп в виде массива байтов
    byte[][] memory = new byte[height][16];
    // начальный номер адресов памяти
    int address_number = Integer.parseInt("100000", 16);
    // Заготовка под классы, с помощью который обрабатываются данные
    DataTypeMapping dataTypeMapping = null;
    // Выбран ли порядок байтов big endian
    public static boolean big_endian_flag = false;
    // Контекст для получения его прочими классами, наследниками DataTypeMapping.
    // В первую очередь нужен для создания уведомлений
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Сохраняем все по переменным из xml
        input_field = findViewById(R.id.text_field);
        datatype = findViewById(R.id.datatype);
        cyrillic = findViewById(R.id.cyrillic);

        big_endian = findViewById(R.id.endian);

        address = findViewById(R.id.address);
        address_x = findViewById(R.id.address_x);
        address_y = findViewById(R.id.address_y);
        memory_dump = findViewById(R.id.memory_dump);

        // Формируем выпадающий список для выбора адреса по оси у
        String[] addresses = new String[height];

        // Адрес случайное число,если что
        address_number += (new Random()).nextInt(Integer.parseInt("800000", 16));
        address_number -= address_number % 16;

        for (int j = 0; j < height; ++j) {
            // Добавляем числа 16-ричные в память
            StringBuilder adr = new StringBuilder((Integer.toHexString(address_number)).toUpperCase());
            adr.setCharAt(adr.length() - 1, '*');

            addresses[j] = adr.toString();

            address_number += 16;
        }

        // И добавляем наши адреса в выпадающий список
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        addresses);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        address_y.setAdapter(spinnerArrayAdapter);

        // Открываем сохраненные настройки
        SharedPreferences settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        // Загрузка значний перменных из постоянной памяти
        cyrillic.setChecked(settings.getBoolean(APP_PREFERENCES_CYRILLIC, false));
        big_endian_flag = settings.getBoolean(APP_PREFERENCES_BIG_ENDIAN, false);
        // В зависимости от выбранного порядка айтов, или меняем текст endian, или же
        // Оставляем его в том же виде, т.е. с текстом big
        if (!big_endian_flag) {
            big_endian.setText(LITTLE_ENDIAN);
        }

        // Выбираем тип отображаемых данных. И загружаем его, да
        int val = settings.getInt(APP_PREFERENCES_DATATYPE, 0);
        datatype.setSelection(val);
        chooseMapping(val);

        // Отображаем массив адресов как ось у
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
        // Выбранная ось у, х, то бишь позиция выбранного в текущий момент байта
        // И то, сколько байтов в одной ячейке
        int x = 0, y = 0, width = 0;

        // Массив под старые логические флаги, отображающие то, в
        // каких ячейках что-то было введено
        boolean[][] old_memory = null;
        // Если не первый запуск, а переключение с одного на другой тип
        if (dataTypeMapping != null) {
            // Достаем все важные данные из старого класса
            input_field.removeTextChangedListener(dataTypeMapping.getTextWatcher());
            x = dataTypeMapping.getX();
            y = dataTypeMapping.getY();
            width = dataTypeMapping.getWidth();
            old_memory = dataTypeMapping.getRealMemory();
        }
        // Всегда вырубаем кириллицу. Понадобится - включим в CharMapping
        cyrillic.setEnabled(false);

        // Выбираем класс
        switch (position) {
            case 1:
                dataTypeMapping = new LongMapping(
                        memory, memory_dump, input_field, big_endian, x, y, width
                );
                break;
            case 2:
                dataTypeMapping = new FloatMapping(
                        memory, memory_dump, input_field, big_endian, x, y, width
                );
                break;
            case 3:
                // Я ж сказал, что включим, как понадобится
                cyrillic.setEnabled(true);
                dataTypeMapping = new CharMapping(
                        memory, memory_dump, input_field, big_endian, address_x, cyrillic, x, y, width
                );
                break;
            case 0:
            default:
                // По умолчанию включаем int
                dataTypeMapping = new IntMapping(memory, memory_dump,
                        input_field, big_endian, x, y, width);
                break;
        }

        // С учетом типа данных формируем выпадающий список
        // который будет давать выбор адреса по оси х. Или подадреса, тут уже
        // название на ваше усмотрение
        String[] values = new String[16 / dataTypeMapping.getWidth()];

        for (int i = 0, value = 0; i < values.length; value += dataTypeMapping.getWidth(), ++i)
            values[i] = "*" + Integer.toHexString(value).toUpperCase();

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        values);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        address_x.setAdapter(spinnerArrayAdapter);

        // Ставим выбранным тот х, который соответствует старой позиции с учетом смены типа
        address_x.setSelection(dataTypeMapping.getX());

        // Ставим тип ввода и фильтр
        input_field.setInputType(dataTypeMapping.getInputType());
        input_field.setFilters(dataTypeMapping.getInputFilter());
        input_field.addTextChangedListener(dataTypeMapping.getTextWatcher());

        // Ставим прослушку смены адреса
        address_x.setOnItemSelectedListener(dataTypeMapping.getAddressXListener());
        address_y.setOnItemSelectedListener(dataTypeMapping.getAddressYListener());

        // Записываем данные из памяти с учетом старых флагов
        dataTypeMapping.setBoolean(old_memory);
    }

    @Override
    protected void onStop() {
        // Сохраняем все, что может понадобиться
        super.onStop();
        Log.d(TAG, datatype.getSelectedItemPosition() + "");
        SharedPreferences settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(APP_PREFERENCES_CYRILLIC, cyrillic.isChecked());
        editor.putBoolean(APP_PREFERENCES_BIG_ENDIAN, big_endian_flag);
        editor.putInt(APP_PREFERENCES_DATATYPE, datatype.getSelectedItemPosition());

        editor.apply();
    }

    public static Context getContext() {
        return context;
    }
}