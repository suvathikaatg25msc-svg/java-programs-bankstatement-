package com.wipro.bankstatement.util;

import com.wipro.bankstatement.bean.TransactionBean;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses regex to parse and validate a single raw statement line.
 * Expected format: DATE|DESCRIPTION|AMOUNT|TYPE
 * e.g. 2026-05-12|Grocery Store|1500.00|DEBIT
 */
public class StatementParser {

    // Group 1: date (yyyy-MM-dd)
    // Group 2: description (any chars except the pipe delimiter)
    // Group 3: amount (integer or decimal, up to 2 decimal places)
    // Group 4: type (DEBIT or CREDIT, case-insensitive)
    private static final String LINE_REGEX =
            "^(\\d{4}-\\d{2}-\\d{2})\\|([^|]+)\\|(\\d+(?:\\.\\d{1,2})?)\\|(DEBIT|CREDIT)$";

    private static final Pattern LINE_PATTERN =
            Pattern.compile(LINE_REGEX, Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private StatementParser() {
        // utility class, no instantiation
    }

    /**
     * Parses and validates a single raw statement line using regex.
     *
     * @param line raw line from the bank statement
     * @return a populated TransactionBean
     * @throws InvalidTransactionException if the line does not match the expected format
     */
    public static TransactionBean parseLine(String line) throws InvalidTransactionException {
        if (line == null || line.trim().isEmpty()) {
            throw new InvalidTransactionException("Line is null or empty", line);
        }

        Matcher matcher = LINE_PATTERN.matcher(line.trim());

        if (!matcher.matches()) {
            throw new InvalidTransactionException(
                    "Line does not match expected format DATE|DESCRIPTION|AMOUNT|TYPE", line);
        }

        String rawDate = matcher.group(1);
        String description = matcher.group(2).trim();
        String rawAmount = matcher.group(3);
        String type = matcher.group(4).toUpperCase();

        LocalDate date;
        try {
            date = LocalDate.parse(rawDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidTransactionException("Invalid date value: " + rawDate, line);
        }

        double amount;
        try {
            amount = Double.parseDouble(rawAmount);
        } catch (NumberFormatException e) {
            throw new InvalidTransactionException("Invalid amount value: " + rawAmount, line);
        }

        if (amount <= 0) {
            throw new InvalidTransactionException("Amount must be positive: " + rawAmount, line);
        }

        return new TransactionBean(date, description, amount, type);
    }
}
