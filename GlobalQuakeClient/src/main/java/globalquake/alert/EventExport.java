package globalquake.alert;

import globalquake.core.earthquake.data.Earthquake;
import globalquake.core.earthquake.quality.QualityClass;
import globalquake.core.intensity.IntensityScales;
import globalquake.core.intensity.Level;
import globalquake.utils.GeoUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EventExport {

    // Exports on event tick event details into a csv file. After 100 records it removes the first one and appends a new one.
    private void exportEvent(Long eventTime, String eventRegion, double eventMagnitude, double eventMMI, String eventQuality, String mmi) {
        String file = "events.csv"; // filename
        Runnable task = () -> {
            try {
                // Read existing records
                List<String> records = new ArrayList<>();
                boolean fileExists = new File(file).exists();
                boolean isEmpty = !fileExists || new File(file).length() == 0;
                if (!fileExists) {
                    new File(file).createNewFile();
                }
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        records.add(line);
                    }
                }

                // Remove the first 20 records there are 100 or more records
                if (records.size() >= 100) {
                    for (int i = 1; i <= 20; i++) {
                        records.remove(i);
                    }
                }

                // Convert eventTime to hh:mm:ss format
                Instant instant = Instant.ofEpochMilli(eventTime);
                String formattedTime = DateTimeFormatter.ofPattern("HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(instant);

                // Add new record with formatted values, mag and mmi written up to 4 decimal places
                String newRecord = String.format("%s,%s,%s,%.4f,%.4f,%s", formattedTime, eventQuality, eventRegion, eventMagnitude, eventMMI, mmi);
                records.add(newRecord);

                // Write back to the file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    // Write headers if the file is empty
                    if (isEmpty) {
                        writer.write("EventTime,EventQuality,EventRegion,EventMagnitude,EventMMI,MMI");
                        writer.newLine();
                    }
                    for (String record : records) {
                        writer.write(record);
                        writer.newLine();
                    }
                }

                System.out.println("Event exported to " + file + " at " + formattedTime + " with region " + eventRegion + " and magnitude " + eventMagnitude + " and MMI " + mmi);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        new Thread(task).start(); // to not slow down the alert system
    }

    public static void exportEarthquakeEvent(Earthquake earthquake) {
        if(earthquake == null){
            return;
        }
        // Extracting all information for exporting
        // Earthquake quality
        QualityClass summaryQuality = earthquake.getHypocenter().quality.getSummary(); // getting quality of earthquake
        String earthquake_quality = summaryQuality.toString(); // getting the quality as a string
        if(earthquake_quality.equals("D")){
            return; // if earthquake is of low quality, do not export it
        }

        //Event time
        Long eventTime = earthquake.getCreatedAt(); // Time of the event

        //Event Region and Magnitude
        String eventRegion = earthquake.getRegion(); // Region of the earthquake
        double eventMagnitude = earthquake.getMag(); // Magnitude of the earthquake

        //calculating MMI, IDK if you can grab it directly from the earthquake object
        double eventMMi = 0.0;
        String mmi = "";
        try {
            eventMMi = GeoUtils.getMaxPGA(earthquake.getLat(), earthquake.getLon(), earthquake.getDepth(), earthquake.getMag());
            Level level = IntensityScales.getIntensityScale().getLevel(eventMMi);
            mmi = "%s%s".formatted(level.getName(), level.getSuffix());

        } catch (Exception e) {
            System.out.println("Error: Cant calculate MMI for exporting event at event time: " + eventTime);
        }
        // Call exportEvent with the extracted details
        EventExport export = new EventExport();
        export.exportEvent(eventTime, eventRegion, eventMagnitude, eventMMi, earthquake_quality, mmi);
    }
}