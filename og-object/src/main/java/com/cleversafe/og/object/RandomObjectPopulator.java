/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.object;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RandomObjectPopulator extends Thread implements ObjectManager {
  private static final Logger _logger = LoggerFactory.getLogger(RandomObjectPopulator.class);
  private static final int OBJECT_SIZE = LegacyObjectMetadata.OBJECT_SIZE;
  private static final int MAX_PERSIST_ARG = 30 * 1000 * 60;
  public static final int MAX_OBJECT_ARG = 100 * (1048576 / OBJECT_SIZE);
  private final int maxObjects;
  private final String directory;
  private final String prefix;
  public static final String SUFFIX = ".object";
  private final Pattern filenamePattern;

  // object read from a file
  private final RandomAccessConcurrentHashSet<ObjectMetadata> objects =
      new RandomAccessConcurrentHashSet<ObjectMetadata>();
  private final ReadWriteLock objectsLock = new ReentrantReadWriteLock(true);
  private final SortedMap<ObjectMetadata, Integer> currentlyReading = Collections
      .synchronizedSortedMap(new TreeMap<ObjectMetadata, Integer>());
  private final ReadWriteLock readingLock = new ReentrantReadWriteLock(true);
  private final ReadWriteLock persistLock = new ReentrantReadWriteLock(true);
  private final File saveFile;
  private volatile boolean testEnded = false;
  private final int idFileIndex;
  private final Random rand = new Random();
  private final UUID vaultId;
  private final ScheduledExecutorService saver;

  class IdFilter implements FilenameFilter {
    @Override
    public boolean accept(final File dir, final String name) {
      return RandomObjectPopulator.this.filenamePattern.matcher(name).matches();
    }
  }

  public static int getObjectSize() {
    return OBJECT_SIZE;
  }

  public RandomObjectPopulator(final UUID vaultId) {
    this(vaultId, "");
  }

  @Inject
  public RandomObjectPopulator(@Named("objectfile.location") final String directory,
      @Named("objectfile.name") final String prefix) {
    this(UUID.randomUUID(), directory, prefix, MAX_OBJECT_ARG, MAX_PERSIST_ARG);
  }

  public RandomObjectPopulator(final UUID vaultId, final String directory, final String prefix) {
    this(vaultId, directory, prefix, MAX_OBJECT_ARG, MAX_PERSIST_ARG);
  }

  public RandomObjectPopulator(final UUID vaultId, final String prefix) {
    this(vaultId, ".", prefix, MAX_OBJECT_ARG, MAX_PERSIST_ARG);
  }

  public RandomObjectPopulator(final UUID vaultId, final int maxObjects) {
    this(vaultId, ".", "", maxObjects, MAX_PERSIST_ARG);
  }

  public RandomObjectPopulator(final UUID vaultId, final String prefix, final int maxObjects) {
    this(vaultId, ".", prefix, maxObjects, MAX_PERSIST_ARG);
  }

  public RandomObjectPopulator(final UUID vaultId, final String directory, final String prefix,
      final int maxObjectCount, final long persistTime) {
    this.vaultId = checkNotNull(vaultId);
    this.directory = checkNotNull(directory);
    if (prefix != null && !prefix.isEmpty()) {
      this.prefix = prefix;
    } else {
      this.prefix = "id_";
    }
    this.filenamePattern =
        Pattern.compile(String.format("%s(\\d|[1-9]\\d*)%s", this.prefix,
            RandomObjectPopulator.SUFFIX));
    this.maxObjects = maxObjectCount;
    final File[] files = getIdFiles();
    if (files != null && files.length > 1) {
      this.idFileIndex = this.rand.nextInt(files.length - 1);
    } else {
      this.idFileIndex = 0;
    }
    this.saveFile = createFile(this.idFileIndex);

    loadObjects();

    this.saver = Executors.newScheduledThreadPool(1);
    this.saver.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          persistIds();
        }

        catch (final IOException e) {
          _logger.error("Can't store id file", e);
        }
      }
      // Every 30 minutes
    }, persistTime, persistTime, TimeUnit.MILLISECONDS);
  }

  private void loadObjects() {
    this.objects.clear();
    try {
      final byte[] objectBytes = new byte[OBJECT_SIZE];

      if (this.saveFile.exists()) {
        final InputStream input = new BufferedInputStream(new FileInputStream(this.saveFile));
        while (input.read(objectBytes) == OBJECT_SIZE) {
          final ObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes);
          this.objects.put(id);
        }
        input.close();
      }
    } catch (final Exception e) {
      this.testEnded = true;
      _logger.error("", e);
    }
  }

  private File[] getIdFiles() {
    final File dir = new File(this.directory);
    return dir.listFiles(new IdFilter());
  }

  @Override
  public long getSavedObjectCount() {
    long count = 0;
    final File[] idFiles = getIdFiles();
    for (final File file : idFiles) {
      count += file.length() / OBJECT_SIZE;
    }
    return count;
  }

  public long getCurrentObjectCount() {
    return this.objects.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.cleversafe.simpleobject.performance.ObjectManager#getIdForDelete()
   */
  @Override
  public ObjectMetadata getNameForDelete() {
    this.persistLock.readLock().lock();
    try {
      ObjectMetadata id = null;
      while (id == null) {
        this.objectsLock.writeLock().lock();
        id = this.objects.removeRandom();
        this.objectsLock.writeLock().unlock();
        checkForNull(id);
        boolean unavailable;
        this.readingLock.readLock().lock();
        unavailable = this.currentlyReading.containsKey(id);
        this.readingLock.readLock().unlock();
        if (unavailable) {
          this.objects.put(id);
          id = null;
        }
      }
      return id;
    } finally {
      this.persistLock.readLock().unlock();
    }
  }

  private void checkForNull(final ObjectMetadata id) {
    if (id == null) {
      throw new ObjectManagerException("No objects available.");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.cleversafe.simpleobject.performance.ObjectManager#getIdForRead()
   */
  @Override
  public ObjectMetadata acquireNameForRead() {
    if (this.testEnded) {
      throw new RuntimeException("Test already ended");
    }

    ObjectMetadata id;

    this.objectsLock.readLock().lock();
    id = this.objects.getRandom();
    try {
      checkForNull(id);
    } catch (final ObjectManagerException e) {
      this.objectsLock.readLock().unlock();
      throw e;
    }

    int count = 0;
    this.readingLock.writeLock().lock();
    if (this.currentlyReading.containsKey(id)) {
      // The only reason to have both locked simultaneously is to prevent an id from being
      // selected for deletion before it has been added to currentlyReading
      this.objectsLock.readLock().unlock();
      count = this.currentlyReading.get(id).intValue();
    }
    this.currentlyReading.put(id, Integer.valueOf(count + 1));
    if (count == 0) {
      this.objectsLock.readLock().unlock();
    }
    this.readingLock.writeLock().unlock();

    return id;
  }

  @Override
  public void releaseNameFromRead(final ObjectMetadata id) {
    this.readingLock.writeLock().lock();
    final int count = this.currentlyReading.get(id).intValue();
    if (count > 1) {
      this.currentlyReading.put(id, Integer.valueOf(count - 1));
    } else {
      this.currentlyReading.remove(id);
    }
    this.readingLock.writeLock().unlock();
    return;
  }

  @Override
  public void writeNameComplete(final ObjectMetadata id) {
    this.persistLock.readLock().lock();
    try {
      this.objects.put(id);
    } finally {
      this.persistLock.readLock().unlock();
    }
  }

  private void persistIds() throws IOException {
    this.persistLock.writeLock().lock();
    final int toSave = this.objects.size();
    final OutputStream out = new BufferedOutputStream(new FileOutputStream(this.saveFile));
    if (toSave > this.maxObjects) {
      for (int size = this.objects.size(); size > this.maxObjects; size = this.objects.size()) {
        final int numFiles = getIdFiles().length;
        File surplus = createFile(numFiles - 1);
        if (surplus.equals(this.saveFile) || (surplus.length() / OBJECT_SIZE) >= this.maxObjects) {
          // Create a new file
          surplus = createFile(numFiles);
        }
        final OutputStream dos = new BufferedOutputStream(new FileOutputStream(surplus, true));
        final int remaining = getRemaining(size, surplus);
        // While writing surplus, remove them from this.objects, to keep consistent with
        // this.savefile
        final Iterator<ObjectMetadata> iterator = this.objects.iterator();
        for (int i = 0; i < remaining; i++) {
          final ObjectMetadata sid = iterator.next();
          dos.write(sid.toBytes());
          iterator.remove();
        }
        dos.close();
      }
    } else if (toSave < this.maxObjects) {
      for (int size = this.objects.size(); size < this.maxObjects; size = this.objects.size()) {
        // Try to borrow from last id file
        // When borrowing, add to this.objects
        // Count the number of objects to borrow and truncate file by that amount
        final int numFiles = getIdFiles().length;
        final File surplus = createFile(numFiles - 1);
        // Need to ensure last file is not current file
        // If it is, don't borrow at all
        if (this.saveFile.equals(surplus)) {
          break;
        }
        final int toTransfer = getTransferrable(size, surplus);
        final DataInputStream in = new DataInputStream(new FileInputStream(surplus));
        final long skip = surplus.length() - (toTransfer * OBJECT_SIZE);
        in.skip(skip);
        final byte[] buf = new byte[OBJECT_SIZE];
        for (int i = 0; i < toTransfer; i++) {
          if (in.read(buf) == OBJECT_SIZE) {
            final ObjectMetadata sid = LegacyObjectMetadata.fromBytes(buf);
            this.objects.put(sid);
          }
        }
        in.close();

        // If surplus is out of objects, delete it
        if (skip == 0) {
          surplus.delete();
        } else {
          // We borrowed from the end of the file so nothing is lost from truncating
          RandomAccessFile truncater = null;
          try {
            truncater = new RandomAccessFile(surplus, "rwd");
            truncater.setLength(skip);
          } finally {
            if (truncater != null)
              truncater.close();
          }
        }
      }
    }

    // Finally we save a number less than or equal to the maximum number of objects to our
    // savefile
    _logger.debug(String.format("Writing state file: %d objects into ", this.objects.size())
        + this.saveFile);
    for (final Iterator<ObjectMetadata> iterator = this.objects.iterator(); iterator.hasNext();) {
      out.write(iterator.next().toBytes());
    }
    out.close();
    this.persistLock.writeLock().unlock();
  }

  private int getRemaining(final int size, final File surplus) {
    final int objectsAvailable = size - this.maxObjects;
    final int spaceAvailable = this.maxObjects - ((int) (surplus.length() / OBJECT_SIZE));
    return Math.min(objectsAvailable, spaceAvailable);
  }

  private int getTransferrable(final int size, final File surplus) {
    final int slotsAvailable = this.maxObjects - size;
    final int surplusAvailable = (int) (surplus.length() / OBJECT_SIZE);
    return Math.min(slotsAvailable, surplusAvailable);
  }

  private File createFile(final int idx) {
    return new File(this.directory + "/" + this.prefix + idx + SUFFIX);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.cleversafe.simpleobject.performance.ObjectManager#testComplete()
   */
  @Override
  public void testComplete() {
    this.testEnded = true;
    shutdownSaverThread();
    try {
      join();
    } catch (final InterruptedException e) {
      throw new RuntimeException("Failed to join");
    }

    try {
      persistIds();
    } catch (final Exception e) {
      throw new ObjectManagerException(e);
    }
  }

  private void shutdownSaverThread() {
    this.saver.shutdown();
    while (!this.saver.isTerminated()) {
      try {
        this.saver.awaitTermination(10, TimeUnit.SECONDS);

      }

      catch (final InterruptedException e)

      {
        _logger.error("", e);
      }
    }
  }

  @Override
  public String toString() {
    return String.format("RandomObjectPopulator [maxObjects=%s, directory=%s, prefix=%s]",
        this.maxObjects, this.directory, this.prefix);
  }
}
