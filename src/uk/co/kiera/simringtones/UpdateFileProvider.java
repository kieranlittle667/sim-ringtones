package uk.co.kiera.simringtones;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;

/** Shares only the verified cached APK, read-only, with Android's installer. */
public final class UpdateFileProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private File file(Uri uri) {
        if(!"content".equals(uri.getScheme())||!(getContext().getPackageName()+".updates").equals(uri.getAuthority())
                ||!"/update.apk".equals(uri.getEncodedPath())||uri.getQuery()!=null||uri.getFragment()!=null) {
            throw new IllegalArgumentException("Unknown update URI");
        }
        return new File(getContext().getCacheDir(),"updates/update.apk");
    }
    @Override public String getType(Uri uri) { file(uri); return "application/vnd.android.package-archive"; }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order) {
        File file=file(uri);
        String[] columns=projection!=null?projection:new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE};
        Object[] values=new Object[columns.length];
        for(int i=0;i<columns.length;i++) {
            if(OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i]="sim-ringtones.apk";
            else if(OpenableColumns.SIZE.equals(columns[i])) values[i]=file.length();
            else throw new IllegalArgumentException("Unsupported column");
        }
        MatrixCursor cursor=new MatrixCursor(columns,1); cursor.addRow(values); return cursor;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode)) throw new FileNotFoundException("Updates are read-only");
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String selection,String[] args) { throw new UnsupportedOperationException(); }
}
