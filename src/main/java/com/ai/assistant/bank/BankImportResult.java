package com.ai.assistant.bank;

import java.util.List;

/** Rezultatul importului unui extras: câte s-au salvat și câte duplicate au fost ignorate. */
public record BankImportResult(int saved, int skippedDuplicates, List<BankTransaction> transactions) {
}
