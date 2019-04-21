package codyAgent;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class Logger {
    private static Map<String, Map<LoggableValue, Integer>> logs_ = new HashMap<>();
    private static Map<String, Map<Integer, Integer>> prioLogs_ = new HashMap<>();

    public static void logPriority(@Nonnull String agentID, int epoch, int priority) {
        if (!prioLogs_.containsKey(agentID)) {
            prioLogs_.put(agentID, new HashMap<>());
        }

        prioLogs_.get(agentID).put(epoch, priority);
    }

    public static void exportPriosAsCSV(@Nonnull File file) {
        StringBuilder csvContent = new StringBuilder();
        List<String> agentIDs = prioLogs_.keySet().stream().sorted(Comparator.comparingInt(Integer::parseInt)).collect(Collectors.toList());

        agentIDs.forEach(id -> csvContent.append("A").append(id).append(";"));
        csvContent.deleteCharAt(csvContent.length()-1);

        prioLogs_.values().stream().flatMap(log -> log.keySet().stream()).distinct().sorted().forEach(
                epoch -> {
                    csvContent.append("\r\n");
                    agentIDs.forEach(id -> csvContent.append(prioLogs_.get(id).get(epoch)).append(";"));
                    csvContent.deleteCharAt(csvContent.length()-1);
                });

        try (Writer writer = new FileWriter(file)) {
            writer.write(csvContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        logs_.clear();
        System.out.println("Saved agent logs as " + file.getAbsolutePath());
    }

    public static void increment(@Nonnull String agentID, @Nonnull LoggableValue loggableValue) {
        get(agentID).put(loggableValue, logs_.get(agentID).get(loggableValue) + 1);
    }

    public static void set(@Nonnull String agentID, @Nonnull LoggableValue loggableValue, int value) {
        get(agentID).put(loggableValue, value);
    }

    public static void exportAsCSV(@Nonnull File file) {
        StringBuilder csvContent = new StringBuilder("agentID");
        Arrays.stream(LoggableValue.values()).forEach(loggableValue -> csvContent.append(";").append(loggableValue));

        logs_.keySet().stream().sorted(Comparator.comparingInt(Integer::parseInt)).forEach(agentID -> {
            csvContent.append("\r\n").append(agentID);
            Arrays.stream(LoggableValue.values()).forEach(
                    loggableValue -> csvContent.append(";").append(get(agentID).get(loggableValue)));
        });

        try (Writer writer = new FileWriter(file)) {
            writer.write(csvContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        logs_.clear();
        System.out.println("Saved agent logs as " + file.getAbsolutePath());
    }

    private static @Nonnull
    Map<LoggableValue, Integer> get(@Nonnull String agentID) {
        if (!logs_.containsKey(agentID)) {
            Map<LoggableValue, Integer> logs = new HashMap<>();
            Arrays.stream(LoggableValue.values()).forEach(loggableValue -> logs.put(loggableValue, 0));
            logs_.put(agentID, logs);
        }

        return logs_.get(agentID);
    }
}
