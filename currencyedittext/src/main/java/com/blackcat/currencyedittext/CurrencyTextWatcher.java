package com.blackcat.currencyedittext;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.Currency;
import java.util.Locale;

@SuppressWarnings("unused")
class CurrencyTextWatcher implements TextWatcher {


    private CurrencyEditText editText;
    private Locale defaultLocale;

    private boolean ignoreIteration;
    private String lastGoodInput;

    private byte deletedCentsIndexes;
    private final int fract;

    //double CURRENCY_DECIMAL_DIVISOR;
    final int CURSOR_SPACING_COMPENSATION = 2;

    //Setting a max length because after this length, java represents doubles in scientific notation which breaks the formatter
    final int MAX_RAW_INPUT_LENGTH = 15;


    /**
     * A specialized TextWatcher designed specifically for converting EditText values to a pretty-print string currency value.
     * @param textBox The EditText box to which this TextWatcher is being applied.
     *                Used for replacing user-entered text with formatted text as well as handling cursor position for inputting monetary values
     */
    public CurrencyTextWatcher(CurrencyEditText textBox){
        this(textBox, Locale.US);
    }

    /**
     * A specialized TextWatcher designed specifically for converting EditText values to a pretty-print string currency value.
     * @param textBox The EditText box to which this TextWatcher is being applied.
     *                Used for replacing user-entered text with formatted text as well as handling cursor position for inputting monetary values
     * @param defaultLocale optional locale to default to in the event that the provided CurrencyEditText locale fails due to being unsupported
     */
    public CurrencyTextWatcher(CurrencyEditText textBox, Locale defaultLocale){
        editText = textBox;
        lastGoodInput = "";
        ignoreIteration = false;
        this.defaultLocale = defaultLocale;
        fract = Currency.getInstance(defaultLocale).getDefaultFractionDigits();

        //Different countries use different fractional values for denominations (0.999 <x> vs. 0.99 cents), therefore this must be defined at runtime
//        try{
//            CURRENCY_DECIMAL_DIVISOR = (int) Math.pow(10, Currency.getInstance(editText.getLocale()).getDefaultFractionDigits());
//        }
//        catch(IllegalArgumentException e){
//            Log.e("CurrencyTextWatcher", "Unsupported locale provided, defaulting to Locale.US. Error: " + e.getMessage());
//            CURRENCY_DECIMAL_DIVISOR = (int) Math.pow(10, Currency.getInstance(defaultLocale).getDefaultFractionDigits());
//        }
    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        deletedCentsIndexes = 0;
        if (after == 0) {
            int ix = charSequence.length() - 1;

            for (int i = 0; i < fract; i++) {
                int i1 = ix - i;
                if (i1 >= start && i1 < start + count) {
                    deletedCentsIndexes |= 1<<i;
                }
            }

        }
    }

    /**
     * After each letter is typed, this method will take in the current text, process it, and take the resulting
     * formatted string and place it back in the EditText box the TextWatcher is applied to
     * @param editable text to be transformed
     */
    @Override
    public void afterTextChanged(Editable editable) {
        //Use the ignoreIteration flag to stop our edits to the text field from triggering an endlessly recursive call to afterTextChanged
        if(!ignoreIteration){
            ignoreIteration = true;
            //Start by converting the editable to something easier to work with, then remove all non-digit characters

            int ss = editText.getSelectionStart();
            int se = editText.getSelectionEnd();

            if (deletedCentsIndexes != 0) {
                for (int i = 0; i < fract; i++) {
                    if ((deletedCentsIndexes & (1<<i)) != 0) {
                        editable.insert(editable.length() - i -1, "0");
                    }
                }
            }
            String rawText = editable.toString();

            Long price = parsePrice(rawText);
            if (price == null) return;

            editText.setPrice(price);

            //Store the last known good input so if there are any issues with new input later, we can fall back gracefully.
            lastGoodInput = rawText;

            //setCursorPosition(rawText);
            CurrencyEditText editText = this.editText;
            this.editText.setSelection(safeRange(ss), safeRange(se));
        }
        else{
            ignoreIteration = false;
        }
    }

    private int safeRange(int ss) {
        ss =   ss < editText.length() ? ss : editText.length() - 1;
        ss = ss < 0 ? 0 : ss;
        return ss;
    }

    private Long parsePrice(String rawText) {
        final String newText = (editText.areNegativeValuesAllowed()) ? rawText.replaceAll("[^0-9/-]", "") : rawText.replaceAll("[^0-9]", "");
        if(!newText.equals("") && newText.length() < MAX_RAW_INPUT_LENGTH && !newText.equals("-")) {
            return Long.valueOf(newText);
        }

        return null;
    }

    private void setCursorPosition(String textToDisplay) {
        //locate the position to move the cursor to. The CURSOR_SPACING_COMPENSATION constant is to account for locales where the Euro is displayed as " €" (2 characters).
        //A more robust cursor strategy will be implemented at a later date.
        int cursorPosition = editText.getText().length();
        if(textToDisplay.length() > 0 && Character.isDigit(textToDisplay.charAt(0))) cursorPosition -= CURSOR_SPACING_COMPENSATION;

        //Move the cursor to the end of the numerical value to enter the next number in a right-to-left fashion, like you would on a calculator.
        editText.setSelection(cursorPosition);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

    }
}
