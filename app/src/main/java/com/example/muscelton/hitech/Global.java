package com.example.muscelton.hitech;

import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Singleton "tietokanta", joka sisältää arvot kaikesta ohjelmassa käytettävästä vähäisestä datasta.
 * - Getterit ja setterit datalle
 * - Harjoitusten uusimiseen käytetyt algoritmit
 * - Ladatun tiedon validiointi
 * - Sample datan generointi algoritmi
 * @author Elias Perttu
 */
public class Global {

    public final Random rng;
    private int difficulty; //current
    private Exercise[] exercises; //current
    private int[] repetitions; //current
    private ArrayList<int[]> repetitionHistory; //Sisältää jokaiselle päivälle (ohjelman käynnistämisestä) listan kunkin harjoituksen toistoista

    private String startDate;
    private long dayCount; //equals repetitionHistory.size();
    private long dayCountPrevious; //on the last opening of app

    private static final Global singleton = new Global(); //luokka luodaan ohjelman kännistyessä

    /**
     * Konstruktorissa asetetaan vain tietokannan default arvot siltä varalta, että tallennustiedostoa ei vielä ole olemassa
     */
    private Global() {
        this.rng = new Random(1337);
        this.difficulty = 0;
        this.exercises = new Exercise[5];
        this.renewExercises();
        this.repetitions = new int[ExerciseData.count];
        initializeHistory();
    }

    //Pääsy tähän luokkaan
    public static Global getInstance() {
        return singleton;
    }


    public int getDifficulty() { return this.difficulty; }
    public Exercise[] getExercises() { return this.exercises; }
    public int[] getRepetitions() { return this.repetitions; }
    public ArrayList<int[]> getRepetitionHistory() {
        return this.repetitionHistory;
    }
    public long getDayCount() {return this.dayCount; }
    public String getStartDate() {return this.startDate; }
    public long getDayCountPrevious() {return this.dayCountPrevious; }

    //set on startup.
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public void setExercises(Exercise[] exercises) {
        this.exercises = exercises;
    }

    public void setRepetitions(int[] repetitions) {
        this.repetitions = repetitions;
    }

    public void setRepetitionHistory(ArrayList<int[]> rh) {
        this.repetitionHistory = rh;
    }
    /**
     * Sufflaa uudet harjoitukset. Koska haluamme käyttää Elmon suositusta, Shufflea, pitää tehdä pari tyyppimuunnosta :(
     * @return lista harjoituksista
     */
    public Exercise[] renewExercises() {
        List<Exercise> newExercises = Arrays.asList(getExercisesOfDifficulty()); //Exercise[] to List<Exercise>
        Collections.shuffle(newExercises);  //SHUFFLE!
        for(int i = 0; i < this.exercises.length; i++)
            exercises[i] = newExercises.get(i);
        Log.d("lmao", exercises[0].ordinal() + ", " + exercises[1].ordinal() + ", " + exercises[2].ordinal() + ", " + exercises[3].ordinal() + ", " + exercises[4].ordinal());
        return this.exercises; //List<Exercise> to Exercise[]
    }
    /**
     * Arpoo yhden uuden harjoituksen.
     * @param index halutun harjoituksen sijainti näkymässä
     * @retun uusi harjoitus
     */
    public Exercise renewExercise(int index) {
        Exercise[] difSet = getExercisesOfDifficulty();
        int newIndex = rng.nextInt(difSet.length -1 ); //arvotaan kaikista lukuunottamatta viimeistä, koska:
        if(this.exercises[index] == difSet[newIndex])  //jos harjoitus on sama kuin edellinen, silloin valitaan viimeinen
            newIndex = difSet.length -1 ;
        this.exercises[index] = difSet[newIndex];
        return this.exercises[index];
    }
    /**
     * Palauttaa listan mahdollisista harjoituksista tämänhetkisen vaikeustason perusteella
     * @return lista harjoitusksista
     */
    private Exercise[] getExercisesOfDifficulty() {
        return this.difficulty == 0 ? ExerciseData.easy : this.difficulty == 1 ? ExerciseData.medium : ExerciseData.hard;
    }
    /**
     * Nollaa historian, ja asettaa täten aloituspäivämäärn nykyhetkeksi
     */
    public void initializeHistory() {
        dayCount = 0;
        dayCountPrevious = 0;
        this.repetitionHistory = new ArrayList<>();
        this.startDate = SaveManager.sdf.format(new Date(System.currentTimeMillis()));

    }
    /**
     * Tarkistaa ladatun datan
     * mikäli päivä viimetallennuksesta on vaihtunut, historiaan lisätään aiemmat harjoitukset
     * lisää tyhjien, tallentamattomien päivien historian tietokantaan tyhjänä
     * @return viimelatauksesta kuluneiden päivien määrä
     */
    public long validateLoad(String startDate, int dayCountPrevious){
       try {
           this.startDate = startDate;
           this.dayCountPrevious = dayCountPrevious;
           Date start = SaveManager.sdf.parse(startDate);
           Date now = new Date(System.currentTimeMillis());
           this.dayCount = TimeUnit.DAYS.convert(now.getTime() - start.getTime(), TimeUnit.MILLISECONDS);
           long daysPassed =  this.dayCount - this.dayCountPrevious;
           for(long i = 0; i < daysPassed - 1; i++) //Here we add all the gap days to history with zero reps.
               this.repetitionHistory.add(new int[ExerciseData.count]);
           if(daysPassed > 0) {
               this.repetitionHistory.add(this.repetitions); //save reps and star over
               this.repetitions = new int[ExerciseData.count];
           }
           return daysPassed;
       } catch (Exception e) {
           Log.d("lmao", "Date parse exception (save data corrupted)");
           return 0;
        }
    }
    /**
     * Kirjoittaa tietokantaan esimerkkidataa, joka kuvastaa oikeita käyttäjätyyppejä.
     * Huom! Algoritmi kirjoittaa käyttäjän tiedon päälle!
     */
    public void overwriteRandomData() {
        int historyDays = rng.nextInt(3) > 0 ? 1 + rng.nextInt(7)  :  10 + rng.nextInt(40);

        Calendar c = Calendar.getInstance();
        try {
            c.setTime((SaveManager.sdf.parse(startDate)));
            c.add(Calendar.DATE, (int)(dayCount - historyDays));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        startDate = SaveManager.sdf.format(c.getTime());
        dayCount = historyDays;
        dayCountPrevious = 0;
        repetitionHistory = new ArrayList<>();

        int dedication = rng.nextInt(5);
        for(int i = 0; i < historyDays + 1; i++) {
            int[] randomReps = new int[ExerciseData.count];
            if(rng.nextInt( 2 + dedication) != 0)  //empty days
                for(int j = 0; j < randomReps.length; j++)
                    randomReps[j] = rng.nextInt(8) > 0 ? 0 :
                            rng.nextInt(5) == 0 ? 5 + rng.nextInt(5 + 5*dedication) : 5 + rng.nextInt(1 + 20 * dedication);
            if(i < historyDays)
                repetitionHistory.add(randomReps);
            else
                repetitions = randomReps;
        }
    }

    @Override
    public String toString() {
        String exercisesString = "Exercises: ";
        for(Exercise e: exercises)
            exercisesString += e.name() + ", ";
        return "\nDifficulty: " + this.difficulty
                + "\n" + exercisesString
                + "\nRepetitions: " + Arrays.toString(repetitions)
                + "\nHistory days count: " + repetitionHistory.size();
    }

}

