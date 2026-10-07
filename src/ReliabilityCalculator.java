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

    Random random = new Random();

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

    private double[] generateUniform(int count, double min, double max) {
        double[] values = new double[count];
        for (int i = 0; i < count; i++) {
            values[i] = random.nextDouble(min, max);
        }
        return values;
    }

    private void calculate() {
        // Оценочные элементы
        m41 = 1.0 - (double) Q / N;

        Double tvMean = mean(generateUniform(100, tvMin, tvMax));
        m51 = (tvMean > tvDop) ? tvDop / tvMean : 1.0;

        double[] tpSample = generateUniform(200, tpMin, tpMax);
        double sum52 = 0.0;
        for (double tp : tpSample) {
            sum52 += (tp <= tpDop) ? 1.0 : tpDop / tp;
        }
        m52 = sum52 / 200;

        // Метрики (формула 3)
        p24M = m41;
        p25M = (m51 + m52) / 2.0;

        // Абсолютный показатель критерия (формула 4)
        p12 = p24M * 0.5 + p25M * 0.5;

        // Относительный показатель (формула 5)
        k12 = p12 / pBas;

        // Фактор надёжности (формула 6)
        kFactor = k12 * 1.0;
    }

    private double mean(double[] values) {
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private void writeOutput(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Q = %d (отказы)%n", Q));
        sb.append(String.format("N = %d (эксперименты)%n", N));
        sb.append(String.format("Tv: R[%s; %s], 100 значений, Tvдоп = %s с%n",
                tvMin, tvMax, tvDop));
        sb.append(String.format("Tp: R[%s; %s], 200 значений, Tpдоп = %s с%n",
                tpMin, tpMax, tpDop));
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
}