package com.release.course_work;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

public class StandardInputField {
    public int target = 256;

    InputFilter inputFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
        Spanned dest, int dstart, int dend) {
            String d = null;
            // Если текст есть и он не содержит допустимые символы => содержит недопустимые
            if (source != null) {
                // Меняем их на пустую строку
                for (int i = 0; i < source.length(); ++i) {
                    if (source.charAt(i) >= target)
                        d = "";
                    else
                        return null;
                }
            }

            return d;
        }
    };

    public StandardInputField(int target) {
        this.target = target;
    }

    public InputFilter getFilter() {
        return inputFilter;
    }
}
