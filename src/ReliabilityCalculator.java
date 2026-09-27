import java.io.*;
import java.nio.file.*;
import java.util.*;


public class ReliabilityCalculator {

    private int Q, N;
    private double tvMin, tvMax, tvDop;
    private double tpMin, tpMax, tpDop;
    private double pBas;

    private double m41, m51, m52;      // Оценочные элементы
    private double p24M, p25M;         // Метрики
    private double p12, k12;           // Критерий
    private double kFactor;            // Фактор

    public static void main(String[] args) {
        new ReliabilityCalculator().run(
            args.length > 0 ? args[0] : "input.txt",
            args.length > 1 ? args[1] : "output.txt"
        );
    }

    private void run(String inputFile, String outputFile) {
        try {
            readInput(inputFile);
            calculate();
            writeOutput(outputFile);
            System.out.println("OK → " + outputFile);
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    private void readInput(String filename) throws IOException {
        Map<String, Double> params = new HashMap<>();
        for (String line : Files.readAllLines(Paths.get(filename))) {
            line = line.split("#")[0].trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("=");
            if (parts.length == 2) {
                try {
                    params.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        Q = (int) req(params, "Q");
        N = (int) req(params, "N");
        tvMin = req(params, "Tv_min");
        tvMax = req(params, "Tv_max");
        tvDop = req(params, "Tv_dop");
        tpMin = req(params, "Tp_min");
        tpMax = req(params, "Tp_max");
        tpDop = req(params, "Tp_dop");
        pBas = req(params, "P_baz");
    }

    private double req(Map<String, Double> p, String key) {
        Double v = p.get(key);
        if (v == null) throw new IllegalArgumentException("Нет параметра: " + key);
        return v;
    }

    private void calculate() {
        // Оценочные элементы
        m41 = 1.0 - (double) Q / N;
        double tvMean = (tvMin + tvMax) / 2.0;
        m51 = (tvMean > tvDop) ? tvDop / tvMean : 1.0;
        double tpMean = (tpMin + tpMax) / 2.0;
        m52 = (tpMean <= tpDop) ? 1.0 : tpDop / tpMean;

        // Метрики (формула 3)
        p24M = m41;
        p25M = (m51 + m52) / 2.0;

        // Абсолютный показатель критерия (формула 4)
        p12 = p24M * 0.5 + p25M * 0.5;

        // Относительный показатель (формула 5)
        k12 = p12 / pBas;

        // Фактор надёжности (формула 6)
        kFactor = k12 * 1.0;

        // Округление
        m41 = round(m41); m51 = round(m51); m52 = round(m52);
        p24M = round(p24M); p25M = round(p25M);
        p12 = round(p12); k12 = round(k12); kFactor = round(kFactor);
    }

    private void writeOutput(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Q = %d (отказы)%n", Q));
        sb.append(String.format("N = %d (эксперименты)%n", N));
        sb.append(String.format("Tv_ср = %s с, Tvдоп = %s с%n",
                round((tvMin + tvMax) / 2.0), tvDop));
        sb.append(String.format("Tp_ср = %s с, Tpдоп = %s с%n",
                round((tpMin + tpMax) / 2.0), tpDop));
        sb.append(String.format("P_баз = %s%n%n", pBas));

        sb.append(String.format("m41 = %s (безотказность)%n", m41));
        sb.append(String.format("m51 = %s (время восстановления)%n", m51));
        sb.append(String.format("m52 = %s (время преобразования)%n", m52));
        sb.append(String.format("P_24^M = %s (метрика H04)%n", p24M));
        sb.append(String.format("P_25^M = %s (метрика H05)%n", p25M));
        sb.append(String.format("P_12 = %s (критерий H2, абс.)%n", p12));
        sb.append(String.format("K_12 = %s (критерий H2, отн.)%n", k12));
        sb.append(String.format("%nK_ф = %s (ФАКТОР НАДЁЖНОСТИ)%n", kFactor));

        Files.write(Paths.get(filename), sb.toString().getBytes());
    }

    private double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}