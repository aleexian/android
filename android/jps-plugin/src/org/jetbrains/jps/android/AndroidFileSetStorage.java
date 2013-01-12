package org.jetbrains.jps.android;

import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.storage.StorageProvider;
import org.jetbrains.jps.incremental.storage.AbstractStateStorage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidFileSetStorage extends AbstractStateStorage<String, AndroidFileSetState> {

  private AndroidFileSetStorage(@NotNull File dataStorageRoot, @NotNull String storageName) throws IOException {
    super(AndroidJpsUtil.getStorageFile(dataStorageRoot, storageName), new EnumeratorStringDescriptor(), new MyDataExternalizer());
  }

  private static class MyDataExternalizer implements DataExternalizer<AndroidFileSetState> {

    @Override
    public void save(DataOutput out, AndroidFileSetState value) throws IOException {
      value.save(out);
    }

    @Override
    public AndroidFileSetState read(DataInput in) throws IOException {
      return new AndroidFileSetState(in);
    }
  }

  public static class Provider extends StorageProvider<AndroidFileSetStorage> {

    private final String myStorageName;

    public Provider(@NotNull String storageName) {
      myStorageName = storageName;
    }

    @NotNull
    @Override
    public AndroidFileSetStorage createStorage(File targetDataDir) throws IOException {
      return new AndroidFileSetStorage(targetDataDir, myStorageName);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Provider provider = (Provider)o;

      if (!myStorageName.equals(provider.myStorageName)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myStorageName.hashCode();
    }
  }
}
