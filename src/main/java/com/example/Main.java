package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.example.api.ElpriserAPI.Prisklass.*;

public class Main {
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("hh");
    private static final int TILL_ORE = 100;

    public static void main(String[] args) {

        String zone = helpOrZone(args);

        while (zone != null) {

            ElpriserAPI elpriserAPI = new ElpriserAPI();
            ElpriserAPI.Prisklass prisklass;
            LocalDate date = dateSetter(args);

            try {
                prisklass = valueOf(zone);
            } catch (IllegalArgumentException e) {
                System.out.println("Ogiltig zon. Vald zon måste vara en av de följande: SE1|SE2|SE3|SE4.");
                break;
            }

            List<Elpris> prislista = pricelistSetter(date, prisklass, elpriserAPI);

            System.out.println("Priser för " + date + " i prisområde " + prisklass + ":");
            for (Elpris elpris : prislista) {
                System.out.println("Tid: " + elpris.timeStart() + ". Pris: " + elpris.sekPerKWh());
            }

            System.out.println();

            printMinMax(prislista);

            break;
        }

    }



    public static String helpOrZone(String[] args) {
        if (args.length == 0 || contains(args, "--help")) {
            printHelpMenu();
            return null;
        }

        String zoneArg = valueAfter(args, "--zone");
        if (zoneArg == null) {
            System.out.println("Zone is required. You must provide a --zone SE1|SE2|SE3|SE4");
            return null;
        } else {
            return zoneArg;
        }
    }

    public static LocalDate dateSetter(String[] args){
        if(contains(args, "--date") && valueAfter(args, "--date") != null){
            String inputDate = valueAfter(args, "--date");
            if(dateValidator(args, inputDate)){
                return LocalDate.parse(inputDate);
            }
        }
        return LocalDate.now();
    }

    public static boolean dateValidator(String[] args, String date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try{
            LocalDate.parse(date, formatter);
            return true;
        } catch (DateTimeParseException e){
            System.out.println("Invalid date format.");
            return false;
        }
    }

    public static boolean isTimeAfterOne(LocalDate date){
        if(date.equals(LocalDate.now())){
            LocalTime currentTime = LocalTime.now();
            if(currentTime.isBefore(LocalTime.parse("13:00"))){
                return false;
            }
        }
        return true;
    }


    public static List<Elpris> pricelistSetter(LocalDate date, ElpriserAPI.Prisklass priceclass, ElpriserAPI elpriserAPI){
        List<Elpris> pricelist = elpriserAPI.getPriser(date, priceclass);
        if(isTimeAfterOne(date)) {
            List<Elpris> pricelist2 = elpriserAPI.getPriser(date.plusDays(1), priceclass);
            pricelist.addAll(pricelist2);
        }
        return pricelist;
    }

    public static void sortPrices(List<Elpris> listOfPrices){
        Comparator<Elpris> compare = Comparator.comparingDouble(Elpris::sekPerKWh).reversed();
        listOfPrices.sort(compare);
    }

    public static void printMinMax(List<Elpris> listOfPrices) {
        if(listOfPrices.isEmpty()){
            System.out.println("Fann ingen data för det valda datumet.");
            return;
        }
        sortPrices(listOfPrices);
        DecimalFormat df = new DecimalFormat("0.00");

        double mean = 0;
        for(int i = 0; i < listOfPrices.size(); i++) {
            mean += listOfPrices.get(i).sekPerKWh();
        }

        System.out.println("Lägsta pris: " + df.format(listOfPrices.getLast().sekPerKWh() * TILL_ORE) + " öre mellan klockan "
                + HOUR_FORMATTER.format(listOfPrices.getLast().timeStart()) + "-" + HOUR_FORMATTER.format(listOfPrices.getLast().timeEnd()));
        System.out.println("Högsta pris: " + df.format(listOfPrices.getFirst().sekPerKWh() * TILL_ORE) + " öre mellan klockan "
                + HOUR_FORMATTER.format(listOfPrices.getFirst().timeStart()) + "-" + Main.HOUR_FORMATTER.format(listOfPrices.getFirst().timeEnd()));
        System.out.println("Medelpris: " + df.format(mean / listOfPrices.size() * TILL_ORE) + " öre.");
    }


    public static void printHelpMenu() {
        System.out.printf("%n Följande promter är giltiga i detta program: %n" +
                "--zone SE1|SE2|SE3|SE4   ->   Obligatorisk. %n" +
                "--date YYYY-MM-DD        ->   Valfri, standardinställningen är dagens datum. %n" +
                "--sorted                 ->   Valfri, visar priser i fallande ordning. %n" +
                "--charging 2h|4h|8h      ->   Valfri, hittar optimalt tidsspann för laddning. %n" +
                "--help                   ->   Valfri, visar information om programmet. %n %n");
    }


    private static boolean contains(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }

    private static String valueAfter(String[] args, String key) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].trim().equalsIgnoreCase(key) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }

}
