package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.example.api.ElpriserAPI.Prisklass.*;

public class Main {
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("hh");

    public static void main(String[] args) {

        Args test = parseArgs(args);

//        ElpriserAPI elpriserAPI = new ElpriserAPI();
//
//        LocalDate defaultDatum = LocalDate.now();
//        List<Elpris> prislista = elpriserAPI.getPriser(defaultDatum, SE3);
//
//        System.out.println("Priser för " + defaultDatum + " i prisområde " + SE3 + ":");
//        for (Elpris elpris : prislista) {
//            System.out.println("Tid: " + elpris.timeStart() + ". Pris: " + elpris.sekPerKWh());
//        }
//
//        System.out.println();
//
//        printMinMax(prislista);

    }


    public static void printMinMax(List<Elpris> prislista) {
        Comparator<Elpris> compare = Comparator.comparingDouble(Elpris::sekPerKWh);
        prislista.sort(compare);
        DecimalFormat df = new DecimalFormat("0.00");
        System.out.println("Lägsta pris: " + df.format(prislista.getFirst().sekPerKWh()) + " öre.");
        System.out.println("Högsta pris: " + df.format(prislista.getLast().sekPerKWh()) + " öre.");
    }

    /// /////////

    public static Args parseArgs(String[] args){
        if(args.length == 0 || contains(args, "--help")) {
            printHelpMenu();
            return null;
        }

        String zoneArg = valueAfter(args, "--zone");
        if(zoneArg == null) {
            System.out.println("You must provide a zone SE1|SE2|SE3|SE4");
            System.exit(0);
        } else {
            System.out.println("You have chosen: " + zoneArg);
        }

        Args zone = new Args(zoneArg);

        return testy;
    }


    public static void printHelpMenu() {
        System.out.println("Help");
    }

    public static class Args{
        public String input;
    }

    private static boolean contains(String[] args, String flag) {
        for(String arg : args){
            if(arg.equalsIgnoreCase(flag)){
                return true;
            }
        }
        return false;
    }

    private static String valueAfter(String[] args, String key) {
        for(int i = 0; i < args.length; i++) {
            if(args[i].trim().equalsIgnoreCase("--zone") && i + 1 < args.length){
                return args[i + 1];
            }
        }
        return null;
    }


}
