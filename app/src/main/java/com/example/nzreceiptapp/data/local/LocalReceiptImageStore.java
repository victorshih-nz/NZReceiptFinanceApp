package com.example.nzreceiptapp.data.local;

import android.content.Context;
import android.net.Uri;

import com.example.nzreceiptapp.domain.service.IReceiptImageStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Copies camera/gallery input into app-private persistent storage. */
public final class LocalReceiptImageStore implements IReceiptImageStore {
    private final Context context;

    public LocalReceiptImageStore(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String persist(String sourceUri) {
        File directory = new File(context.getFilesDir(), "receipt_images");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Unable to create receipt image directory");
        }
        File target = new File(directory, UUID.randomUUID() + ".jpg");
        try (InputStream input = context.getContentResolver()
                .openInputStream(Uri.parse(sourceUri));
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) throw new IOException("Unable to open selected image");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return Uri.fromFile(target).toString();
        } catch (IOException exception) {
            if (target.exists()) target.delete();
            throw new IllegalStateException("Unable to save receipt image", exception);
        }
    }

    @Override
    public void delete(String storedUri) {
        if (storedUri == null) return;
        Uri uri = Uri.parse(storedUri);
        if (!"file".equals(uri.getScheme())) return;
        File file = new File(uri.getPath());
        File receiptDirectory = new File(context.getFilesDir(), "receipt_images");
        if (file.getParentFile() != null && file.getParentFile().equals(receiptDirectory)) {
            file.delete();
        }
    }
}
