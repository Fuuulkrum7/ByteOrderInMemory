package com.example.course_work;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

public class StandardInputField {
    String allowed;

    InputFilter inputFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
        Spanned dest, int dstart, int dend) {
            String d = null;
            // Если текст есть и он не содержит допустимые символы => содержит недопустимые
            if (source != null) {
                // Меняем их на пустую строку
                for (int i = 0; i < source.length(); ++i) {
                    if (!allowed.contains(("" + source.charAt(i))))
                        d = "";
                    else
                        return null;
                }
            }

            return d;
        }
    };

    public StandardInputField(String allowed) {
        this.allowed = allowed;
    }

    public InputFilter getFilter() {
        return inputFilter;
    }
}
