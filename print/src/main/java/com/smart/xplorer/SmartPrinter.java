package com.smart.xplorer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.json.JSONException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SmartPrinter {

    public static final int CENTER = -1;
    public static final int RIGHT = -2;
    public static final int LEFT = 0;
    public static final int FULL_WIDTH = -1;
    public static final int ORIGINAL_WIDTH = 0;
    public static final int GET_PRINTER_CODE = 921;

    private static int MAX_CHAR;
//    private static final int MAX_CHAR_WIDE = MAX_CHAR / 2;

    private static SmartPrinter smartPrinter;
    private final PrinterUtil util;
    private final BluetoothDevice printer;

    //----------------------------------------------------------------------------------------------
    // CONSTRUCTOR
    //----------------------------------------------------------------------------------------------

    public SmartPrinter(Context context, int printerWidth) {
        if (printerWidth == 58) {
            MAX_CHAR = 32;
        } else {
            MAX_CHAR = 48;
        }
        Pref.init(context);
        printer = getPrinter();
        util = new PrinterUtil(printer);
    }

    public SmartPrinter(Context context, String printerName) {
        Pref.init(context);
        printer = getPrinter(printerName);
        util = new PrinterUtil(printer);
    }

    public static SmartPrinter with(Context context, int printerWidth, Callback callback) {
        SmartPrinter smartPrinter = new SmartPrinter(context, printerWidth);
        callback.printama(smartPrinter);
        return smartPrinter;
    }

    public static SmartPrinter with(Context context, int printerWidth) {
        smartPrinter = new SmartPrinter(context, printerWidth);
        return smartPrinter;
    }

    static SmartPrinter with(Context context, String printerName) {
        smartPrinter = new SmartPrinter(context, printerName);
        return smartPrinter;
    }

    private static BluetoothDevice getPrinter() {
        return getPrinter(Pref.getString(Pref.SAVED_DEVICE));
    }

    private static BluetoothDevice getPrinter(String printerName) {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice printer = null;
        if (defaultAdapter == null) return null;
        for (BluetoothDevice device : defaultAdapter.getBondedDevices()) {
            if (device.getName().equalsIgnoreCase(printerName)) {
                printer = device;
            }
        }
        return printer;
    }

    public BluetoothDevice getConnectedPrinter() {
        return getPrinter();
    }

    public void connect(final OnConnected onConnected) {
        connect(onConnected, null);
    }

    public void connect(final OnConnected onConnected, final OnFailed onFailed) {
        util.connectPrinter(() -> {
            if (onConnected != null) onConnected.onConnected(this);
        }, () -> {
            if (onFailed != null) onFailed.onFailed("Failed to connect printer");
        });
    }

    public boolean isConnected() {
        return util.isConnected();
    }

    public void close() {
        setNormalText();
        new Handler().postDelayed(util::finish, 2000);
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEST
    //----------------------------------------------------------------------------------------------

    public void printTest(String appName) {
        smartPrinter.connect(printama -> {
            printama.setNormalText();
            printama.printTextln("------------------", SmartPrinter.CENTER);
            printama.printTextln(appName + " print test", SmartPrinter.CENTER);
            printama.printTextln("------------------", SmartPrinter.CENTER);
            printama.feedPaper();
            printama.close();
        });
    }

    //----------------------------------------------------------------------------------------------
    // PRINTER LIST OVERLAY
    //----------------------------------------------------------------------------------------------

    public static void showPrinterList(FragmentActivity activity, OnConnectPrinter onConnectPrinter) {
        showPrinterList(activity, 0, 0, onConnectPrinter);
    }

    public static void showPrinterList(FragmentActivity activity, int activeColor, OnConnectPrinter onConnectPrinter) {
        showPrinterList(activity, activeColor, 0, onConnectPrinter);
    }

    public static void showPrinterList(FragmentActivity activity, int activeColor, int inactiveColor, OnConnectPrinter onConnectPrinter) {
        Pref.init(activity);
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        int activeColorResource = activeColor == 0 ? activeColor : ContextCompat.getColor(activity, activeColor);
        int inactiveColorResource = inactiveColor == 0 ? inactiveColor : ContextCompat.getColor(activity, inactiveColor);
        if (defaultAdapter != null && !defaultAdapter.getBondedDevices().isEmpty()) {
            FragmentManager fm = activity.getSupportFragmentManager();
            DeviceListFragment fragment = DeviceListFragment.newInstance();
            fragment.setDeviceList(defaultAdapter.getBondedDevices());
            fragment.setOnConnectPrinter(onConnectPrinter);
            fragment.setColorTheme(activeColorResource, inactiveColorResource);
            fragment.show(fm, "DeviceListFragment");
        } else {
            onConnectPrinter.onConnectPrinter("failed to connect printer");
        }
    }

    public static void showPrinterList(Activity activity) {
        Pref.init(activity);
        Intent intent = new Intent(activity, ChoosePrinterActivity.class);
        activity.startActivityForResult(intent, SmartPrinter.GET_PRINTER_CODE);
    }

    public static String getPrinterResult(int resultCode, int requestCode, Intent data) {
        String printerName = "failed to get printer";
        if (-1 == resultCode && SmartPrinter.GET_PRINTER_CODE == requestCode && data != null) {
            printerName = data.getStringExtra("printama");
        }
        return printerName;
    }

    //----------------------------------------------------------------------------------------------
    // PRINTER COMMANDS
    //----------------------------------------------------------------------------------------------

    public void setLineSpacing(int lineSpacing) {
        util.setLineSpacing(lineSpacing);
    }

    public void feedPaper() {
        util.feedPaper();
    }


    public void printDashedLine() {
        util.setAlign(LEFT);
        util.printText("--------------------------------");
    }

    public void printLine() {
        util.setAlign(LEFT);
        util.printText("________________________________");
    }

    public void printDoubleDashedLine() {
        util.setAlign(LEFT);
        util.printText("================================");
    }

    public void addNewLine() {
        util.addNewLine();
    }

    public void addNewLine(int count) {
        util.addNewLine(count);
    }

    public String line() {
        return new String(new char[MAX_CHAR]).replace("\0", "-");
    }

    public String lines() {
        return new String(new char[MAX_CHAR]).replace("\0", " ");
    }


    public void addNewLine(int count, ProgressDialog progressDialog) {
        util.addNewLine(count, progressDialog);
    }

    //----------------------------------------------------------------------------------------------
    // PRINT IMAGE BITMAP
    //----------------------------------------------------------------------------------------------

    public boolean printImage(Bitmap bitmap) {
        return util.printImage(bitmap);
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printImage(Bitmap bitmap, int width, int alignment)} instead
     */
    @Deprecated
    public boolean printImage(int alignment, Bitmap bitmap, int width) {
        return util.printImage(alignment, bitmap, width);
    }

    public boolean printImage(Bitmap bitmap, int width, int alignment) {
        return util.printImage(alignment, bitmap, width);
    }

    public boolean printImage(Bitmap bitmap, int width) {
        return util.printImage(bitmap, width);
    }

    public static Bitmap getBitmapFromVector(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        return getBitmapFromVector(drawable);
    }

    public static Bitmap getBitmapFromVector(Drawable drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = drawable != null ? (DrawableCompat.wrap(drawable)).mutate() : null;
        }
        if (drawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
        return null;
    }

    public void printFromView(View view) {
        ViewTreeObserver vto = view.getViewTreeObserver();
        View finalView = view; // needs to create new variable
        AtomicInteger viewWidth = new AtomicInteger(view.getMeasuredWidth());
        AtomicInteger viewHeight = new AtomicInteger(view.getMeasuredHeight());
        vto.addOnGlobalLayoutListener(() -> {
            viewWidth.set(finalView.getMeasuredWidth());
            viewHeight.set(finalView.getMeasuredHeight());
        });
        new Handler().postDelayed(() -> loadBitmapAndPrint(view, viewWidth.get(), viewHeight.get()), 500);
    }

    private void loadBitmapAndPrint(View view, int viewWidth, int viewHeight) {
        Bitmap b = loadBitmapFromView(view, viewWidth, viewHeight);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> smartPrinter.printImage(b));
    }

    public Bitmap loadBitmapFromView(View view, int viewWidth, int viewHeight) {
        Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        ColorMatrix ma = new ColorMatrix();
        ma.setSaturation(0);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(ma));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return bitmap;
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEXT
    //----------------------------------------------------------------------------------------------

    public void printText(String text) {
        printText(text, LEFT);
    }

    public void printTextRight(String text) {
        setBold();
        printText(text, RIGHT);
    }

    public void printTexCenter(String text) {
        setBold();
        printText(text, CENTER);
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printText(String text, int align)} instead
     */
    @Deprecated
    public void printText(int align, String text) {
        util.setAlign(align);
        util.printText(text);
    }

    public void printText(String text, int align) {
        util.setAlign(align);
        util.printText(text);
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextln(String text, int align)} instead
     */
    @Deprecated
    public void printTextln(int align, String text) {
        util.setAlign(align);
        printTextln(text);
    }

    public void printTextln(String text, int align) {
        util.setAlign(align);
        printTextln(text);
    }

    public void printTextlnTitle(String text) {
        text = text + "\n";
        util.printTextTitle(text);
    }

    public void printTextln(String text) {
        text = text + "\n";
        util.printText(text);
//        Log.e("printText", util.printText(text)+" ");
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEXT JUSTIFY ALIGNMENT
    //----------------------------------------------------------------------------------------------

    public void printTextJustify(String text1, String text2) {
        String justifiedText = getJustifiedText(text1, text2);
        printText(justifiedText);
    }

    public void printTextJustifyRight(String text1, String text2) {
        String justifiedText = getJustifiedText(text1, text2);
        printTextRight(justifiedText);
    }

    public void printTextJustifyCenter(String text1, String text2) {
        String justifiedText = getJustifiedText(text1, text2);
        printTexCenter(justifiedText);
    }

    public void printTextJustify(String text1, String text2, String text3) {
        util.setAlign(CENTER);
//        printTextln(text2);
        String justifiedText = getJustifiedText(text1, text2, text3);
        printText(justifiedText);
    }

    public void printTextJustify_(String text1, String text2, String text3) {
        String justifiedText = getJustifiedText_(text1, text2, text3);
        printTextBold(justifiedText);
    }

    public void printTextJustifyBold_(String text1, String text2) {
        String justifiedText = getJustifiedText_(text1, text2);
        printTextBold(justifiedText);
    }

    public void printTextJustifyBold__(String text1, String text2) {
        String justifiedText = getJustifiedText_(text1, text2);
        printTextBold_(justifiedText);
    }

    public void printWinningTicket(String text1, String text2) {
        String justifiedText = getJustifiedTextWinningTicket(text1, text2);
        printTextBold_(justifiedText);
    }

    public void printHistoricalSales(String text1, String text2) {
        String justifiedText = getJustifiedTextHistoricalSales(text1, text2);
        printTextBold_(justifiedText);
    }


    public void printTextJustify(String text1, String text2, String text3, String text4) {

        String justifiedText = getJustifiedText(text1, text2, text3, text4);
        printText(justifiedText);
    }

    public void printTextJustifyBold(String text1, String text2) {
        String justifiedText = getJustifiedText(text1, text2);
        printTextBold(justifiedText);
    }

    public void printTextJustifyBold(String text1, String text2, String text3) {
        String justifiedText = getJustifiedText(text1, text2, text3);
        printTextBold(justifiedText);
    }

    public void printTextJustifyBold(String text1, String text2, String text3, String text4) {
        String justifiedText = getJustifiedText(text1, text2, text3, text4);
        printTextBold(justifiedText);
    }

    private String getJustifiedText(String text1, String text2) {
        String justifiedText = "";
        justifiedText = text1 + getSpaces(text1, text2) + text2;
        return justifiedText;
    }

    private String getJustifiedText_(String text1, String text2, String text3) {
        String justifiedText = "";

        justifiedText = getSpaces_(text1, text2, text3);
        return justifiedText;
    }

    private String getJustifiedText_(String text1, String text2) {
        String justifiedText = "";

        justifiedText = getSpaces_(text1, text2);
        return justifiedText;
    }

    private String getJustifiedTextWinningTicket(String text1, String text2) {
        String justifiedText = "";

        justifiedText = getSpacesWinnigTicket(text1, text2);
        return justifiedText;
    }

    private String getJustifiedTextHistoricalSales(String text1, String text2) {
        String justifiedText = "";

        justifiedText = getSpacesHistoricalSales(text1, text2);
        return justifiedText;
    }

    private String getJustifiedText(String text1, String text2, String text3) {
        String justifiedText = "";
        String text12 = text1 + getSpaces(text1, text2, text3) + text2;
        justifiedText = text12 + getSpaces(text12, text3) + text3;
        return justifiedText;
    }

    private String getJustifiedText(String text1, String text2, String text3, String text4) {
        String justifiedText = "";
        String text12 = text1 + getSpaces(text1, text2, text3, text4) + text2;
        String text123 = text12 + getSpaces(text12, text3, text4) + text3;
        justifiedText = text123 + getSpaces(text123, text4) + text4;
        return justifiedText;
    }

    private String getSpaces(String text1, String text2) {
        int text1Length = text1.length();
        int text2Length = text2.length();
        int spacesCount = MAX_CHAR - text1Length - text2Length;
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < spacesCount; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    public static String centerText(String text) {

        if (text.length() >= 32) {
            return text;
        }

        int spaces = (32 - text.length()) / 2;

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < spaces; i++) {
            builder.append(" ");
        }

        builder.append(text);

        return builder.toString();
    }

    public static String lineSpace(int lines) {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < lines; i++) {
            builder.append("\n");
        }

        return builder.toString();
    }

    public String center(String text, int align) {

        int spaces = (MAX_CHAR - text.length()) / 2;

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < spaces; i++) {
            builder.append(" ");
        }

        setBold();
        util.setAlign(align);

        builder.append(text);


        return builder.toString();
    }

    public String twoCol(String left, String right) {

        int spaces = MAX_CHAR - left.length() - right.length();

        if (spaces < 1) spaces = 1;

        StringBuilder builder = new StringBuilder();
        builder.append(left);

        for (int i = 0; i < spaces; i++) {
            builder.append(" ");
        }

        builder.append(right);

        return builder.toString();
    }

    public String threeCol(String a, String b, String c) {

        String space = "";

        if (MAX_CHAR == 32) {
            if (b.length() == 2) {
                space = "%-14s %1s %14s";
            } else if (b.length() == 3) {
                space = "%-13s %2s %14s";
            } else if (b.length() == 4) {
                space = "%-12s %3s %14s";
            } else if (b.length() == 5) {
                space = "%-11s %4s %14s";
            }
            return String.format(space, a, b, c);
        } else {
            return String.format("%-24s %8s %14s", a, b, c);
        }
    }

    public String fourCol(String a, String b, String c, String d) {

        if (MAX_CHAR == 32) {
            return String.format("%-10s %5s %7s %8s", a, b, c, d);
        } else {
            return String.format("%-16s %8s %10s %12s", a, b, c, d);
        }
    }

    private String getSpaces_(String text1, String text2, String text3) {
        String spaces = "";
        int text1Length = text1.length();
        int text2Length = text2.length();
        int text3Length = text3.length();

        //LOTTERY 2 NUMBERS
        if (text1Length == 3 && text2Length == 2 && text3Length == 4) {

            spaces = text1 + "              " + text2 + "         " + text3;
        }
        if (text1Length == 3 && text2Length == 2 && text3Length == 5) {

            spaces = text1 + "              " + text2 + "        " + text3;
        }
        if (text1Length == 3 && text2Length == 2 && text3Length == 6) {

            spaces = text1 + "              " + text2 + "       " + text3;
        }
        if (text1Length == 3 && text2Length == 2 && text3Length == 7) {

            spaces = text1 + "              " + text2 + "      " + text3;
        }
        if (text1Length == 3 && text2Length == 2 && text3Length == 8) {

            spaces = text1 + "              " + text2 + "     " + text3;
        }
        if (text1Length == 3 && text2Length == 2 && text3Length == 9) {

            spaces = text1 + "              " + text2 + "    " + text3;
        }
        //MARIAGE NUMBERS
        if (text1Length == 3 && text2Length == 5 && text3Length == 4) {

            spaces = text1 + "           " + text2 + "         " + text3;
        }
        if (text1Length == 3 && text2Length == 5 && text3Length == 5) {

            spaces = text1 + "           " + text2 + "        " + text3;
        }
        if (text1Length == 3 && text2Length == 5 && text3Length == 6) {

            spaces = text1 + "           " + text2 + "       " + text3;
        }
        if (text1Length == 3 && text2Length == 5 && text3Length == 7) {

            spaces = text1 + "           " + text2 + "      " + text3;
        }
        if (text1Length == 3 && text2Length == 5 && text3Length == 8) {

            spaces = text1 + "           " + text2 + "     " + text3;
        }
        if (text1Length == 3 && text2Length == 5 && text3Length == 9) {

            spaces = text1 + "           " + text2 + "    " + text3;
        }
        //LOTTO 3 NUMBERS && PIC 3 BOX
        if (text1Length == 2 && text2Length == 3 && text3Length == 4) {

            spaces = text1 + "              " + text2 + "         " + text3;
        }
        if (text1Length == 2 && text2Length == 3 && text3Length == 5) {

            spaces = text1 + "              " + text2 + "        " + text3;
        }
        if (text1Length == 2 && text2Length == 3 && text3Length == 6) {

            spaces = text1 + "              " + text2 + "       " + text3;
        }
        if (text1Length == 2 && text2Length == 3 && text3Length == 7) {

            spaces = text1 + "              " + text2 + "      " + text3;
        }
        if (text1Length == 2 && text2Length == 3 && text3Length == 8) {

            spaces = text1 + "              " + text2 + "     " + text3;
        }
        if (text1Length == 2 && text2Length == 3 && text3Length == 9) {

            spaces = text1 + "              " + text2 + "    " + text3;
        }
        //LOTTO 4 NUMBERS
        if (text1Length == 4 && text2Length == 4 && text3Length == 4) {

            spaces = text1 + "           " + text2 + "         " + text3;
        }
        if (text1Length == 4 && text2Length == 4 && text3Length == 5) {

            spaces = text1 + "           " + text2 + "        " + text3;
        }
        if (text1Length == 4 && text2Length == 4 && text3Length == 6) {

            spaces = text1 + "           " + text2 + "       " + text3;
        }
        if (text1Length == 4 && text2Length == 4 && text3Length == 7) {

            spaces = text1 + "           " + text2 + "      " + text3;
        }
        if (text1Length == 4 && text2Length == 4 && text3Length == 8) {

            spaces = text1 + "           " + text2 + "     " + text3;
        }
        if (text1Length == 4 && text2Length == 4 && text3Length == 9) {

            spaces = text1 + "           " + text2 + "    " + text3;
        }
        //LOTTO 5 NUMBERS
        if (text1Length == 4 && text2Length == 5 && text3Length == 4) {

            spaces = text1 + "          " + text2 + "         " + text3;
        }
        if (text1Length == 4 && text2Length == 5 && text3Length == 5) {

            spaces = text1 + "          " + text2 + "        " + text3;
        }
        if (text1Length == 4 && text2Length == 5 && text3Length == 6) {

            spaces = text1 + "          " + text2 + "       " + text3;
        }
        if (text1Length == 4 && text2Length == 5 && text3Length == 7) {

            spaces = text1 + "          " + text2 + "      " + text3;
        }
        if (text1Length == 4 && text2Length == 5 && text3Length == 8) {

            spaces = text1 + "          " + text2 + "     " + text3;
        }
        if (text1Length == 4 && text2Length == 5 && text3Length == 9) {

            spaces = text1 + "          " + text2 + "    " + text3;
        }
        //PIC 2 FLORIDA
        if (text1Length == 2 && text2Length == 2 && text3Length == 4) {

            spaces = text1 + "               " + text2 + "         " + text3;
        }
        if (text1Length == 2 && text2Length == 2 && text3Length == 5) {

            spaces = text1 + "               " + text2 + "        " + text3;
        }
        if (text1Length == 2 && text2Length == 2 && text3Length == 6) {

            spaces = text1 + "               " + text2 + "       " + text3;
        }
        if (text1Length == 2 && text2Length == 2 && text3Length == 7) {

            spaces = text1 + "               " + text2 + "      " + text3;
        }
        if (text1Length == 2 && text2Length == 2 && text3Length == 8) {

            spaces = text1 + "               " + text2 + "     " + text3;
        }
        if (text1Length == 2 && text2Length == 2 && text3Length == 9) {

            spaces = text1 + "               " + text2 + "    " + text3;
        }


        return spaces;
    }

    private String getSpaces_(String text1, String text2) {
        String spaces = "";
        int text1Length = text1.length();
        int text2Length = text2.length();

        //TOTAL
        if (text1Length == 5) {

            spaces = "                " + text1 + " " + text2;
        }
        //AGENT
        if (text1Length == 5 && text2Length == 3) {

            spaces = "         " + text1 + "              " + text2;
        }
        if (text1Length == 5 && text2Length == 4) {

            spaces = "         " + text1 + "             " + text2;
        }
        if (text1Length == 5 && text2Length == 5) {

            spaces = "         " + text1 + "             " + text2;
        }
        if (text1Length == 5 && text2Length == 6) {

            spaces = "         " + text1 + "            " + text2;
        }
        if (text1Length == 5 && text2Length == 7) {

            spaces = "         " + text1 + "           " + text2;
        }
        if (text1Length == 5 && text2Length == 8) {

            spaces = "         " + text1 + "          " + text2;
        }
        //TICKET PENDING
        if (text1Length == 17 && text2Length == 1) {

            spaces = "  " + text1 + "           " + text2;
        }
        if (text1Length == 17 && text2Length == 2) {

            spaces = "  " + text1 + "          " + text2;
        }
        if (text1Length == 17 && text2Length == 3) {

            spaces = "  " + text1 + "         " + text2;
        }
        if (text1Length == 17 && text2Length == 4) {

            spaces = "  " + text1 + "        " + text2;
        }
        if (text1Length == 17 && text2Length == 5) {

            spaces = "  " + text1 + "       " + text2;
        }
        //TICKET LOSS, WIN & BALANCE OF THE DAY
        if (text1Length == 15 && text2Length == 1) {

            spaces = "  " + text1 + "             " + text2;
        }
        if (text1Length == 15 && text2Length == 2) {

            spaces = "  " + text1 + "            " + text2;
        }
        if (text1Length == 15 && text2Length == 3) {

            spaces = "  " + text1 + "           " + text2;
        }
        if (text1Length == 15 && text2Length == 4) {

            spaces = "  " + text1 + "          " + text2;
        }
        if (text1Length == 15 && text2Length == 5) {

            spaces = "  " + text1 + "         " + text2;
        }
        if (text1Length == 15 && text2Length == 6) {

            spaces = "  " + text1 + "        " + text2;
        }
        if (text1Length == 15 && text2Length == 7) {

            spaces = "  " + text1 + "       " + text2;
        }
        if (text1Length == 15 && text2Length == 8) {

            spaces = "  " + text1 + "      " + text2;
        }
        if (text1Length == 15 && text2Length == 9) {

            spaces = "  " + text1 + "     " + text2;
        }
        // TOTAL TICKETS
        if (text1Length == 13 && text2Length == 1) {

            spaces = "   " + text1 + "              " + text2;
        }
        if (text1Length == 13 && text2Length == 2) {

            spaces = "   " + text1 + "             " + text2;
        }
        if (text1Length == 13 && text2Length == 3) {

            spaces = "   " + text1 + "            " + text2;
        }
        if (text1Length == 13 && text2Length == 4) {

            spaces = "   " + text1 + "           " + text2;
        }
        if (text1Length == 13 && text2Length == 5) {

            spaces = "   " + text1 + "          " + text2;
        }
        // SALES
        if (text1Length == 6 && text2Length == 4) {

            spaces = "      " + text1 + "               " + text2;
        }
        if (text1Length == 6 && text2Length == 5) {

            spaces = "      " + text1 + "              " + text2;
        }
        if (text1Length == 6 && text2Length == 6) {

            spaces = "      " + text1 + "             " + text2;
        }
        if (text1Length == 6 && text2Length == 7) {

            spaces = "      " + text1 + "            " + text2;
        }
        if (text1Length == 6 && text2Length == 8) {

            spaces = "      " + text1 + "           " + text2;
        }
        // FEES AND PRIZES WON
        if (text1Length == 11 && text2Length == 4) {

            spaces = "   " + text1 + "             " + text2;
        }
        if (text1Length == 11 && text2Length == 5) {

            spaces = "   " + text1 + "            " + text2;
        }
        if (text1Length == 11 && text2Length == 6) {

            spaces = "   " + text1 + "           " + text2;
        }
        if (text1Length == 11 && text2Length == 7) {

            spaces = "   " + text1 + "          " + text2;
        }
        if (text1Length == 11 && text2Length == 8) {

            spaces = "   " + text1 + "         " + text2;
        }
        // NET
        if (text1Length == 5 && text2Length == 4) {

            spaces = "      " + text1 + "                " + text2;
        }
        if (text1Length == 5 && text2Length == 5) {

            spaces = "      " + text1 + "               " + text2;
        }
        if (text1Length == 5 && text2Length == 6) {

            spaces = "      " + text1 + "              " + text2;
        }
        if (text1Length == 5 && text2Length == 7) {

            spaces = "      " + text1 + "             " + text2;
        }
        if (text1Length == 5 && text2Length == 8) {

            spaces = "      " + text1 + "            " + text2;
        }
        // LOTTERY | FLORIDA AM, FLORIDA PM & GEORGIA AM
        if (text1Length == 7 && text2Length == 10) {

            spaces = "     " + text1 + "         " + text2;
        }
        // LOTTERY | NEW YORK AM & NEW YORK PM
        if (text1Length == 7 && text2Length == 11) {

            spaces = "     " + text1 + "        " + text2;
        }
        // LOTTERY | GEORGIA EVENING 6H
        if (text1Length == 7 && text2Length == 18) {

            spaces = "     " + text1 + " " + text2;
        }
        if (text1Length == 7 && text2Length == 14) {

            spaces = "     " + text1 + "     " + text2;
        }
        if (text1Length == 7 && text2Length == 8) {

            spaces = "     " + text1 + "           " + text2;
        }
        // LOTTERY | 1er LOT
        if (text1Length == 7 && text2Length == 2) {

            spaces = "     " + text1 + "                 " + text2;
        }
        // LOTTERY | 2e LOT & 3e LOT
        if (text1Length == 6 && text2Length == 2) {

            spaces = "     " + text1 + "                  " + text2;
        }
        // LOTTERY | LOTO 3
        if (text1Length == 6 && text2Length == 3) {

            spaces = "     " + text1 + "                 " + text2;
        }
        // LOTTERY | LOTO, 4-1, 4-2 & 4-3
        if (text1Length == 8 && text2Length == 4) {

            spaces = "    " + text1 + "               " + text2;
        }
        // LOTTERY | LOTO, 5-1, 5-2 & 5-3
        if (text1Length == 8 && text2Length == 5) {

            spaces = "    " + text1 + "              " + text2;
        }
        // LOTTERY | PICK 2 FLORIDA
        if (text1Length == 9 && text2Length == 2) {

            spaces = "    " + text1 + "                " + text2;
        }

        return spaces;
    }

    private String getSpacesWinnigTicket(String text1, String text2) {
        String spaces = "";
        int text1Length = text1.length();
        int text2Length = text2.length();

        //AGENT
        if (text1Length == 5 && text2Length == 3) {

            spaces = "   " + text1 + "                " + text2 + "     ";
        }
        if (text1Length == 5 && text2Length == 4) {

            spaces = "   " + text1 + "               " + text2 + "     ";
        }
        if (text1Length == 5 && text2Length == 5) {

            spaces = "   " + text1 + "              " + text2 + "    ";
        }
        if (text1Length == 5 && text2Length == 6) {

            spaces = "  " + text1 + "              " + text2 + "   ";
        }
        if (text1Length == 5 && text2Length == 7) {

            spaces = "  " + text1 + "             " + text2 + "  ";
        }
        if (text1Length == 5 && text2Length == 8) {

            spaces = " " + text1 + "             " + text2 + " ";
        }
        if (text1Length == 5 && text2Length == 9) {

            spaces = "" + text1 + "            " + text2 + "";
        }
        //LABEL NO. TICKET && WINNING PRIZE
        if (text1Length == 12 && text2Length == 13) {

            spaces = "" + text1 + "       " + text2;
        }
        //NO. TICKET && WINNING PRIZE
        if (text1Length == 8 && text2Length == 4) {

            spaces = "  " + text1 + "             " + text2 + "     ";
        }
        if (text1Length == 8 && text2Length == 5) {

            spaces = "  " + text1 + "             " + text2 + "    ";
        }
        if (text1Length == 8 && text2Length == 6) {

            spaces = "  " + text1 + "            " + text2 + "    ";
        }
        if (text1Length == 8 && text2Length == 7) {

            spaces = "  " + text1 + "             " + text2 + "             ";
        }
        if (text1Length == 8 && text2Length == 8) {

            spaces = "  " + text1 + "            " + text2 + "  ";
        }
        if (text1Length == 8 && text2Length == 9) {

            spaces = "  " + text1 + "          " + text2 + "   ";
        }
        if (text1Length == 8 && text2Length == 10) {

            spaces = "  " + text1 + "         " + text2 + "   ";
        }
        if (text1Length == 8 && text2Length == 12) {

            spaces = "  " + text1 + "         " + text2 + " ";
        }
        if (text1Length == 8 && text2Length == 13) {

            spaces = "  " + text1 + "       " + text2 + "  ";
        }
        if (text1Length == 8 && text2Length == 14) {

            spaces = "  " + text1 + "       " + text2 + " ";
        }
        if (text1Length == 8 && text2Length == 16) {

            spaces = "  " + text1 + "      " + text2 + "   ";
        }

        return spaces;
    }

    private String getSpacesHistoricalSales(String text1, String text2) {
        String spaces = "";
        int text1Length = text1.length();
        int text2Length = text2.length();

        //AGENT
        if (text1Length == 5 && text2Length == 3) {

            spaces = "   " + text1 + "                " + text2 + "     ";
        }
        if (text1Length == 5 && text2Length == 4) {

            spaces = "   " + text1 + "               " + text2 + "     ";
        }
        if (text1Length == 5 && text2Length == 5) {

            spaces = "   " + text1 + "              " + text2 + "    ";
        }
        if (text1Length == 5 && text2Length == 6) {

            spaces = "  " + text1 + "              " + text2 + "   ";
        }
        if (text1Length == 5 && text2Length == 7) {

            spaces = "  " + text1 + "             " + text2 + "  ";
        }
        if (text1Length == 5 && text2Length == 8) {

            spaces = " " + text1 + "             " + text2 + " ";
        }
        if (text1Length == 5 && text2Length == 9) {

            spaces = "" + text1 + "            " + text2 + "";
        }
        //REFERENCE
        if (text1Length == 3 && text2Length == 3) {

            spaces = "    " + text1 + "                 " + text2 + "    ";
        }
        if (text1Length == 3 && text2Length == 4) {

            spaces = "    " + text1 + "                " + text2 + "    ";
        }
        if (text1Length == 3 && text2Length == 5) {

            spaces = "    " + text1 + "                " + text2 + "    ";
        }
        if (text1Length == 3 && text2Length == 6) {

            spaces = "    " + text1 + "               " + text2 + "    ";
        }
        if (text1Length == 3 && text2Length == 7) {

            spaces = "    " + text1 + "               " + text2 + "   ";
        }
        if (text1Length == 3 && text2Length == 8) {

            spaces = "    " + text1 + "              " + text2 + "  ";
        }
        if (text1Length == 3 && text2Length == 9) {

            spaces = "    " + text1 + "               " + text2 + " ";
        }
        if (text1Length == 3 && text2Length == 10) {

            spaces = "  " + text1 + "             " + text2 + "";
        }
        if (text1Length == 3 && text2Length == 11) {

            spaces = "   " + text1 + "             " + text2 + "";
        }
        if (text1Length == 3 && text2Length == 12) {

            spaces = "   " + text1 + "            " + text2 + "";
        }
        //NUMBER TICKET
        if (text1Length == 7 && text2Length == 1) {

            spaces = "  " + text1 + "                " + text2 + "      ";
        }
        if (text1Length == 7 && text2Length == 2) {

            spaces = "  " + text1 + "               " + text2 + "      ";
        }
        if (text1Length == 7 && text2Length == 3) {

            spaces = "  " + text1 + "               " + text2 + "     ";
        }
        if (text1Length == 7 && text2Length == 4) {

            spaces = "  " + text1 + "               " + text2 + "    ";
        }
        if (text1Length == 7 && text2Length == 5) {

            spaces = "  " + text1 + "              " + text2 + "    ";
        }
        if (text1Length == 7 && text2Length == 6) {

            spaces = "  " + text1 + "             " + text2 + "    ";
        }
        if (text1Length == 7 && text2Length == 7) {

            spaces = "  " + text1 + "             " + text2 + "   ";
        }
        if (text1Length == 7 && text2Length == 8) {

            spaces = "  " + text1 + "            " + text2 + "   ";
        }
        if (text1Length == 7 && text2Length == 9) {

            spaces = "  " + text1 + "            " + text2 + "  ";
        }
        if (text1Length == 7 && text2Length == 10) {

            spaces = "  " + text1 + "           " + text2 + "  ";
        }
        //SALES
        if (text1Length == 6 && text2Length == 1) {

            spaces = "  " + text1 + "                " + text2 + "      ";
        }
        if (text1Length == 6 && text2Length == 2) {

            spaces = "  " + text1 + "               " + text2 + "      ";
        }
        if (text1Length == 6 && text2Length == 3) {

            spaces = "  " + text1 + "               " + text2 + "     ";
        }
        if (text1Length == 6 && text2Length == 4) {

            spaces = "  " + text1 + "               " + text2 + "    ";
        }
        if (text1Length == 6 && text2Length == 5) {

            spaces = "  " + text1 + "              " + text2 + "    ";
        }
        if (text1Length == 6 && text2Length == 6) {

            spaces = "  " + text1 + "              " + text2 + "   ";
        }
        if (text1Length == 6 && text2Length == 8) {

            spaces = "  " + text1 + "             " + text2 + "  ";
        }
        if (text1Length == 6 && text2Length == 9) {

            spaces = "  " + text1 + "             " + text2 + "  ";
        }
        if (text1Length == 6 && text2Length == 10) {

            spaces = "  " + text1 + "            " + text2 + " ";
        }
        if (text1Length == 6 && text2Length == 12) {

            spaces = "  " + text1 + "            " + text2 + "";
        }
        if (text1Length == 6 && text2Length == 13) {

            spaces = "  " + text1 + "           " + text2 + "";
        }
        if (text1Length == 6 && text2Length == 14) {

            spaces = "  " + text1 + "          " + text2 + "";
        }
        //FEES && WINNING PRIZE
        if (text1Length == 11 && text2Length == 4) {

            spaces = "" + text1 + "            " + text2 + "    ";
        }
        if (text1Length == 11 && text2Length == 5) {

            spaces = "" + text1 + "            " + text2 + "    ";
        }
        if (text1Length == 11 && text2Length == 6) {

            spaces = "" + text1 + "            " + text2 + "   ";
        }
        if (text1Length == 11 && text2Length == 7) {

            spaces = "" + text1 + "           " + text2 + "   ";
        }
        if (text1Length == 11 && text2Length == 8) {

            spaces = "" + text1 + "          " + text2 + "  ";
        }
        if (text1Length == 11 && text2Length == 9) {

            spaces = "" + text1 + "           " + text2 + " ";
        }
        if (text1Length == 11 && text2Length == 10) {

            spaces = "" + text1 + "           " + text2 + "";
        }
        if (text1Length == 11 && text2Length == 11) {

            spaces = "" + text1 + "          " + text2 + "";
        }
        if (text1Length == 11 && text2Length == 12) {

            spaces = "" + text1 + "         " + text2 + "";
        }
        if (text1Length == 11 && text2Length == 13) {

            spaces = "" + text1 + "        " + text2 + "";
        }
        if (text1Length == 11 && text2Length == 14) {

            spaces = "" + text1 + "       " + text2 + "";
        }
        //TOTAL BALANCE
        if (text1Length == 14 && text2Length == 4) {

            spaces = "" + text1 + "          " + text2 + "    ";
        }
        if (text1Length == 14 && text2Length == 5) {

            spaces = "" + text1 + "         " + text2 + "    ";
        }
        if (text1Length == 14 && text2Length == 6) {

            spaces = "" + text1 + "         " + text2 + "    ";
        }
        if (text1Length == 14 && text2Length == 8) {

            spaces = "" + text1 + "       " + text2 + "  ";
        }
        if (text1Length == 14 && text2Length == 9) {

            spaces = "" + text1 + "      " + text2 + "  ";
        }
        if (text1Length == 14 && text2Length == 10) {

            spaces = "" + text1 + "     " + text2 + "  ";
        }
        if (text1Length == 14 && text2Length == 12) {

            spaces = "" + text1 + "      " + text2 + "";
        }
        if (text1Length == 14 && text2Length == 13) {

            spaces = "" + text1 + "     " + text2 + "";
        }
        if (text1Length == 14 && text2Length == 14) {

            spaces = "" + text1 + "    " + text2 + "";
        }
        if (text1Length == 14 && text2Length == 16) {

            spaces = "" + text1 + "  " + text2 + "";
        }
        if (text1Length == 14 && text2Length == 17) {

            spaces = "" + text1 + " " + text2 + "";
        }
        if (text1Length == 14 && text2Length == 18) {

            spaces = "" + text1 + "" + text2 + "";
        }

        return spaces;
    }

    private String getSpaces(String text1, String text2, String text3) {
        int text1Length = text1.length();
        int text2Length = text2.length();
        int text3Length = text3.length();
        double spacesCount = (MAX_CHAR - text1Length - text2Length - text3Length) / 2;
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < spacesCount; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    private String getSpaces(String text1, String text2, String text3, String text4) {
        int text1Length = text1.length();
        int text2Length = text2.length();
        int text3Length = text3.length();
        int text4Length = text4.length();
        int spacesCount = (MAX_CHAR - text1Length - text2Length - text3Length - text4Length) / 3;
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < spacesCount; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEXT WITH FORMATTING
    //----------------------------------------------------------------------------------------------

    // Normal
    public void printTextNormal(String text) {
        setNormalText();
        printText(text, LEFT);
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextNormal(String text, int align)} instead
     */
    @Deprecated
    public void printTextNormal(int align, String text) {
        setNormalText();
        util.setAlign(align);
        util.printText(text);
    }

    public void printTextNormal(String text, int align) {
        setNormalText();
        util.setAlign(align);
        util.printText(text);
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnNormal(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnNormal(int align, String text) {
        setNormalText();
        util.setAlign(align);
        printTextln(text);
    }

    public void printTextlnNormal(String text, int align) {
        setNormalText();
        util.setAlign(align);
        printTextln(text);
    }

    public void printTextlnNormal(String text) {
        setNormalText();
        text = text + "\n";
        util.printText(text);
//        util.printTextTall(text);
//        util.printTextBold(text);
//        util.printTextWide(text);
    }

    public void printTextTalllnNormal(String text) {
        setNormalText();
        text = text + "\n";
//        util.printText(text);
        util.printTextTall(text);
//        util.printTextBold(text);
//        util.printTextWide(text);
    }

    public void printBarCode(String text) {
        util.printBarCode(text);
    }

    public void printQrCode(String text) {
        util.qrCode(text);
    }

    public void printTextBold_(String text) {
        setBold();
        text = text + "\n";
        util.printText(text);
    }

    // Bold
    public void printTextBold(String text) {
        setBold();
        printTextlnTitleBold(text, LEFT);
//        setWideTall();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextBold(int align, String text) {
        setBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    public void printTextBold(String text, int align) {
        setBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnBold(int align, String text) {
        setBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnBold(String text, int align) {
        setBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnTitleBold(String text, int align) {
        setBold();
        util.setAlign(align);
        printTextlnTitle(text);
        setNormalText();
    }

    public void printTextlnBold(String text) {
        setBold();
        text = text + "\n";
        util.printText(text);
        setNormalText();
    }

    // Tall
    public void printTextTall(String text) {
        setTall();
        printText(text, LEFT);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextTall(String text, int align)} instead
     */
    @Deprecated
    public void printTextTall(int align, String text) {
        setTall();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    public void printTextTall(String text, int align) {
        setTall();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnTall(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnTall(int align, String text) {
        setTall();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnTall(String text, int align) {
//        setTall();
        util.setAlign(align);
        util.printTextTall(text);
//        setNormalText();
    }

    public void printTextlnTall(String text) {
//        setTall();
        text = text + "\n";
        util.printTextTall(text);
        setNormalText();
    }

    // TallBold
    public void printTextTallBold(String text) {
        setTallBold();
        printText(text, LEFT);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextTallBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextTallBold(int align, String text) {
        setTallBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    public void printTextTallBold(String text, int align) {
        setTallBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnTallBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnTallBold(int align, String text) {
        setTallBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnTallBold(String text, int align) {
        setTallBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnTallBold(String text) {
        setTallBold();
        text = text + "\n";
        util.printText(text);
        setNormalText();
    }

    // Wide
    public void printTextWide(String text) {
        setWide();
        printText(text, LEFT);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextWide(String text, int align)} instead
     */
    @Deprecated
    public void printTextWide(int align, String text) {
        setWide();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    public void printTextWide(String text, int align) {
        setWide();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnWide(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnWide(int align, String text) {
        setWide();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWide(String text, int align) {
        setWide();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWide(String text) {
        setWide();
        text = text + "\n";
        util.printText(text);
        setNormalText();
    }

    // WideBold
    public void printTextWideBold(String text) {
        setWideBold();
        printText(text, LEFT);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextWideBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextWideBold(int align, String text) {
        setWideBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    public void printTextWideBold(String text, int align) {
        setWideBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnWideBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnWideBold(int align, String text) {
        setWideBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWideBold(String text, int align) {
        setWideBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWideBold(String text) {
        setWideBold();
        text = text + "\n";
        util.printText(text);
        setNormalText();
    }

    // WideTall
    public void printTextWideTall(String text) {
        setWideTall();
        printText(text, LEFT);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextWideTall(String text, int align)} instead
     */
    @Deprecated
    public void printTextWideTall(int align, String text) {
        setWideTall();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    public void printTextWideTall(String text, int align) {
        setWideTall();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnWideTall(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnWideTall(int align, String text) {
        setWideTall();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWideTall(String text, int align) {
        setWideTall();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWideTall(String text) {
        setWideTall();
        text = text + "\n";
        util.printText(text);
        setNormalText();
    }

    // WideTallBold
    public void printTextWideTallBold(String text) {
        setWideTallBold();
        printText(text, LEFT);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextWideTallBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextWideTallBold(int align, String text) {
        setWideTallBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    public void printTextWideTallBold(String text, int align) {
        setWideTallBold();
        util.setAlign(align);
        util.printText(text);
        setNormalText();
    }

    /**
     * @deprecated As of release 1.0.0,
     * replaced by {@link SmartPrinter#printTextlnWideTallBold(String text, int align)} instead
     */
    @Deprecated
    public void printTextlnWideTallBold(int align, String text) {
        setWideTallBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWideTallBold(String text, int align) {
        setWideTallBold();
        util.setAlign(align);
        printTextln(text);
        setNormalText();
    }

    public void printTextlnWideTallBold(String text) {
        setWideTallBold();
        text = text + "\n";
        util.printText(text);
        setNormalText();
    }

    //----------------------------------------------------------------------------------------------
    // TEXT FORMAT
    //----------------------------------------------------------------------------------------------

    public void setNormalText() {
        util.setNormalText();
    }

    public void setSmallText() {
        util.setSmallText();
    }

    public void setBold() {
        util.setBold();
    }

    public void setUnderline() {
        util.setUnderline();
    }

    public void setDeleteLine() {
        util.setDeleteLine();
    }

    public void setTall() {
        util.setTall();
    }

    public void setWide() {
        util.setWide();
    }

    public void setWideBold() {
        util.setWideBold();
    }

    public void setTallBold() {
        util.setTallBold();
    }

    public void setWideTall() {
        util.setWideTall();
    }

    public void setWideTallBold() {
        util.setWideTallBold();
    }

    //----------------------------------------------------------------------------------------------
    // INTERFACES
    //----------------------------------------------------------------------------------------------

    public interface OnConnected {
        void onConnected(SmartPrinter smartPrinter) throws JSONException;
    }

    public interface OnFailed {
        void onFailed(String message);
    }

    public interface OnConnectPrinter {
        void onConnectPrinter(String printerName);
    }

    public interface Callback {
        void printama(SmartPrinter smartPrinter);
    }


}
