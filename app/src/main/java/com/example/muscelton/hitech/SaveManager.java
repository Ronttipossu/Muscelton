package com.example.muscelton.hitech;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
 * Luokka hoitaa kaiken tietokannan datan lataamisen ja tallentamisen csv-tiedostoihin (comma-separated values) formatoimalla muuttujat integereiksi.
 * Sisältää päivämäärän formatointiin käytettävän SimpleDateFormat tyypin
 * @author Elias Perttu
 */
public class SaveManager {

    public static final String historyFile = "muscelton_history.csv"; //data is appended through time
    public static final String saveFile = "muscelton_save.csv"; //data is appended through time
    public static SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy"); //date save format
    /**
    *data storage format:  csv
    * String startDate; int dayCount; int difficulty; int[] exercises; int[] repetitions
     * @param context konteksti, esim this
     * @return oliko tallennustiedosto olemassa
     */
    public static boolean loadAllData(Context context) {
        Log.d("lmao", "-------------------------- BEGIN LOAD" );
        String saveData = readFile(context, saveFile); //daily
        String historyData = readFile(context, historyFile);
        if(saveData == null) { Log.d("lmao", "No save file."); return false; }
        if(historyData == null) { Log.d("lmao", "No history file."); return true; }
        String[] prefs = saveData.split(";");
        if(prefs.length != 5) {  Log.d("lmao", "Save file corrupt."); return true; }
        //Parse
        String[] exerciseStrings = prefs[3].split(",");
        String[] repetitionsStrings = prefs[4].split(",");
        Exercise[] exercises = new Exercise[exerciseStrings.length];
        int[] repetitions = new int[repetitionsStrings.length];
        for (int i = 0; i < exerciseStrings.length; i++)
            exercises[i] = Exercise.values()[Integer.valueOf(exerciseStrings[i])]; //String -> Exercise
        for (int i = 0; i < ExerciseData.count; i++)
            repetitions[i] = Integer.valueOf(repetitionsStrings[i]); //String -> int
        ArrayList<int[]> repetitionHistory = new ArrayList<>();
        String[] sets = historyData.split(";");
        for (String set : sets) {
            String[] values = set.split(",");
            int[] r = new int[ExerciseData.count];
            for (int i = 0; i < r.length; i++)
                r[i] = Integer.valueOf(values[i]);
            repetitionHistory.add(r);
        }
        //Set values
        Global g = Global.getInstance();
        g.setDifficulty(Integer.valueOf(prefs[1]));
        g.setExercises(exercises);
        g.setRepetitions(repetitions);
        g.setRepetitionHistory(repetitionHistory);
        long daysPassed = g.validateLoad(prefs[0], Integer.valueOf(prefs[1]));

        Log.d("lmao", "Loaded data: " + g.toString() );
        Log.d("lmao", "Validated change of " + String.valueOf(daysPassed) + " days.");
        return true;
    }

    /**
     * Formatoi ja kirjoittaa tallennustiedostot.
     * @param context konteksti, esim this
     */
    public static void saveAllData(Context context) {
        Log.d("lmao", "-------------------------- BEGIN SAVE" );
        Global g = Global.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append(g.getStartDate()).append(";");
        sb.append(String.valueOf(g.getDayCount())).append(";");
        sb.append(g.getDifficulty()).append(";");
        for (Exercise e : g.getExercises())
            sb.append(e.ordinal()).append(",");
        sb.setLength(sb.length() - 1); //useless comma
        sb.append(";");
        for(int rep: g.getRepetitions())
            sb.append(Integer.valueOf(rep)).append(",");
        sb.setLength(sb.length() - 1); //useless comma

        writeFile(context, saveFile, sb.toString(), false);
        Log.d("lmao", "Saved savefile: " + sb.toString() );

        sb = new StringBuilder();
        for (int[] reps : g.getRepetitionHistory()) {
            for(int rep: reps)
                sb.append(rep).append(",");
            sb.setLength(sb.length() - 1);
            sb.append(";");
        }
        sb.setLength(sb.length() - 1); //useless semicolon
        writeFile(context, historyFile, sb.toString(), false);
        Log.d("lmao", "Saved history of " + (g.getDayCount() + 1) + " days, " + (g.getDayCount() - g.getDayCountPrevious()) + " day change." );
    }

    /**
     * lukee tiedoston
     * @param context konteksti, esim this
     * @param fileName, tiedostonimi
     * @return tiedosto
     */
    public static String readFile(Context context, String fileName) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            file.createNewFile();
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line = reader.readLine(); //meidän tapauksessa luetaan vain yksi rivi
            if (line == null) return null; //ei dataa
            sb.append(line);
            reader.close();
            inputStream.close();
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Get rekt son: " + e.toString()); //for debug
        }
    }

    /**
     * kirjotittaa tiedostoon
     * @param context konteksti, esim this
     * @param fileName tiedostonimi
     * @param data formatoitu tieto
     * @param append lisätäänkö tiedostoon, vai overwrite
     */
    public static void writeFile(Context context, String fileName, String data, boolean append) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    context.openFileOutput(fileName, append ? Context.MODE_APPEND : Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Tiedoston " + fileName + " kirjoittaminen epäonnistui. Virhe: " + e.getMessage()); //for debug
        }
        Log.d("lmao", "Wrote to file: " + (new File(context.getFilesDir(), fileName).getAbsolutePath()));
    }

}
