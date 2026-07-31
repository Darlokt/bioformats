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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import loci.formats.Memoizer;
import loci.formats.in.DynamicMetadataOptions;
import loci.formats.in.FakeReader;
import loci.formats.in.MetadataLevel;
import loci.formats.in.MetadataOptions;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MemoizerOptionsTest {

  private static final String TEST_FILE =
    "options&pixelType=int8&sizeX=20&sizeY=20&sizeC=1&sizeZ=1&sizeT=1.fake";

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

  @Test
  public void testChangedDynamicOptionsInvalidateMemo() throws Exception {
    FakeReader firstReader = new FakeReader();
    DynamicMetadataOptions firstOptions = new DynamicMetadataOptions();
    firstOptions.set("reader.option", "first");
    firstReader.setMetadataOptions(firstOptions);
    try (Memoizer first = new Memoizer(firstReader, 0)) {
      first.setId(id);
      assertTrue(first.isSavedToMemo());
    }

    FakeReader secondReader = new FakeReader();
    DynamicMetadataOptions secondOptions = new DynamicMetadataOptions();
    secondOptions.set("reader.option", "second");
    secondReader.setMetadataOptions(secondOptions);
    try (Memoizer second = new Memoizer(secondReader, 0)) {
      second.setId(id);
      assertFalse(second.isLoadedFromMemo());
    }
  }

  @Test
  public void testOptionsFileParticipatesInMemoCompatibility()
    throws Exception
  {
    Path optionsFile = new File(id + ".bfoptions").toPath();
    Files.write(optionsFile,
      "[options]\nmetadata.level=MINIMUM\n".getBytes(StandardCharsets.UTF_8));

    try (Memoizer first = new Memoizer(new FakeReader(), 0)) {
      first.setId(id);
      assertTrue(first.isSavedToMemo());
    }
    try (Memoizer unchanged = new Memoizer(new FakeReader(), 0)) {
      unchanged.setId(id);
      assertTrue(unchanged.isLoadedFromMemo());
    }

    Files.write(optionsFile,
      "[options]\nmetadata.level=ALL\n".getBytes(StandardCharsets.UTF_8));
    try (Memoizer changed = new Memoizer(new FakeReader(), 0)) {
      changed.setId(id);
      assertFalse(changed.isLoadedFromMemo());
    }
  }

  @Test
  public void testIdentityMetadataOptionsSafelyMissMemo() throws Exception {
    class IdentityMetadataOptions implements MetadataOptions {

      private MetadataLevel level = MetadataLevel.ALL;
      private boolean validate;

      @Override
      public void setMetadataLevel(MetadataLevel metadataLevel) {
        level = metadataLevel;
      }

      @Override
      public MetadataLevel getMetadataLevel() {
        return level;
      }

      @Override
      public void setValidate(boolean validateMetadata) {
        validate = validateMetadata;
      }

      @Override
      public boolean isValidate() {
        return validate;
      }
    }

    FakeReader firstReader = new FakeReader();
    firstReader.setMetadataOptions(new IdentityMetadataOptions());
    try (Memoizer first = new Memoizer(firstReader, 0)) {
      first.setId(id);
      assertTrue(first.isSavedToMemo());
    }

    FakeReader secondReader = new FakeReader();
    secondReader.setMetadataOptions(new IdentityMetadataOptions());
    try (Memoizer second = new Memoizer(secondReader, 0)) {
      second.setId(id);
      assertFalse(second.isLoadedFromMemo());
      assertTrue(second.isSavedToMemo());
    }
  }
}
