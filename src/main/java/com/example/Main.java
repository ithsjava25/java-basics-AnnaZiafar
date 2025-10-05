package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.example.api.ElpriserAPI.Prisklass.*;

public class Main {
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH");
    private static final DateTimeFormatter HOUR_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DecimalFormat ORE_FORMATTER = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.of("sv", "SE")));
    private static final int TO_ORE = 100;
    private static final double TO_EURO = 0.0908;
    private static final int QUARTERS_IN_HOUR = 4;

    public static void main(String[] args) {
        Locale.setDefault(Locale.of("sv", "SE"));

        String zone = whichZone(args);
        if(zone == null){
            return;
        }

        ElpriserAPI elpriserAPI = new ElpriserAPI();
        ElpriserAPI.Prisklass prisklass = valueOf(zone);
        LocalDate date = dateSetter(args);

        List<Elpris> prislista = pricelistSetter(date, prisklass, elpriserAPI);

        if(contains(args, "--sorted")){
            printPrices(sortedPrices(prislista));
        } else {
            printPrices(prislista);
        }

        System.out.println();
        printMinMax(prislista);
        System.out.println();

        if(contains(args, "--charging") && valueAfter(args, "--charging") != null){
            String chargingTime = valueAfter(args, "--charging");

            if(!chargingTime.matches("^[248]h$")){
                System.out.println("Inkorrekt data. Ange --charging 2h|4h|8h");
            } else {
                printCheapestChargingHour(cheapestChargingHours(prislista, Integer.parseInt(chargingTime.replace("h", ""))));
            }
        }

    }

    private static String whichZone(String[] args) {
        if (args.length == 0 || contains(args, "--help")) {
            printHelpMenu();
            return null;
        }

        String zoneArg = valueAfter(args, "--zone");
        if (zoneArg == null) {
            System.out.println("Zon är obligatorisk. Var vänlig och välj en --zone SE1|SE2|SE3|SE4");
            return null;
        } else if(!zoneArg.matches("^SE[1-4]$")){
            System.out.println("Ogiltig zon. Vald zon måste vara en av de följande: SE1|SE2|SE3|SE4.");
            return null;
        }
        return zoneArg;
    }

    private static LocalDate dateSetter(String[] args){
        if(contains(args, "--date") && valueAfter(args, "--date") != null){
            String inputDate = valueAfter(args, "--date");
            if(dateValidator(args, inputDate)){
                return LocalDate.parse(inputDate);
            }
        }
        return LocalDate.now();
    }

    private static boolean dateValidator(String[] args, String date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try{
            LocalDate.parse(date, formatter);
            return true;
        } catch (DateTimeParseException e){
            System.out.println("Invalid date format.");
            return false;
        }
    }

    private static boolean isTimeAfterOne(LocalDate date){
        if(date.equals(LocalDate.now())){
            LocalTime currentTime = LocalTime.now();
            if(currentTime.isBefore(LocalTime.parse("13:00"))){
                return false;
            }
        }
        return true;
    }

    private static List<Elpris> calculateHourlyPrices(List<Elpris> listOfPrices){
        List<Elpris> hourlyAveragePrices = new ArrayList<>();
        int hours = listOfPrices.size() / QUARTERS_IN_HOUR;

        for(int i = 0; i < hours; i++){
            double sum = 0;
            int index = i * QUARTERS_IN_HOUR;

            for(int j = 0; j < QUARTERS_IN_HOUR; j++){
                sum += listOfPrices.get(index + j).sekPerKWh();
            }

            double meanPrice = sum / QUARTERS_IN_HOUR;
            ZonedDateTime timeStart = listOfPrices.get(index).timeStart().withMinute(0).withSecond(0);

            Elpris hourlyPrice = new Elpris(meanPrice, meanPrice * TO_EURO, 0, timeStart, timeStart.plusHours(1));
            hourlyAveragePrices.add(hourlyPrice);
        }
        return hourlyAveragePrices;
    }

    private static List<Elpris> pricelistSetter(LocalDate date, ElpriserAPI.Prisklass priceclass, ElpriserAPI elpriserAPI){
        List<Elpris> pricelist = elpriserAPI.getPriser(date, priceclass);
        if(isTimeAfterOne(date)) {
            List<Elpris> pricelist2 = elpriserAPI.getPriser(date.plusDays(1), priceclass);
            pricelist.addAll(pricelist2);
        }

        if(pricelist.size() > 24){
            return calculateHourlyPrices(pricelist);
        }
        return pricelist;
    }

    private static List<Elpris> sortedPrices(List<Elpris> listOfPrices){
        List<Elpris> sortedPrices = new ArrayList<>(listOfPrices);
        Comparator<Elpris> compare = Comparator.comparingDouble(Elpris::sekPerKWh).reversed();
        sortedPrices.sort(compare);
        return sortedPrices;
    }

    private static List<Elpris> cheapestChargingHours(List<Elpris> listOfPrices, int chargingTime){
        double currentCost = 0;
        int startIndex = 0;

        for(int i = 0; i < chargingTime; i++){
            currentCost += listOfPrices.get(i).sekPerKWh();
        }
        double minCost = currentCost;

        for(int i = 1; i <= listOfPrices.size() - chargingTime; i++){
            currentCost -= listOfPrices.get(i - 1).sekPerKWh();
            currentCost += listOfPrices.get(i + chargingTime - 1).sekPerKWh();

            if(currentCost < minCost){
                minCost = currentCost;
                startIndex = i;
            }
        }
        return listOfPrices.subList(startIndex, startIndex + chargingTime);
    }

    private static void printCheapestChargingHour(List<Elpris> cheapestChargingHour){
        System.out.println("Påbörja laddning kl " + HOUR_MINUTE_FORMATTER.format(cheapestChargingHour.getFirst().timeStart()));
        System.out.println("Medelpris för fönster: " + meanPrice(cheapestChargingHour) + " öre.");
    }

    private static void printPrices(List<Elpris> listOfPrices){
        System.out.printf("%-13s %-13s %s %n", "Datum: ", "Klockslag: ", "Pris: ");
        for(Elpris elpris : listOfPrices){
            LocalDate elprisDate = elpris.timeStart().toLocalDate();
            System.out.println(elprisDate + "    " + HOUR_FORMATTER.format(elpris.timeStart()) + "-" +
                    HOUR_FORMATTER.format(elpris.timeStart().plusHours(1)) + "         " +
                    ORE_FORMATTER.format(elpris.sekPerKWh() * TO_ORE) + " öre");
        }
    }

    private static String meanPrice(List<Elpris> listOfPrices){
        double mean = 0;
        for(int i = 0; i < listOfPrices.size(); i++) {
            mean += listOfPrices.get(i).sekPerKWh();
        }
        return ORE_FORMATTER.format(mean / listOfPrices.size() * TO_ORE);
    }

    private static void printMinMax(List<Elpris> listOfPrices) {
        if(listOfPrices.isEmpty()){
            System.out.println("Fann ingen data för det valda datumet.");
            return;
        }

        List<Elpris> sortedCopy = sortedPrices(listOfPrices);
        double lowestPrice = sortedCopy.getLast().sekPerKWh() * TO_ORE;
        double highestPrice = sortedCopy.getFirst().sekPerKWh() * TO_ORE;

        System.out.println("Lägsta pris: " + ORE_FORMATTER.format(lowestPrice) + " öre mellan klockan "
                + HOUR_FORMATTER.format(sortedCopy.getFirst().timeStart()) + "-" + Main.HOUR_FORMATTER.format(sortedCopy.getFirst().timeStart().plusHours(1)));
        System.out.println("Högsta pris: " + ORE_FORMATTER.format(highestPrice) + " öre mellan klockan "
                + HOUR_FORMATTER.format(sortedCopy.getLast().timeStart()) + "-" + HOUR_FORMATTER.format(sortedCopy.getLast().timeStart().plusHours(1)));
        System.out.println("Medelpris: " + meanPrice(listOfPrices) + " öre.");
    }


    private static void printHelpMenu() {
        System.out.printf("%nExpected Command-Line Arguments: %n" +
                "--zone SE1|SE2|SE3|SE4   ->   Required. %n" +
                "--date YYYY-MM-DD        ->   Optional, defaults to current date. %n" +
                "--sorted                 ->   Optional, to display prices in descending order. %n" +
                "--charging 2h|4h|8h      ->   Optional, to find optimal charging windows. %n" +
                "--help                   ->   Optional, to display usage information. %n %n");
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
