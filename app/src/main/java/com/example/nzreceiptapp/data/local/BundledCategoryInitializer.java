package com.example.nzreceiptapp.data.local;

import android.content.Context;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;
import com.example.nzreceiptapp.domain.service.ICategoryInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Loads the bundled category TSV once, before the first receipt is parsed. */
public final class BundledCategoryInitializer implements ICategoryInitializer {
    private static final String ASSET_NAME = "category_rules.tsv";

    private final Context context;
    private final ICategoryRepository repository;
    private boolean initialized;

    public BundledCategoryInitializer(Context context, ICategoryRepository repository) {
        this.context = context.getApplicationContext();
        this.repository = repository;
    }

    @Override
    public synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        // The seeder is idempotent: existing categories are reused and keyword
        // rules are replaced. Running it once per app process also picks up new
        // rules added in a later app version.
        seedFromText(readAsset(), repository);
        initialized = true;
    }

    private static void seedFromText(String content, ICategoryRepository repository) {
        String[] lines = content.split("\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            String[] parts = trimmedLine.split("\\t");
            if (parts.length < 3) {
                continue;
            }

            String keyword = parts[0].trim().toLowerCase();
            String parentName = parts[1].trim();
            String subCategoryName = parts[2].trim();

            Category parent = repository.findCategoryByName(parentName, null);
            if (parent == null) {
                parent = new Category(UUID.randomUUID().toString(), parentName, null);
                repository.saveCategory(parent);
            }

            Category subCategory = repository.findCategoryByName(subCategoryName, parentName);
            if (subCategory == null) {
                subCategory = new Category(
                        UUID.randomUUID().toString(), subCategoryName, parent);
                repository.saveCategory(subCategory);
            }

            repository.saveRule(keyword, subCategory.getId());
        }
    }

    private String readAsset() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(ASSET_NAME), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            return content.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load bundled category rules", exception);
        }
    }
}
