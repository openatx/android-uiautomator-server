package com.github.uiautomator;

import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;

import com.github.uiautomator.compat.WindowManagerWrapper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Console {
    final static String PROCESS_NAME = "apkagent.cli";
    final static String VERSION = "1.0.0";

    private static void setArgV0(String text) {
        try {
            Method setter = android.os.Process.class.getMethod("setArgV0", String.class);
            setter.invoke(android.os.Process.class, text);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        setArgV0(PROCESS_NAME);

        Options options = new Options();
        options.addOption("v", "version", false, "show current version");
        options.addOption("h", "help", false, "show this message");
        options.addOption("d", "debug-info", false, "show debug info");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp(PROCESS_NAME, options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("help")) {
            formatter.printHelp(PROCESS_NAME, options);
            return;
        }
        if (cmd.hasOption("version")) {
            System.out.println(VERSION);
            return;
        }
        if (cmd.hasOption("debug-info")) {
            System.out.println("Debug info is not ready yet.");
            return;
        }

        new Console().listenAndServe();
    }

    private void listenAndServe(){
        Looper.prepare();
        Handler handler = new Handler();

        WindowManagerWrapper wm = new WindowManagerWrapper();
        Point size = wm.getDisplaySize();
        if (size == null){
            System.err.println("Can not get device resolution");
            System.exit(1);
        }

        RotationAgent rotation = new RotationAgent("rotationagent");
        rotation.start();

        Looper.loop();
    }
}
