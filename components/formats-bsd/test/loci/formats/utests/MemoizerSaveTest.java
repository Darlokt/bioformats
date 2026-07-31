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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import loci.formats.IFormatReader;
import loci.formats.Memoizer;
import loci.formats.in.FakeReader;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MemoizerSaveTest {

  private static final String TEST_FILE =
    "save&pixelType=int8&sizeX=20&sizeY=20&sizeC=1&sizeZ=1&sizeT=1.fake";

  private static class FailingInstallMemoizer extends Memoizer {

    FailingInstallMemoizer() {
      super(new FakeReader(), 0);
    }

    @Override
    public IFormatReader loadMemo() {
      return null;
    }

    @Override
    protected void installMemo(File source, File destination)
      throws IOException
    {
      throw new IOException("expected installation failure");
    }
  }

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

  private void assertNoTemporaryMemo() {
    File[] files = directory.toFile().listFiles();
    if (files == null) {
      throw new AssertionError("Could not list temporary test directory");
    }
    for (File file : files) {
      assertFalse(file.getName().contains(".bfmemo."));
    }
  }

  @Test
  public void testInstallFailureIsNotReportedAsSaved() throws Exception {
    try (Memoizer memoizer = new FailingInstallMemoizer()) {
      File memoFile = memoizer.getMemoFile(id);
      memoizer.setId(id);
      assertFalse(memoizer.isSavedToMemo());
      assertFalse(memoFile.exists());
    }
    assertNoTemporaryMemo();
  }

  @Test
  public void testFailedReplacementPreservesExistingMemo() throws Exception {
    File memoFile;
    try (Memoizer first = new Memoizer(new FakeReader(), 0)) {
      first.setId(id);
      memoFile = first.getMemoFile();
      assertTrue(first.isSavedToMemo());
    }
    assertTrue(memoFile.exists());

    try (Memoizer failed = new FailingInstallMemoizer()) {
      failed.setId(id);
      assertFalse(failed.isSavedToMemo());
      assertTrue(memoFile.exists());
    }
    assertNoTemporaryMemo();
  }
}
