package com.wipro.bankstatement.service;

import com.wipro.bankstatement.bean.TransactionBean;
import com.wipro.bankstatement.util.InvalidTransactionException;
import com.wipro.bankstatement.util.StatementParser;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Coordinates parsing (with per-line exception handling) and
 * Stream-based aggregation of bank statement transactions.
 */
public class StatementAnalyzerService {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH);

    // Collects malformed line errors encountered during the last parseStatement() call
    private final List<String> parseErrors = new ArrayList<>();

    /**
     * Parses raw statement lines into TransactionBean objects.
     * Malformed lines throw InvalidTransactionException, which is caught
     * per-line so that processing continues for the remaining lines.
     *
     * @param lines raw statement lines
     * @return list of successfully parsed transactions
     */
    public List<TransactionBean> parseStatement(List<String> lines) {
        parseErrors.clear();
        List<TransactionBean> transactions = new ArrayList<>();

        for (String line : lines) {
            try {
                TransactionBean txn = StatementParser.parseLine(line);
                transactions.add(txn);
            } catch (InvalidTransactionException e) {
                // Log and continue - do not crash the program
                parseErrors.add(e.toString());
                System.err.println("Skipping malformed line -> " + e);
            }
        }
        return transactions;
    }

    /**
     * @return the errors collected from the most recent parseStatement() call
     */
    public List<String> getParseErrors() {
        return parseErrors;
    }

    /**
     * Computes total credits from the given transactions using Streams.
     */
    public double getTotalCredits(List<TransactionBean> txns) {
        return txns.stream()
                .filter(t -> "CREDIT".equalsIgnoreCase(t.getType()))
                .mapToDouble(TransactionBean::getAmount)
                .sum();
    }

    /**
     * Computes total debits from the given transactions using Streams.
     */
    public double getTotalDebits(List<TransactionBean> txns) {
        return txns.stream()
                .filter(t -> "DEBIT".equalsIgnoreCase(t.getType()))
                .mapToDouble(TransactionBean::getAmount)
                .sum();
    }

    /**
     * Groups transactions by month (yyyy-MM) and sums amounts per month.
     * Debits are treated as negative contributions, credits as positive,
     * so the value represents net movement for that month.
     */
    public Map<String, Double> getMonthlyTotals(List<TransactionBean> txns) {
        Map<String, Double> monthly = txns.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDate().format(MONTH_FORMATTER),
                        TreeMap::new,
                        Collectors.summingDouble(t ->
                                "CREDIT".equalsIgnoreCase(t.getType()) ? t.getAmount() : -t.getAmount())
                ));
        return monthly;
    }

    /**
     * Groups transactions by a category derived from keywords found in the
     * description, and sums the amount per category.
     */
    public Map<String, Double> getCategoryTotals(List<TransactionBean> txns) {
        return txns.stream()
                .collect(Collectors.groupingBy(
                        this::categorize,
                        LinkedHashMap::new,
                        Collectors.summingDouble(TransactionBean::getAmount)
                ));
    }

    /**
     * Simple keyword-based categorization of a transaction's description.
     */
    private String categorize(TransactionBean txn) {
        String desc = txn.getDescription().toLowerCase(Locale.ENGLISH);

        if (desc.contains("grocery") || desc.contains("supermarket") || desc.contains("mart")) {
            return "Groceries";
        } else if (desc.contains("salary") || desc.contains("payroll")) {
            return "Salary";
        } else if (desc.contains("rent")) {
            return "Rent";
        } else if (desc.contains("electricity") || desc.contains("water bill")
                || desc.contains("utility") || desc.contains("gas bill")) {
            return "Utilities";
        } else if (desc.contains("amazon") || desc.contains("shopping") || desc.contains("mall")) {
            return "Shopping";
        } else if (desc.contains("movie") || desc.contains("netflix")
                || desc.contains("entertainment") || desc.contains("cinema")) {
            return "Entertainment";
        } else if (desc.contains("uber") || desc.contains("fuel")
                || desc.contains("petrol") || desc.contains("transport")) {
            return "Transport";
        } else if (desc.contains("restaurant") || desc.contains("cafe")
                || desc.contains("food") || desc.contains("dining")) {
            return "Dining";
        } else if (desc.contains("hospital") || desc.contains("pharmacy")
                || desc.contains("medical") || desc.contains("clinic")) {
            return "Healthcare";
        } else {
            return "Others";
        }
    }

    /**
     * Prints a formatted summary report to the console.
     */
    public void printSummaryReport(List<TransactionBean> txns) {
        double totalCredits = getTotalCredits(txns);
        double totalDebits = getTotalDebits(txns);
        Map<String, Double> monthlyTotals = getMonthlyTotals(txns);
        Map<String, Double> categoryTotals = getCategoryTotals(txns);

        System.out.println("\n===================================================");
        System.out.println("            BANK STATEMENT SUMMARY REPORT          ");
        System.out.println("===================================================");
        System.out.printf("Total Transactions Parsed : %d%n", txns.size());
        System.out.printf("Total Credits             : %.2f%n", totalCredits);
        System.out.printf("Total Debits              : %.2f%n", totalDebits);
        System.out.printf("Net Balance               : %.2f%n", (totalCredits - totalDebits));

        System.out.println("\n--------- Monthly Totals (Net: Credit - Debit) ---------");
        if (monthlyTotals.isEmpty()) {
            System.out.println("No data available.");
        } else {
            for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
                System.out.printf("%-10s : %.2f%n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("\n--------- Category-wise Totals ---------");
        if (categoryTotals.isEmpty()) {
            System.out.println("No data available.");
        } else {
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                System.out.printf("%-15s : %.2f%n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("\n--------- Parse Errors ---------");
        if (parseErrors.isEmpty()) {
            System.out.println("No malformed lines encountered.");
        } else {
            System.out.printf("%d malformed line(s) skipped:%n", parseErrors.size());
            for (String err : parseErrors) {
                System.out.println(" - " + err);
            }
        }
        System.out.println("===================================================\n");
    }

    public static void main(String[] args) {
        // 10 valid lines + 2 malformed lines (per test case requirements)
        List<String> rawLines = List.of(
                "2026-05-12|Grocery Store|1500.00|DEBIT",
                "2026-05-15|Monthly Salary|55000.00|CREDIT",
                "2026-05-18|Netflix Subscription|649.00|DEBIT",
                "2026-05-20|House Rent|18000.00|DEBIT",
                "2026-05-22|Electricity Bill|2200.50|DEBIT",
                "2026-06-01|Amazon Shopping|3499.00|DEBIT",
                "2026-06-05|Uber Ride|350.75|DEBIT",
                "2026-06-10|Freelance Payment|12000.00|CREDIT",
                "2026-06-15|Restaurant Dinner|1200.00|DEBIT",
                "2026-06-20|Pharmacy Purchase|560.00|DEBIT",
                "2026-05-12,Grocery,1500,DEBIT",              // malformed: wrong delimiter (comma)
                "2026-07-01|Water Bill|ABC|DEBIT"              // malformed: non-numeric amount
        );

        StatementAnalyzerService service = new StatementAnalyzerService();

        List<TransactionBean> transactions = service.parseStatement(rawLines);

        System.out.println("Parsed " + transactions.size() + " valid transaction(s) out of "
                + rawLines.size() + " line(s). " + service.getParseErrors().size()
                + " error(s) logged.");

        service.printSummaryReport(transactions);
    }
}
