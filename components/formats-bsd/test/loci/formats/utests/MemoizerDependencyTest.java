/*
 * #%L
 * BSD implementations of Bio-Formats readers and writers
 * %%
 * Copyright (C) 2005 - 2026 Open Microscopy Environment
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES ARE DISCLAIMED.
 * #L%
 */

package loci.formats.utests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import loci.formats.Memoizer;
import loci.formats.in.FakeReader;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MemoizerDependencyTest {

  private static final String TEST_FILE =
    "dependencies&pixelType=int8&sizeX=20&sizeY=20&sizeC=1&sizeZ=1&sizeT=1.fake";

  private Path directory;
  private String id;

  @BeforeMethod(alwaysRun = true)
  public void setUp() throws Exception {
    directory = Files.createTempDirectory(getClass().getName() + ".");
    Path file = directory.resolve(TEST_FILE);
    Files.createFile(file);
    id = file.toAbsolutePath().toString();
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() throws Exception {
    if (directory != null && Files.exists(directory)) {
      File[] files = directory.toFile().listFiles();
      if (files == null) {
        throw new AssertionError("Could not list temporary test directory");
      }
      for (File file : files) {
        Files.delete(file.toPath());
      }
      Files.delete(directory);
    }
  }

  private static byte[] utf8(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static void setDifferentModificationTime(Path file,
    long previousTime) throws Exception
  {
    Files.setLastModifiedTime(file, FileTime.fromMillis(previousTime + 2000));
    assertFalse(Files.getLastModifiedTime(file).toMillis() == previousTime);
  }

  @Test
  public void testChangedCompanionFileInvalidatesMemo() throws Exception {
    Path companion = new File(id + ".ini").toPath();
    Files.write(companion, utf8("sizeX=20\n"));

    try (Memoizer first = new Memoizer(new FakeReader(), 0)) {
      first.setId(id);
      assertTrue(first.isSavedToMemo());
    }
    try (Memoizer unchanged = new Memoizer(new FakeReader(), 0)) {
      unchanged.setId(id);
      assertTrue(unchanged.isLoadedFromMemo());
    }

    long previousTime = Files.getLastModifiedTime(companion).toMillis();
    Files.write(companion, utf8("sizeX=30\n"));
    setDifferentModificationTime(companion, previousTime);
    try (Memoizer changed = new Memoizer(new FakeReader(), 0)) {
      changed.setId(id);
      assertFalse(changed.isLoadedFromMemo());
      assertTrue(changed.isSavedToMemo());
      assertEquals(changed.getSizeX(), 30);
    }
  }

  @Test
  public void testAddedCompanionFileInvalidatesMemo() throws Exception {
    try (Memoizer first = new Memoizer(new FakeReader(), 0)) {
      first.setId(id);
      assertTrue(first.isSavedToMemo());
    }

    Path companion = new File(id + ".ini").toPath();
    Files.write(companion, utf8("sizeX=30\n"));
    try (Memoizer changed = new Memoizer(new FakeReader(), 0)) {
      changed.setId(id);
      assertFalse(changed.isLoadedFromMemo());
      assertEquals(changed.getSizeX(), 30);
    }
  }

  @Test
  public void testDeletedCompanionFileInvalidatesMemo() throws Exception {
    Path companion = new File(id + ".ini").toPath();
    Files.write(companion, utf8("sizeX=30\n"));
    try (Memoizer first = new Memoizer(new FakeReader(), 0)) {
      first.setId(id);
      assertTrue(first.isSavedToMemo());
    }

    Files.delete(companion);
    try (Memoizer changed = new Memoizer(new FakeReader(), 0)) {
      changed.setId(id);
      assertFalse(changed.isLoadedFromMemo());
      assertEquals(changed.getSizeX(), 20);
    }
  }
}
